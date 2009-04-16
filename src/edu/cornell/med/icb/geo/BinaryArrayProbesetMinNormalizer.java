/*
 * Copyright (C) 2007-2009 Institute for Computational Biomedicine,
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
import edu.cornell.med.icb.geo.binaryarray.ArrayWriter;
import edu.cornell.med.icb.tissueinfo.annotations.AnnotationParser;
import edu.cornell.med.icb.tissueinfo.annotations.AnnotationSet;
import edu.cornell.med.icb.tissueinfo.similarity.ScoredTranscriptBoundedSizeQueue;
import edu.cornell.med.icb.tissueinfo.similarity.TranscriptScore;
import edu.cornell.med.icb.identifier.IndexedIdentifier;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatisticsImpl;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * An adapter that normalizes probeset signal data for each probeset. For each probeset on an array, the signal values
 * for present calls are ordered by increasing signal values. The smallest k present signal values are used to estimate
 * the signal value for the lower limit of signal detection for the probeset.  This value is used to divide each signal
 * value for the probeset, such that signal is brought back into a range where 1 indicates minimum detection ability
 * of the platform. A value of zero indicates that the probeset could not be observed in the sample. Signal values
 * larger than 1 indicate that more expression was detected. This normalization should eliminate probeset hybridization
 * differences.
 *
 * @author Fabien Campagne
 * Date: Dec 7, 2007
 * Time: 12:21:45 PM
 *
 */
public class BinaryArrayProbesetMinNormalizer implements FormatAdapter {

    /**
     * Used to log debug and informational messages.
     */
    private static final Logger LOGGER =
            Logger.getLogger(BinaryArrayProbesetMinNormalizer.class);

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

    public void analyzeSampleData(final GEOPlatformIndexed platform, final SampleDataCallback callback,
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
        final double[] sumSignalBySample = sumSignal(platform);

        final SummaryStatistics statHelper = new SummaryStatisticsImpl();
        for (final double sumForOneSample : sumSignalBySample) {
            statHelper.addValue(sumForOneSample);
        }
        final double averageSumForSamples = statHelper.getMean();
        final float[] minValuesPerProbesets =
                estimateMinValueForProbesets(sumSignalBySample, platform, 10, averageSumForSamples);

        writeNormalize(averageSumForSamples, sumSignalBySample, minValuesPerProbesets, platform);
        System.exit(0);
    }

    private ArrayReader getReader(final GEOPlatformIndexed platform) throws IOException, ClassNotFoundException {
        if (inputBasename == null) {
            inputBasename = platform.getName().toString();
        }
        return new ArrayReader(new MutableString(inputBasename));
    }

    private ArrayWriter getWriter(final GEOPlatformIndexed platform) throws IOException {
        if (outputBasename == null) {
            outputBasename = platform.getName().toString() + "-minnormalized";
        }
        return new ArrayWriter(outputBasename, platform);
    }


    private double[] sumSignal(final GEOPlatformIndexed platform) {
        final float[] signal;
        final int minProbeIndex = 0;

        final int maxProbeIndex = platform.getNumProbeIds();
        final int k = maxProbeIndex - minProbeIndex;
        try {


            final ArrayReader arrayReader = getReader(platform);
            signal = arrayReader.allocateSignalArray();
            final ObjectList<MutableString> sampleIds;
            sampleIds = arrayReader.getSampleIdList();

            final double[] sumSignalBySample = new double[sampleIds.size()];

            progressLogger.expectedUpdates = sampleIds.size();
            progressLogger.start("Starting to sum signal for each sample");

            for (int sampleIndex = 0; sampleIndex < sampleIds.size(); ++sampleIndex) {
                progressLogger.update();
                arrayReader.readNextSample(signal);
                for (final float signalValue : signal) {
                    sumSignalBySample[sampleIndex] += signalValue;
                }
            }
            arrayReader.close();
            progressLogger.stop("Finished summing signal for each sample");
            return sumSignalBySample;
        } catch (IOException
                e) {
            System.err.println("Error reading binary information from file");
            e.printStackTrace();
            System.exit(10);
        } catch (ClassNotFoundException
                e) {
            System.err.println("Could not initialize an array reader.");
            e.printStackTrace();
            System.exit(10);
        }
        return null;
    }

    private float[] estimateMinValueForProbesets(final double[] sumSignalBySample, final GEOPlatformIndexed platform,
                                                 final int k, final double averageSumForSamples) {

        final float[] signal;
        final int minProbeIndex = 0;
        final int maxProbeIndex = platform.getNumProbeIds();

        try {


            final ArrayReader arrayReader = getReader(platform);
            signal = arrayReader.allocateSignalArray();
            final ObjectList<MutableString> sampleIds;
            sampleIds = arrayReader.getSampleIdList();


            final AnnotationSet annotationSet = readAnnotationSet(platform, sampleIds);

            progressLogger.expectedUpdates = sampleIds.size();
            progressLogger.start("Starting estimateMinValueForProbesets");
            final float[] minSignalAveragePerProbeset = new float[maxProbeIndex];
            // Create one queue per probeset where the signal values will be enqueued. Capacity is set to k, and largest
            // values are kept.
            final ScoredTranscriptBoundedSizeQueue[] smallestSignalValuesForProbesets =
                    new ScoredTranscriptBoundedSizeQueue[maxProbeIndex];
            for (int i = 0; i < smallestSignalValuesForProbesets.length; i++) {
                smallestSignalValuesForProbesets[i] = new ScoredTranscriptBoundedSizeQueue(k);
            }

            for (int sampleIndex = 0; sampleIndex < sampleIds.size(); ++sampleIndex) {

                // load signal for one sample (all probesets)
                arrayReader.readNextSample(signal);

                for (int probesetIndex = minProbeIndex; probesetIndex < maxProbeIndex; ++probesetIndex) {


                    final int transcriptIndex = getTranscriptIndex(probesetIndex);
                    if (transcriptIndex != -1 && signal[probesetIndex] != 0) {

                        // probeset maps to a transcript in a non-ambiguous way, and was called 'present' in the sample
                        final double sampleNormalizedSignal =
                                averageSumForSamples * (signal[probesetIndex]) / sumSignalBySample[sampleIndex];
                        smallestSignalValuesForProbesets[probesetIndex].enqueue(sampleIndex, -sampleNormalizedSignal);
                    }
                }

                progressLogger.update();
            }
            arrayReader.close();
            for (int probesetIndex = 0; probesetIndex < smallestSignalValuesForProbesets.length; probesetIndex++) {
                final SummaryStatistics helper = new SummaryStatisticsImpl();
                while (!smallestSignalValuesForProbesets[probesetIndex].isEmpty()) {
                    final TranscriptScore score = smallestSignalValuesForProbesets[probesetIndex].dequeue();
                    final double smallestSignalValue = -score.score;

                    helper.addValue(smallestSignalValue);
                }

                minSignalAveragePerProbeset[probesetIndex] = (float) helper.getMean();
                // System.out.println("estimated "+minSignalAveragePerProbeset[probesetIndex]+" for probeset "+probesetIndex);
            }
            progressLogger.stop("Finished estimateMinValueForProbesets.");
            return minSignalAveragePerProbeset;
        } catch (IOException e) {
            System.err.println("Error reading binary information from file");
            e.printStackTrace();
            System.exit(10);
        } catch (ClassNotFoundException e) {
            System.err.println("Could not initialize an array reader.");
            e.printStackTrace();
            System.exit(10);
        }
        return null;
    }

    private void writeNormalize(final double averageSumForSamples, final double[] sumSignalBySample,
                                final float[] minValuesPerProbesets,
                                final GEOPlatformIndexed platform) {

        try {

            final ArrayReader arrayReader = getReader(platform);
            final ArrayWriter arrayWriter = getWriter(platform);
            final ObjectList<MutableString> sampleIds;
            sampleIds = arrayReader.getSampleIdList();
            progressLogger.expectedUpdates = sampleIds.size();
            progressLogger.start("Starting to write normalized signal for samples");

            final float[] signal = arrayReader.allocateSignalArray();
            final int minProbeIndex = 0;
            final int transcriptNumber = options.tpr.getTranscriptNumber();
            final int maxProbeIndex = platform.getNumProbeIds();
            final float[] signalOutput = new float[signal.length];
            for (int sampleIndex = 0; sampleIndex < sampleIds.size(); ++sampleIndex) {
                progressLogger.update();
                arrayReader.readNextSample(signal);
                for (int probesetIndex = minProbeIndex; probesetIndex < maxProbeIndex; ++probesetIndex) {

                    final int transcriptIndex = getTranscriptIndex(probesetIndex);
                    if (transcriptIndex != -1) {
                        final double sampleNormalizedSignal =
                                averageSumForSamples * (signal[probesetIndex]) / sumSignalBySample[sampleIndex];
                        double probesetNormalizedSignal =
                                sampleNormalizedSignal / minValuesPerProbesets[probesetIndex];
                        if (probesetNormalizedSignal != probesetNormalizedSignal) {  // if NaN set to zero.
                            probesetNormalizedSignal = 0;
                        }
                        signalOutput[probesetIndex] = (float) probesetNormalizedSignal;
                        // System.out.println("normalized signal: " + probesetNormalizedSignal);
                    }

                }
                arrayWriter.appendSample(signalOutput, sampleIds.get(sampleIndex));
            }
            arrayReader.close();
            arrayWriter.close();
            progressLogger.stop("Finished to write normalized signal");
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


    private AnnotationSet readAnnotationSet(final GEOPlatformIndexed platform,
                                            final ObjectList<MutableString> sampleIds)
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
     * Convert a probeset index to a transcript index. Several probeset indices may yield the same transcript index if the
     * probesets measure the same transcripts. This property makes it possible to accumulate counts over probesets.
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
            this.inputBasename = CLI.getOption(opts, "--input", null);
            System.out.println("Will read input: " + this.inputBasename);
            this.outputBasename = CLI.getOption(opts, "--output", null);
            System.out.println("Will write output: " + this.outputBasename);
        } else {
            System.out.println("adapter options were null.");
        }
    }
}
