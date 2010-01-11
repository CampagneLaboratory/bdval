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

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import edu.cornell.med.icb.geo.binaryarray.ArrayReader;
import edu.cornell.med.icb.identifier.IndexedIdentifier;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;

/*
 * An adapter that reads the signal data in binary format and generates
 * a tab delimited file. Probeset ids, transcript ids and signal values are
 * written to the tab delimited file for each sample, for a random selection
 * of probeset on the platform.
 *
 * @author Fabien Campagne
 * Date: Dec 7, 2007
 * Time: 11:55 PM
 *
 */
public class BinaryArray2TabDelimitedAdapter implements FormatAdapter {
    /**
     * Used to log debug and informational messages.
     */
    private static final Logger LOGGER =
            Logger.getLogger(BinaryArray2TabDelimitedAdapter.class);

    /**
     * The time interval for a new log in milliseconds.
     *
     * @see it.unimi.dsi.logging.ProgressLogger#DEFAULT_LOG_INTERVAL
     */
    protected final long logInterval = ProgressLogger.DEFAULT_LOG_INTERVAL;

    private final ProgressLogger progressLogger =
            new ProgressLogger(LOGGER, logInterval, "samples processed");
    private String projectTo;
    private String inputBasename;

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
        invert(platform);

    }

    ArrayReader arrayReader;
    private boolean normalizePerProbeset;

    private void invert(final GEOPlatformIndexed platform) {
        final float[] signal;
        final int maxProbeIndex = platform.getNumProbeIds();
        final IntSet probesetSelection = getRandomProbesetSelection(maxProbeIndex, 100);

        try {


            if (inputBasename == null) {
                inputBasename = platform.getName().toString();
            }
            arrayReader = new ArrayReader(new MutableString(inputBasename));

            signal = arrayReader.allocateSignalArray();
            final ObjectList<MutableString> sampleIds;
            sampleIds = arrayReader.getSampleIdList();


            progressLogger.expectedUpdates = sampleIds.size();
            progressLogger.start("Starting writing tab delimited sample info");

            final PrintWriter output = new PrintWriter("out.tsv");

            output.print("SampleId");
            output.print('\t');
            output.print("TranscriptId");
            output.print('\t');
            output.print("ProbesetId");
            output.print('\t');
            output.print("Signal");
            output.print("\n");

            for (int sampleIndex = 0; sampleIndex < sampleIds.size(); ++sampleIndex) {
                System.out.println(sampleIds.get(sampleIndex));
                // load signal for one sample (all probesets)
                arrayReader.readNextSample(signal);
                double min = Double.MAX_VALUE;
                double max = Double.NEGATIVE_INFINITY;
                for (final double value : signal) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);

                }
                System.out.println("sample " + sampleIds.get(sampleIndex) + " min=" + min + " max=" + max);
                for (final int probesetIndex : probesetSelection) {
                    final int transcriptIndex = getTranscriptIndex(probesetIndex);
                    if (transcriptIndex != -1) {
                        output.print(sampleIds.get(sampleIndex));
                        output.print('\t');
                        output.print(getTranscriptId(probesetIndex));
                        output.print('\t');
                        output.print(platform.getProbesetIdentifier(probesetIndex));
                        output.print('\t');
                        output.print(signal[probesetIndex]);
                        output.print('\n');
                        output.flush();
                    }
                }

                progressLogger.update();
            }

            progressLogger.stop("Finished writing tab delimited sample info.");

            output.flush();
            output.close();


            System.exit(0);
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
    }

    private IntArraySet getRandomProbesetSelection(final int maxProbesetIndex, final int numRandom) {
        final int maxTranscriptIndex = options.tpr.getTranscripts().size();

        final IntArraySet result = new IntArraySet();
        final RandomEngine engine = new MersenneTwister();
        System.out.println("Selecting random set of probesets.");
        for (int numChosen = 0; numChosen < numRandom; numChosen++) {
            final int oldSize = result.size();
            do {
                final int randomTranscriptIndex = randomChoose(engine, 0, maxTranscriptIndex);
                // add all probesets for the corresponding transcript:
                final IntSet probesets = options.tpr.getTranscriptSet(randomTranscriptIndex);
                if (probesets != null) {
                    result.addAll(probesets);
                }
            } while (result.size() == oldSize);
        }
        System.out.println("Selected " + result.size() + " probesets.");
        return result;

    }

    private int randomChoose(final RandomEngine engine, final int minIndexInclusive, final int maxIndeInclusive) {
        final double mii = minIndexInclusive;
        final double maii = maxIndeInclusive;
        return (int) Math.round((engine.nextDouble()) * (maii - mii) + mii);
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

    /**
     * Convert a probeset index to a transcript index. Several probeset indices may yield the same transcript index if the
     * probesets measure the same transcripts. This property makes it possible to accumulate counts over probesets.
     *
     * @param probesetIndex
     * @return Identifier of the transcript that this probeset measures.
     */
    private MutableString getTranscriptId(final int probesetIndex) {
        return options.tpr.getGeneId(options.tpr.probeset2TranscriptIndex(probesetIndex));
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
        } else {
            System.out.println("adapter options were null.");
        }
    }
}
