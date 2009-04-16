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
import edu.cornell.med.icb.identifier.IndexedIdentifier;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.log4j.Logger;

import java.io.IOException;


/**
 * An adapter that makes sure that each signal value is positive. This is done by coding samples that can have negative
 * values as two samples: one with positive counts, one with negative counts. An alternative would be to add a delta
 * signal to each value such that all signal values are positive, but this changes values a lot since the minimum
 * negative value can be fairly large in absolute value.
 *
 * @author Fabien Campagne Date: Dec 10, 2007 Time: 2:00:50 PM
 */
public class BinaryArray2PositiveAdapter implements FormatAdapter {

    /**
     * Used to log debug and informational messages.
     */
    private static final Logger LOGGER =
            Logger.getLogger(BinaryArray2PositiveAdapter.class);

    /**
     * The time interval for a new log in milliseconds.
     *
     * @see it.unimi.dsi.logging.ProgressLogger#DEFAULT_LOG_INTERVAL
     */
    protected final long logInterval = ProgressLogger.DEFAULT_LOG_INTERVAL;

    private final ProgressLogger progressLogger =
            new ProgressLogger(LOGGER, logInterval, "samples processed");

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

        transform(platform);
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


    private void transform(final GEOPlatformIndexed platform) {

        try {

            final ArrayReader arrayReader = getReader(platform);
            final ObjectList<MutableString> sampleIds;
            sampleIds = arrayReader.getSampleIdList();

            final float[] signal = arrayReader.allocateSignalArray();
            final int minProbeIndex = 0;

            final int maxProbeIndex = platform.getNumProbeIds();
            final int numSamples = sampleIds.size();
            final float[][] matrix = new float[numSamples][maxProbeIndex];  // column index, then row index

            progressLogger.expectedUpdates = numSamples;
            progressLogger.start("Starting to read signal for samples");
            final double[] minValues = new double[numSamples];
            for (int sampleIndex = 0; sampleIndex < numSamples; ++sampleIndex) {

                arrayReader.readNextSample(signal);
                //FloatList signalList = new FloatArrayList();
                //signalList.addElements(0, signal);
                //Collections.sort(signalList);
                double min = Double.MAX_VALUE;
                double max = Double.NEGATIVE_INFINITY;
                // min = signalList.get((int) (maxProbeIndex * 0.05));  // ignore 5% of values smaller
                // max = signalList.get((int) (maxProbeIndex * 0.95));
                for (final double value : signal) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
                //    System.out.println("sample " + sampleIds.get(sampleIndex) + " min=" + min + " max=" + max);

                matrix[sampleIndex] = new float[signal.length];
                final float[] dest = matrix[sampleIndex];

                System.arraycopy(signal, 0, dest, 0, signal.length);
                minValues[sampleIndex] = min;

                progressLogger.lightUpdate();
            }
            arrayReader.close();

            progressLogger.stop("Finished to read input signal");
            progressLogger.expectedUpdates = 2;

            /*
            progressLogger.start("Shifting signal values..");

            progressLogger.update();


            for (int sampleIndex = 0; sampleIndex < numSamples; ++sampleIndex) {
                for (int probesetIndex = minProbeIndex; probesetIndex < maxProbeIndex; ++probesetIndex) {

                    // shift all signal values to make them >=0
                    //       matrix[sampleIndex][probesetIndex] += -minValues[sampleIndex];

                }
            }
            progressLogger.update();
            progressLogger.stop("Finished transforming signal.");
            */
            System.out.flush();
            System.out.println("Initial matrix dimensions " + maxProbeIndex + " x " + numSamples);
            final ArrayWriter arrayWriter = getWriter(platform);

            progressLogger.start("Starting to write transformed signal");

            for (int sampleIndex = 0; sampleIndex < sampleIds.size(); sampleIndex++) {
                if (minValues[sampleIndex] < 0) {
                    final float[] negatives = new float[maxProbeIndex];

                    for (int i = 0; i < negatives.length; i++) {
                        negatives[i] = matrix[sampleIndex][i] < 0 ? -matrix[sampleIndex][i] : 0;
                        assert negatives[i] >= 0 : "all elements must be positive, element value was not :"+negatives[i] ;
                        matrix[sampleIndex][i] = matrix[sampleIndex][i] >= 0 ? matrix[sampleIndex][i] : 0;
                        assert matrix[sampleIndex][i] >= 0 : "all elements must be positive, element value was not :"+matrix[sampleIndex][i] ;

                    }

                    arrayWriter.appendSample(negatives, new MutableString(sampleIds.get(sampleIndex) + "_negative"));

                }
                arrayWriter.appendSample(matrix[sampleIndex], sampleIds.get(sampleIndex));
                progressLogger.lightUpdate();

            }
            arrayWriter.close();
            progressLogger.stop("Finished writing transformed signal");
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


    public void postSeries(
            final GEOPlatformIndexed platform,
            final ObjectList<MutableString> sampleIdSelection) {

    }

    GeoScanOptions options;

    public void setOptions
            (final GeoScanOptions options) {
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
