/*
 * Copyright (C) 2007-2010 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.cornell.med.icb.geo;

import edu.cornell.med.icb.geo.binaryarray.ArrayReader;
import edu.cornell.med.icb.identifier.IndexedIdentifier;
import edu.cornell.med.icb.tissueinfo.annotations.AnnotationParser;
import edu.cornell.med.icb.tissueinfo.annotations.AnnotationSet;
import edu.cornell.med.icb.tissueinfo.annotations.Projection;
import edu.cornell.med.icb.tissueinfo.similarity.TissueESTCountsWriter;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;


/*
 * An adapter that transposes the signal data in binary format and generates
 * TEPSS counts. This adapter reads the platform information, but no sample data.
 * Normalized sample data is read from the binary files produced by
 * edu.cornell.icb.geo.QuantileNormalizerPass2Adapter
 *
 * @author Fabien Campagne
 * Date: Aug 20, 2007
 * Time: 4:45:17 PM
 *
 */
public class BinaryArray2CountsAdapter implements FormatAdapter {

    /**
     * Used to log debug and informational messages.
     */
    private static final Logger LOGGER =
            Logger.getLogger(BinaryArray2CountsAdapter.class);

    /**
     * The time interval for a new log in milliseconds.
     *
     * @see ProgressLogger#DEFAULT_LOG_INTERVAL
     */
    protected final long logInterval = ProgressLogger.DEFAULT_LOG_INTERVAL;

    private final ProgressLogger progressLogger =
            new ProgressLogger(LOGGER, logInterval, "samples processed");
    private String projectTo;
    private String inputBasename;
    private String outputBasename;

    public SampleDataCallback getCallback(final GEOPlatformIndexed platform) {

        return null;
    }

    public void analyzeSampleData(final GEOPlatformIndexed platform,
                                  final SampleDataCallback callback,
                                  final MutableString sampleIdentifier) {
    }

    public void preSeries(final GEOPlatformIndexed platform) {
        if (options.tpr != null) {
            LOGGER.info(String.format("Platform maps to %d transcripts.", options.tpr.getTranscripts().size()));
        } else {
            // install default mapping from probsetId -> probesetId/asTranscriptId

            final IndexedIdentifier transcriptIndices = new IndexedIdentifier();
            options.tpr = new TranscriptProbesetRelationship(transcriptIndices);
            final IndexedIdentifier indexOfProbesets = platform.getProbeIds();
            for (final MutableString probesetId : indexOfProbesets.keySet()) {
                options.tpr.addRelationship(probesetId, indexOfProbesets.get(probesetId));
            }
        }
        projectTo = "tissue";
        invert(platform);

    }

    ArrayReader arrayReader;
    private boolean normalizePerProbeset;

    private ArrayReader getReader(final GEOPlatformIndexed platform) throws IOException, ClassNotFoundException {
        if (inputBasename == null) {
            inputBasename = platform.getName().toString();
        }
        return new ArrayReader(new MutableString(inputBasename));
    }

    private void invert(final GEOPlatformIndexed platform) {
        final int numProbes = platform.getNumProbeIds();
        final float[] signal;
        final int minProbeIndex = 0;
        final int transcriptNumber = options.tpr.getTranscriptNumber();
        final int maxProbeIndex = platform.getNumProbeIds();
        final int k = maxProbeIndex - minProbeIndex;
        try {
            arrayReader = getReader(platform);

            signal = arrayReader.allocateSignalArray();
            final ObjectList<MutableString> sampleIds;
            sampleIds = arrayReader.getSampleIdList();


            final AnnotationSet annotationSet = readAnnotationSet(platform, sampleIds);

            progressLogger.expectedUpdates = sampleIds.size();
            final int[][] counts = new int[transcriptNumber][sampleIds.size()];
            progressLogger.start("Starting to read samples");
            final int[] numProbesetsPerTranscript = new int[transcriptNumber];

            for (int sampleIndex = 0; sampleIndex < sampleIds.size(); ++sampleIndex) {

                // load signal for one sample (all probesets)
                arrayReader.readNextSample(signal);
                double min = Double.MAX_VALUE;
                double max = Double.NEGATIVE_INFINITY;
                for (final double value : signal) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);

                }
                //     System.out.println("sampleId: "+sampleIds.get(sampleIndex )+"min: "+min+" max: "+max);
                for (int probesetIndex = minProbeIndex; probesetIndex < maxProbeIndex; ++probesetIndex) {
                    //     System.out.println("count["+getTranscriptIndex(probesetIndex)+"] ["+sampleIndex+"]");
                    //    System.out.flush();
                    final int transcriptIndex = getTranscriptIndex(probesetIndex);
                    if (transcriptIndex != -1) {

                        // probeset maps to a transcript in a non-ambiguous way.
                        counts[transcriptIndex][sampleIndex] += Math.round(signal[probesetIndex]);

                        ++numProbesetsPerTranscript[transcriptIndex];
                    }
                }
                progressLogger.update();
            }
            progressLogger.stop("Finished reading samples.");

            progressLogger.expectedUpdates = sampleIds.size();
            progressLogger.start("Staring to adjust counts.");
            // make sure each count is positive (>=0):
            for (int sampleIndex = 0; sampleIndex < sampleIds.size(); sampleIndex++) {

                for (int transcriptIndex = 0; transcriptIndex < counts.length; ++transcriptIndex) {
                    if (counts[transcriptIndex][sampleIndex] < 0) {
                        counts[transcriptIndex][sampleIndex] = 0;
                    } else {
                        if (normalizePerProbeset) {
                            // normalize by number of probesets that contributed to the transcript count:
                            if (numProbesetsPerTranscript[transcriptIndex] != 0) {
                                counts[transcriptIndex][sampleIndex] /= numProbesetsPerTranscript[transcriptIndex];
                            }
                        }
                    }
                }
                progressLogger.lightUpdate();
            }
            progressLogger.stop("Finished adjusting counts");
            progressLogger.expectedUpdates = counts.length;
            progressLogger.start("Starting to write counts");

            // write counts for transcript ids:
            final TissueESTCountsWriter writer = new TissueESTCountsWriter(getOutputBasename(platform));

            final Projection projection = new Projection(annotationSet);

            projection.addProjectionRule(new MutableString(projectTo));
            //scan the counts to determine valid projection indices and know the dimension of the counts array:
            final IntSet destinationSpace = new IntArraySet();
            final IntSet allCombinations = new IntArraySet();
            for (final MutableString sampleId : sampleIds) {
                projection.project(sampleId, destinationSpace);
                allCombinations.addAll(destinationSpace);
                for (final int projectedIndex : destinationSpace) {
                    final String tissue = projection.getIndexDescription(projectedIndex).toString();
                    //                                           System.out.println("1. sampleId: "+sampleId+" projectedIndex: " + projectedIndex +  " "+tissue);

                    writer.registerTissueType(tissue);
                }
            }
            final int[] projectedSpace = new int[allCombinations.size()];

            assert writer.getTissueCount() == projectedSpace
                    .length : "Number of registered tissues must match size of counts array.";
            final IntSet destinationSpaceIndices = new IntArraySet();
            for (int transcriptIndex = 0; transcriptIndex < counts.length; ++transcriptIndex) {
                // project samples onto output space:
                Arrays.fill(projectedSpace, 0);
                for (int sampleIndex = 0; sampleIndex < sampleIds.size(); sampleIndex++) {
                    //   MutableString sampleId = sampleIds.get(sampleIndex);

                    projection.project(sampleIds.get(sampleIndex), destinationSpaceIndices);
                    for (final int projectedIndex : destinationSpaceIndices) {
                        final String tissue = projection.getIndexDescription(projectedIndex).toString();
                        //     System.out.println("2. sampleId: "+ sampleId +" projectedIndex: " + projectedIndex +  " "+tissue);
                        projectedSpace[projectedIndex] += counts[transcriptIndex][sampleIndex];
                    }
                }
                final boolean notZero = hasAtLeastOneNonZeroCount(projectedSpace);
                if (notZero) { // write only those transcripts for which there is at least one non-zero count.

                    // now write the counts:
                    final MutableString transcriptId = options.tpr.getGeneId(transcriptIndex);
                    if (transcriptId != null) {
                        writer.writeTissueCounts(transcriptId.toString(),
                                projectedSpace);
                    }
                }
                progressLogger.lightUpdate();
            }
            writer.close();
            System.err.println("Finished writing counts.");
            progressLogger.stop();

            System.exit(0);
        } catch (IOException e) {
            System.err.println("Error reading binary information from file");
            e.printStackTrace();
            System.exit(10);
        } catch (ClassNotFoundException e) {
            System.err.println("Could not initialize an array reader.");
            e.printStackTrace();
            System.exit(10);
        }
    }

    private boolean hasAtLeastOneNonZeroCount(final int[] projectedSpace) {
        boolean notZero = false;
        for (int i = 0; i < projectedSpace.length; i++) {
            final int count = projectedSpace[i];

            if (count != 0) {
                notZero = true;
            }
        }
        return notZero;
    }

    private String getOutputBasename(final GEOPlatformIndexed platform) {
        if (outputBasename == null) {
            return platform.getName().toString();
        } else {
            return outputBasename;
        }
    }

    private AnnotationSet readAnnotationSet(final GEOPlatformIndexed platform, final ObjectList<MutableString> sampleIds)
            throws FileNotFoundException {
        final AnnotationParser parser = new AnnotationParser();
        // find annotations for this platform.
        final String filename = "data/curation/" + platform.getName() + ".txt";
        LOGGER.info("Reading sample annotations from " + filename);
        final File annotationFile = new File(filename);
        final AnnotationSet annotationSet;
        if (annotationFile.exists()) {
            annotationSet = parser.parse(new FileReader(filename));
        } else {
            // create dummy annotations where each sampleId is annotated with a value that corresponds to the
            // sample id:
            final String projectToSampleId = "sampleId";
            final MutableString annotationType = new MutableString(projectToSampleId);
            projectTo = projectToSampleId;
            annotationSet = new AnnotationSet();
            for (final MutableString sampleId : sampleIds) {

                annotationSet.associate(sampleId, annotationType, sampleId);
            }
        }
        return annotationSet;
    }


    /**
     * Convert a probeset index to a transcript index. Several probeset indices may yield the same transcript index if
     * the probesets measure the same transcripts. This property makes it possible to accumulate counts over probesets.
     *
     * @param probesetIndex
     * @return index of the transcript that this probeset measures.
     */
    private int getTranscriptIndex(final int probesetIndex) {
        return options.tpr.probeset2TranscriptIndex(probesetIndex);
    }

    private ObjectArrayList<MutableString> readSampleList(final String sampleListFilename) {
        return GeoScanSelectedSamplesMode.readSampleIds(sampleListFilename);
    }

    public void postSeries(final GEOPlatformIndexed platform,
                    final ObjectList<MutableString> sampleIdSelection) {

    }

    GeoScanOptions options;

    public void setOptions(final GeoScanOptions options) {
        assert options.tpr != null : "This implementation requires transcript to probeset relationships.";
        this.options = options;
        if (options.adapterOptions != null) {
            final String[] opts = options.adapterOptions.split("[\\s]+");
            this.normalizePerProbeset = CLI.isKeywordGiven(opts, "--normalize");
            System.out.println("normalizing: " + this.normalizePerProbeset);
            this.inputBasename = CLI.getOption(opts, "--input", null);
            System.out.println("Will read input: " + this.inputBasename);
            this.outputBasename = CLI.getOption(opts, "--output", null);
            System.out.println("Will write output: " + this.outputBasename);
        } else {
            System.out.println("adapter options were null.");
        }
    }
}
