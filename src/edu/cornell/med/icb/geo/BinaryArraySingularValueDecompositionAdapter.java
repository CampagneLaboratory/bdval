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
import edu.cornell.med.icb.svd.SVDFactory;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.io.TextIO;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.log4j.Logger;

import java.io.IOException;


/**
 * An adapter that performs a singular value decomposition (SVD) on the content of the binary array. SVD is a clustering
 * and data reduction technique (see <a href="http://en.wikipedia.org/wiki/Singular_value_decomposition">
 * wikipedia</a>).
 *
 * @author Fabien Campagne Date: Dec 10, 2007 Time: 2:00:50 PM
 */
public class BinaryArraySingularValueDecompositionAdapter implements FormatAdapter {

    /**
     * Used to log debug and informational messages.
     */
    private static final Logger LOGGER =
            Logger.getLogger(BinaryArraySingularValueDecompositionAdapter.class);

    /**
     * The time interval for a new log in milliseconds.
     *
     * @see it.unimi.dsi.logging.ProgressLogger#DEFAULT_LOG_INTERVAL
     */
    protected final long logInterval = ProgressLogger.DEFAULT_LOG_INTERVAL;

    final ProgressLogger progressLogger =
            new ProgressLogger(LOGGER, logInterval, "samples processed");
    private String projectTo;
    private String inputBasename;
    private String outputBasename;
    private boolean absoluteSignal;
    private String SVDImplementationName;
    private int maxSvdComponentNumber = -1;
    private boolean testSVD;
    private int maxSampleNumber;
    private boolean scaleWithFirstSingularValue;

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


        svd(platform);
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


    private void svd(final GEOPlatformIndexed platform) {

        try {

            final ArrayReader arrayReader = getReader(platform);
            final ObjectList<MutableString> sampleIds;
            sampleIds = arrayReader.getSampleIdList();

            final float[] signal = arrayReader.allocateSignalArray();
            final int minProbeIndex = 0;

            final int maxProbeIndex = platform.getNumProbeIds();
            final int numSamples =
                    Math.min(sampleIds.size(), maxSampleNumber != -1 ? maxSampleNumber : sampleIds.size());
            final double[][] matrix = new double[numSamples][maxProbeIndex];  // column index, then row index
            final edu.cornell.med.icb.svd.SingularValueDecomposition svd =
                    SVDFactory.getImplementation(SVDImplementationName);

            if (testSVD) { // test SVD connection before any time is spent loading the samples:
                System.out.println("Testing SVD implementation..");
                // double[][] testMatrix = new double[numSamples][maxProbeIndex];
                svd.svd(matrix, 0, 0);
                if (svd.getSingularValues() == null) {
                    System.err.println("Unable to use SVD implementation. Aborting.");
                    System.exit(10);
                } else {
                    System.out.println("Test complete.");
                }
            }

            progressLogger.expectedUpdates = numSamples;
            progressLogger.start("Starting to read signal for samples");

            for (int sampleIndex = 0; sampleIndex < numSamples; ++sampleIndex) {
                progressLogger.update();
                arrayReader.readNextSample(signal);
                final double[] dest = matrix[sampleIndex];
                for (int probesetIndex = minProbeIndex; probesetIndex < maxProbeIndex; ++probesetIndex) {
                    dest[probesetIndex] = (double) signal[probesetIndex];
                }
            }
            arrayReader.close();

            progressLogger.stop("Finished to read input signal");
            progressLogger.expectedUpdates = 2;


            progressLogger.start("Computing SVD");
            System.out.print("Computing SVD...");
            progressLogger.update();


            if (maxSvdComponentNumber == -1) {
                maxSvdComponentNumber = matrix.length; // keep all components if not specified on the command line.
            }
            final int reducedMatrixRank = maxSvdComponentNumber;
            svd.svd(matrix, maxSvdComponentNumber, 0);

            TextIO.storeDoubles(svd.getSingularValues(), "singular-values.txt");
            System.out.println("done");
            progressLogger.update();
            progressLogger.stop("Finished computing SVD.");
            System.out.flush();
            System.out.println("Initial matrix dimensions " + maxProbeIndex + " x " + numSamples);
            System.out.println("Reduced rank estimated to be " + reducedMatrixRank);
            System.out.println("Will write  " + maxProbeIndex + " x " + reducedMatrixRank);
            final ArrayWriter arrayWriter = getWriter(platform);


            final double[][] U = svd.getU();
            double[] signalOutput;
            final float[] fSignalOut = new float[maxProbeIndex];
            progressLogger.expectedUpdates = reducedMatrixRank;
            progressLogger.start("Starting to write reduced signal");

            final double[] S = svd.getSingularValues();
            // scale all values by a factor. If scaleWithFirstSingularValue=true, use the first singular value
            // for all components. This choice means
            // that each SVD component is given the same importance with respect to TEPSS scoring.
            // We scale values so that the conversion to integers will not result in all zeroes and most
            // TEPSS scorers have a numeric value in an appropriate integer range.
            // Scaling each column by its own sigular value could be tried instead if we were calculating TEPSS scores
            // with floating precision numbers.
            // If scaleWithFirstSingularValue=false, each component is scaled with its corresponding singular value.
            //
            /*    for (int c = 0; c < svd.getU().length; c++) {
                System.out.println("U[" + c + "]");
                TextIO.storeDoubles(svd.getU()[c], System.out);
            }*/
            for (int newSampleIndex = 0; newSampleIndex < reducedMatrixRank; newSampleIndex++) {
                final double scalingFactor = scaleWithFirstSingularValue ? S[0] : S[newSampleIndex];

                signalOutput = U[newSampleIndex];   // column index, then row index

                int i = 0;
                float max = 0;
                for (final double value : signalOutput) {

                    fSignalOut[i] = (float) ((absoluteSignal ? Math.abs(value) : value) * scalingFactor);
                    max = Math.max(max, fSignalOut[i]);
                    i++;
                }
                if (max < 2) {
                    // scale up if signal too small for integer coding.
                    for (int k = 0; k < fSignalOut.length; ++k) {
                        fSignalOut[k] *= 1000;
                    }
                }
                final MutableString newSampleId =
                        new MutableString("SVDComponent_");
                newSampleId.append(Integer.toString(newSampleIndex));
                arrayWriter.appendSample(fSignalOut, newSampleId);
                progressLogger.update();
            }
            arrayWriter.close();
            progressLogger.stop("Finished writting reduced signal");
        } catch (IOException
                e) {
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
            this.SVDImplementationName = CLI.getOption(opts, "--svd-implementation", "R");
            System.out.println("Will use SVD Implementation: " + this.SVDImplementationName);
            this.maxSvdComponentNumber = CLI.getIntOption(opts, "--max-svd-components", -1);
            System.out.println("Will caculate k SVD components, k=" + this.maxSvdComponentNumber);
            this.maxSampleNumber = CLI.getIntOption(opts, "--max-samples", -1);
            System.out.println("Will consider a maximum of " + this.maxSampleNumber + " samples.");
            this.testSVD = CLI.isKeywordGiven(opts, "--test-svd");
            this.scaleWithFirstSingularValue = !CLI.isKeywordGiven(opts, "--scale-with-each-singular-value");
            if (this.scaleWithFirstSingularValue) {
                System.out.println("Will scale each component with the first singular value.");
            } else {
                System.out.println("Will scale each component with its corresponding singular value.");
            }
            if (this.testSVD) {
                System.out.println("Will test SVD calculation early");
            }

            this.absoluteSignal = CLI.isKeywordGiven(opts, "--abs");
            if (absoluteSignal) {
                System.out.println("Will store absolute value of SVD components: " + this.outputBasename);
            }
        } else {
            System.out.println("adapter options were null.");
        }
    }
}
