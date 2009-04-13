/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

package org.bdval;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.util.RandomAdapter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

/**
 * Creates a synthetic biomarker discovery dataset from specifications.
 *
 * @author Fabien Campagne
 *         Date: Mar 20, 2008
 *         Time: 12:09:38 PM
 */
public class MakeSyntheticDataset {
    private RandomAdapter randomAdapter;

    private double scalePositiveLabel = 1.0d;
    private double meanPositiveLabel = 3.7d;
    private double meanNegativeLabel = 3.0d;
    private double scaleNonInformativeFeature = 1.0d;
    private double meanNonInformativeFeature = 2.9d;
    private double trainingVsTestingSizeRatio = 0.6d;
    private double scaleNegativeLabel = scalePositiveLabel;
    private String outputDirectory = "synthetic";

    private MakeSyntheticDataset() {
        super();
    }

    public static void main(final String[] args) throws IOException, JSAPException {
        final JSAP jsap = new JSAP();
        final MakeSyntheticDataset processor = new MakeSyntheticDataset();

        final Parameter datasetNameOption = new FlaggedOption("dataset-name")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setLongFlag("dataset-name")
                .setHelp("Name of the dataset to generate. A number of files will be produced "
                        + "for each dataset: dataset.tasks, dataset.cids, dataset.tmm");
        jsap.registerParameter(datasetNameOption);

        final Parameter numProbesetOption = new FlaggedOption("probeset-number")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("1000")
                .setRequired(false)
                .setLongFlag("probeset-number")
                .setHelp("Total number of probesets");
        jsap.registerParameter(numProbesetOption);

        final Parameter numPositiveSamplesOption = new FlaggedOption("positive-sample-number")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("150")
                .setRequired(false)
                .setLongFlag("positive-sample-number")
                .setHelp("Number of positive samples.");
        jsap.registerParameter(numPositiveSamplesOption);

        final Parameter numSamplesOption = new FlaggedOption("sample-number")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("300")
                .setRequired(false)
                .setLongFlag("sample-number")
                .setHelp("Total number of samples");
        jsap.registerParameter(numSamplesOption);

        final Parameter numInformativeProbesetsOption =
                new FlaggedOption("number-informative-probesets")
                        .setStringParser(JSAP.INTEGER_PARSER)
                        .setDefault("20")
                        .setRequired(true)
                        .setShortFlag('n')
                        .setLongFlag("number-informative-probesets")
                        .setHelp("Number of informative probesets.");
        jsap.registerParameter(numInformativeProbesetsOption);

        final Parameter scalePositiveLabelOption = new FlaggedOption("scale-positive-labels")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(Double.toString(processor.scalePositiveLabel))
                .setRequired(false)
                .setLongFlag("scale-positive-labels")
                .setHelp("Scale of the Gaussian distribution that generates signal for the "
                        + "positive labels.");
        jsap.registerParameter(scalePositiveLabelOption);

        final Parameter meanPositiveLabelOption = new FlaggedOption("mean-positive-labels")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(Double.toString(processor.meanPositiveLabel))
                .setRequired(false)
                .setLongFlag("mean-positive-labels")
                .setHelp("Mean of the Gaussian distribution that generates signal for the "
                        + "positive labels.");
        jsap.registerParameter(meanPositiveLabelOption);

        final Parameter scaleNegativeLabelOption = new FlaggedOption("scale-negative-labels")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(Double.toString(processor.scaleNegativeLabel))
                .setRequired(false)
                .setLongFlag("scale-negative-labels")
                .setHelp("Scale of the Gaussian distribution that generates signal for the "
                        + "negative labels.");
        jsap.registerParameter(scaleNegativeLabelOption);

        final Parameter meanNegativeLabelOption = new FlaggedOption("mean-negative-labels")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(Double.toString(processor.meanNegativeLabel))
                .setRequired(false)
                .setLongFlag("mean-negative-labels")
                .setHelp("Mean of the Gaussian distribution that generates signal for the "
                        + "negative labels.");
        jsap.registerParameter(meanNegativeLabelOption);


        final Parameter scaleNonInformativeOption = new FlaggedOption("scale-non-informative")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(Double.toString(processor.scaleNonInformativeFeature))
                .setRequired(false)
                .setLongFlag("scale-non-informative")
                .setHelp("Scale of the Gaussian distribution that generates signal for the non "
                        + "informative probesets.");
        jsap.registerParameter(scaleNonInformativeOption);

        final Parameter meanNonInformativeOption = new FlaggedOption("mean-non-informative")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(Double.toString(processor.meanNonInformativeFeature))
                .setRequired(false)
                .setLongFlag("mean-non-informative")
                .setHelp("Mean of the Gaussian distribution that generates signal for the non "
                        + "informative probesets.");
        jsap.registerParameter(meanNonInformativeOption);

        final Parameter outputDirectoryOption = new FlaggedOption("output-directory")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(processor.outputDirectory)
                .setRequired(true)
                .setLongFlag("output-directory")
                .setHelp("Directory to write output files to");
        jsap.registerParameter(outputDirectoryOption);

        final JSAPResult parsedArguments = jsap.parse(args);
        processor.process(parsedArguments);
    }

    private void process(final JSAPResult arguments) throws IOException {
        outputDirectory = arguments.getString("output-directory");
        FileUtils.forceMkdir(new File(outputDirectory));

        final int numProbesets = arguments.getInt("probeset-number");
        final int numSamples = arguments.getInt("sample-number");
        final int numPositiveSamples = arguments.getInt("positive-sample-number");

        final int numInformativeProbesets = arguments.getInt("number-informative-probesets");
        final String outputFilenamePrefix = arguments.getString("dataset-name");

        scalePositiveLabel = arguments.getDouble("scale-positive-labels");
        meanPositiveLabel = arguments.getDouble("mean-positive-labels");

        scaleNegativeLabel = arguments.getDouble("scale-negative-labels");
        meanNegativeLabel = arguments.getDouble("mean-negative-labels");

        scaleNonInformativeFeature = arguments.getDouble("scale-non-informative");
        meanNonInformativeFeature = arguments.getDouble("mean-non-informative");

        final double[][] data = new double[numSamples][numProbesets];

        final RandomEngine random = new MersenneTwister();
        randomAdapter = new RandomAdapter(random);

        printStats(numProbesets, numSamples, numPositiveSamples, numInformativeProbesets, outputFilenamePrefix, new PrintWriter(System.out));

        // pick informative probeset indices, making sure indices are not picked more than once.
        final IntSet informativeProbesetIndices =
                generateRandomIndices(numInformativeProbesets, numProbesets, randomAdapter);
        final IntSet positiveSampleIndices =
                generateRandomIndices(numPositiveSamples, numSamples, randomAdapter);

        for (final double[] sample : data) {    // for each sample:
            for (int probesetIndex = 0; probesetIndex < sample.length; probesetIndex++) { // for each  probeset:
                sample[probesetIndex] = generateNonInformativeFeatureValue();
            }
        }

        int sampleIndex = 0;
        for (final double[] sample : data) {    // for each sample:
            for (final int informativeProbesetIndex : informativeProbesetIndices) { // for each informative probeset:
                sample[informativeProbesetIndex] =
                        generateInformativeFeatureValue(positiveSampleIndices.contains(sampleIndex));
            }
            sampleIndex++;
        }

        final String datasetFilename = FilenameUtils.concat(outputDirectory,
                FilenameUtils.concat("norm-data", outputFilenamePrefix + ".tmm"));
        FileUtils.forceMkdir(new File(FilenameUtils.getPath(datasetFilename)));
        PrintWriter datasetWriter = null;
        try {
            datasetWriter = new PrintWriter(datasetFilename);
            outputDataset(data, informativeProbesetIndices, positiveSampleIndices, datasetWriter);
        } finally {
            IOUtils.closeQuietly(datasetWriter);
        }

        final IntList sampleIndicesList =
                outputCids(numSamples, positiveSampleIndices, outputFilenamePrefix);
        outputTrainingAndTestingCids(positiveSampleIndices, outputFilenamePrefix, sampleIndicesList);
        Collections.shuffle(sampleIndicesList);
        final IntList trainingSetSampleList =
                sampleIndicesList.subList(0, (int) (sampleIndicesList.size() * trainingVsTestingSizeRatio));
        final IntList testingSetSampleList =
                sampleIndicesList.subList((int) (sampleIndicesList.size() * trainingVsTestingSizeRatio) + 1,
                        sampleIndicesList.size());
        outputCids(positiveSampleIndices, outputFilenamePrefix + "Training", trainingSetSampleList);
        outputCids(positiveSampleIndices, outputFilenamePrefix + "Testing", testingSetSampleList);

        final IntSet positiveInCompleteSet = new IntLinkedOpenHashSet();
        positiveInCompleteSet.addAll(sampleIndicesList);
        positiveInCompleteSet.retainAll(positiveSampleIndices);
        System.out.println("positiveInCompleteSet: " + positiveInCompleteSet.size());
        // task for full training set:
        outputTasks(outputFilenamePrefix,
                numSamples, positiveInCompleteSet, outputFilenamePrefix + ".tasks");

        // task for training set only:
        final IntSet positiveInTrainingSet = new IntLinkedOpenHashSet();
        positiveInTrainingSet.addAll(trainingSetSampleList);
        positiveInTrainingSet.retainAll(positiveSampleIndices);
        System.out.println("positiveInTrainingSet: " + positiveInTrainingSet.size());

        outputTasks(outputFilenamePrefix + "_Training",
                trainingSetSampleList.size(),
                positiveInTrainingSet, outputFilenamePrefix + "_Training" + ".tasks");
        //task for test set only:
        final IntSet positiveInTestingSet = new IntLinkedOpenHashSet();
        positiveInTestingSet.addAll(testingSetSampleList);
        positiveInTestingSet.retainAll(positiveSampleIndices);
        System.out.println("positiveInTestingSet: " + positiveInTestingSet.size());
        outputTasks(outputFilenamePrefix + "_Testing",
                testingSetSampleList.size(),
                positiveInTestingSet, outputFilenamePrefix + "_Testing" + ".tasks");

        final String summaryFilename =
                FilenameUtils.concat(outputDirectory, outputFilenamePrefix + "-README.txt");
        PrintWriter summaryWriter = null;
        try {
            summaryWriter = new PrintWriter(summaryFilename);
            printStats(numProbesets, numSamples, numPositiveSamples, numInformativeProbesets, outputFilenamePrefix, summaryWriter);
        } finally {
            IOUtils.closeQuietly(summaryWriter);
        }
    }

    private void printStats(final int numProbesets, final int numSamples,
                            final int numPositiveSamples, final int numInformativeProbesets,
                            final String outputFilenamePrefix, final PrintWriter summaryWriter) {
        summaryWriter.println(outputFilenamePrefix + " was generated with the following parameters: ");
        summaryWriter.println(String.format(
                "--probeset-number %d" + SystemUtils.LINE_SEPARATOR
                        + "--sample-number %d" + SystemUtils.LINE_SEPARATOR
                        + "--positive-sample-number %d" + SystemUtils.LINE_SEPARATOR
                        + "--number-informative-probesets %d" + SystemUtils.LINE_SEPARATOR
                        + "--scale-positive-labels %f" + SystemUtils.LINE_SEPARATOR
                        + "--scale-non-informative %f" + SystemUtils.LINE_SEPARATOR
                        + "--scale-negative-labels %f" + SystemUtils.LINE_SEPARATOR
                        + "--mean-positive-labels %f" + SystemUtils.LINE_SEPARATOR
                        + "--mean-negative-labels %f" + SystemUtils.LINE_SEPARATOR
                        + "--mean-non-informative %f" + SystemUtils.LINE_SEPARATOR
                        + "--dataset-name %s" + SystemUtils.LINE_SEPARATOR,
                numProbesets,
                numSamples, numPositiveSamples, numInformativeProbesets, scalePositiveLabel,
                scaleNonInformativeFeature, scaleNegativeLabel,
                meanPositiveLabel, meanNegativeLabel, meanNonInformativeFeature,
                outputFilenamePrefix));
        summaryWriter.flush();
    }

    private void outputTasks(final String outputFilenamePrefix,
                             final int numSamples, final IntSet positiveSampleIndices,
                             final String outputFilename) throws IOException {
        final String tasksFilename = FilenameUtils.concat(outputDirectory,
                FilenameUtils.concat("tasks", outputFilename));
        FileUtils.forceMkdir(new File(FilenameUtils.getPath(tasksFilename)));

        PrintWriter tasksWriter = null;
        try {
            tasksWriter =  new PrintWriter(tasksFilename);

            tasksWriter.println(String.format("%s\tnegative\tpositive\t%d\t%d",
                    outputFilenamePrefix, numSamples - positiveSampleIndices.size(),
                    positiveSampleIndices.size()));
        } finally {
            IOUtils.closeQuietly(tasksWriter);
        }
    }

    private IntList outputCids(final int numSamples, final IntSet positiveSampleIndices,
                               final String outputFilenamePrefix) throws IOException {
        // make cids for full training set:
        final IntList sampleIndices = new IntArrayList();

        for (int sampleIndex = 0; sampleIndex < numSamples; sampleIndex++) {
            sampleIndices.add(sampleIndex);
        }
        outputCids(positiveSampleIndices, outputFilenamePrefix, sampleIndices);
        return sampleIndices;
    }

    private void outputTrainingAndTestingCids(final IntSet positiveSampleIndices,
                                              final String outputFilenamePrefix,
                                              final IntList sampleIndices) throws IOException {
        outputCids(positiveSampleIndices, outputFilenamePrefix + "TrainingAndTesting", sampleIndices);
    }

    private void outputCids(final IntSet positiveSampleIndices,
                            final String outputFilenamePrefix,
                            final IntList sampleIndices) throws IOException {
        final String cidsFilename = FilenameUtils.concat(outputDirectory,
                FilenameUtils.concat("cids", outputFilenamePrefix + ".cids"));
        FileUtils.forceMkdir(new File(FilenameUtils.getPath(cidsFilename)));
        PrintWriter cidsWriter = null;
        try {
            cidsWriter = new PrintWriter(cidsFilename);

            for (final int sampleIndex : sampleIndices) {
                cidsWriter.print(positiveSampleIndices.contains(sampleIndex) ? "positive" : "negative");
                cidsWriter.print("\t");
                cidsWriter.print(sampleId(sampleIndex, positiveSampleIndices));
                cidsWriter.println();
            }
        } finally {
            IOUtils.closeQuietly(cidsWriter);
        }
    }

    private void outputDataset(final double[][] data,
                               final IntSet informativeProbesetIndices,
                               final IntSet positiveSampleIndices, final PrintWriter out) {
        // print sample ids:
        out.print("ID_REF\t");
        for (int sampleIndex = 0; sampleIndex < data.length; sampleIndex++) {
            out.print(String.format("%s\t", sampleId(sampleIndex, positiveSampleIndices)));
        }
        out.println();
        for (int probesetIndex = 0; probesetIndex < data[0].length; probesetIndex++) { // for each  probeset:
            out.print(String.format("%s\t", probesetId(probesetIndex, informativeProbesetIndices)));
            for (int sampleIndex = 0; sampleIndex < data.length; sampleIndex++) {    // for each sample:
                final double value = data[sampleIndex][probesetIndex];
                out.print(String.format("%3.3f\t", value));
            }
            out.println();
        }
    }

    private String sampleId(final int sampleIndex, final IntSet positiveSampleIndices) {
        if (positiveSampleIndices.contains(sampleIndex)) {
            return "sample_" + Integer.toString(sampleIndex) + "(positive)";
        } else {
            return "sample_" + Integer.toString(sampleIndex) + "(negative)";
        }
    }

    private String probesetId(final int probesetIndex, final IntSet informativeProbesetIndices) {
        if (informativeProbesetIndices.contains(probesetIndex)) {
            return "informative_" + Integer.toString(probesetIndex);
        } else {
            return "other_" + Integer.toString(probesetIndex);
        }
    }

    private IntSet generateRandomIndices(final int numInformativeProbesets,
                                         final int maxValue,
                                         final RandomAdapter randomAdapter) {
        final IntSet chosen = new IntArraySet();
        for (int i = 0; i < numInformativeProbesets; i++) {
            int choice;
            do {
                choice = randomAdapter.choose(0, maxValue - 1);
            } while (chosen.contains(choice));

            chosen.add(choice);
        }
        return chosen;
    }

    private double generateInformativeFeatureValue(final boolean isPositiveLabel) {
        return randomAdapter.nextGaussian() *
                (isPositiveLabel ? scalePositiveLabel : scaleNegativeLabel) +
                (isPositiveLabel ? meanPositiveLabel : meanNegativeLabel);
    }

    private double generateNonInformativeFeatureValue() {
        return randomAdapter.nextGaussian() * scaleNonInformativeFeature +
                meanNonInformativeFeature;
    }
}
