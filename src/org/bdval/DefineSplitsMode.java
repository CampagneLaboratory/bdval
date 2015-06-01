/*
 * Copyright (C) 2008-2010 Institute for Computational Biomedicine,
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

import com.martiansoftware.jsap.*;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.RegressionTask;
import edu.cornell.med.icb.util.RandomAdapter;
import it.unimi.dsi.fastutil.doubles.DoubleArraySet;
import it.unimi.dsi.fastutil.doubles.DoubleSet;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

/**
 * Partition a training set into various splits for training and testing. A typical split design is cross-validation,
 * but other splitting strategies are possible (though not supported at this time). This tool generates a file which precisely
 * describes how the samples in the whole training set should be distributed into splits.
 * The generated file consists of lines of the form:
 * <PRE>
 * repeat#  split#  fold-type    sample-id   numeric-class-label sample-index
 * </PRE>
 * Repeat# (integer) identifies a (random) repetition of the split strategy.
 * Split# is an integer which uniquely identifies a split.
 * Fold-type (string) identifies the purpose of the fold in a given split. Samples which have a fold-type=training
 * should be used for training the model, whereas samples with fold-type=test should be used to test the model.
 * Sample-id is a string which indicates that the corresponding sample is part of the split/fold described.
 * The last two columns are optional and useful to
 * The following encodes a leave-one-out split strategy with three samples (6 folds for three splits):
 * <PRE>
 * 1 1 training sample2
 * 1 1 training sample3
 * 1 1 test sample1
 * 1 2 training sample1
 * 1 2 training sample3
 * 1 2 test sample2
 * 1 3 training sample1
 * 1 3 training sample2
 * 1 3 test sample3
 * </PRE>
 * This encoding makes it possible to devise strategies that define several partitions of the input samples.
 * For instance, it is possible to define feature-selection, training and test fold-types, in the context
 * of cross-validation with a number of random repeats. The split plan can also be generated independently of
 * DefineSplitsMode and given to execute-splits.
 *
 * @author Fabien Campagne
 *         Date: Apr 2, 2008
 *         Time: 2:02:31 PM
 */
public class DefineSplitsMode extends DAVMode {
    private int cvRepeatNumber;
    static final NumberFormat formatter;
    private RandomAdapter randomAdapter;
    private boolean stratification;
    private static final Log LOG = LogFactory.getLog(DefineSplitsMode.class);
    private boolean createFeatureSelectionFold;

    static {
        formatter = new DecimalFormat();
        formatter.setMaximumFractionDigits(2);
    }

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter foldNumber =
                new FlaggedOption("folds")
                        .setStringParser(JSAP.INTEGER_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('f')
                        .setLongFlag("folds")
                        .setHelp("Number of cross validation folds.");
        jsap.registerParameter(foldNumber);

        final Parameter cvRepeatNumberParam =
                new FlaggedOption("cv-repeats")
                        .setStringParser(JSAP.INTEGER_PARSER)
                        .setDefault("1")
                        .setRequired(false)
                        .setLongFlag("cv-repeats")
                        .setHelp("Number of cross validation repeats. default=1 (does on round of cross-validation). " +
                                "Values larger than one cause the cross validation to be repeated and results averaged" +
                                "over the rounds.");
        jsap.registerParameter(cvRepeatNumberParam);

        final Parameter stratificationParam =
                new FlaggedOption("stratification")
                        .setStringParser(JSAP.BOOLEAN_PARSER)
                        .setDefault("true")
                        .setRequired(false)
                        .setLongFlag("stratification")
                        .setHelp("When true, each random fold is constrained to contain the same proportion of positive samples" +
                                "as the whole input set (modulo integer rounding errors). Default is true.");
        jsap.registerParameter(stratificationParam);
        final Parameter featureSelectionFoldParam =
                new FlaggedOption("feature-selection-fold")
                        .setStringParser(JSAP.BOOLEAN_PARSER)
                        .setDefault("false")
                        .setRequired(false)
                        .setLongFlag("feature-selection-fold")
                        .setHelp("When true, one fold is labeled for feature selection (split-type=feature-selection) and excluded from the training split. Default false.");
        jsap.registerParameter(featureSelectionFoldParam);
    }

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        super.interpretArguments(jsap, result, options);
        if (result.contains("folds")) {
            final int foldNumber = result.getInt("folds");
            options.crossValidationFoldNumber = foldNumber;
        }
        this.cvRepeatNumber = result.getInt("cv-repeats");
        this.randomAdapter = new RandomAdapter(options.randomGenerator);
        this.stratification = result.getBoolean("stratification");
        this.createFeatureSelectionFold = result.getBoolean("feature-selection-fold");
        if (createFeatureSelectionFold) {
            LOG.info("A fold will be marked for feature selection.");
        }
    }
    ClassificationTask task;
    @Override
    public void process(final DAVOptions options) {
        super.process(options);
        if (options.classificationTasks.length > 1) {
            System.err.println("Task file must contain exactly one task.");
            System.exit(1);
        }
        final ClassificationTask task = options.classificationTasks[0];
        final Set<String> sampleIdsClass0 = task.getConditionsIdentifiers().getLabelGroup(task.getFirstConditionName());
        final Set<String> sampleIdsClass1 = task.getConditionsIdentifiers().getLabelGroup(task.getSecondConditionName());

        final SortedSet<String> allSampleIds = new ObjectAVLTreeSet<String>();
        this.task=task;
        if (task.isRegression()) {

            System.err.println("Disabling stratification because this task is a regression.");
            // disable stratification when the task is a regression. There is nothing to stratify for and stratification
            // will fail.
            stratification=false;
        }
        allSampleIds.addAll(sampleIdsClass0);
        allSampleIds.addAll(sampleIdsClass1);
        final ObjectList<String> allSampleIdsList=new ObjectArrayList<String>();
        allSampleIdsList.addAll(allSampleIds);
        options.output.println(String.format("# split-id\t repeat-id\tfold-id\tsplit-type\tsample-id\tsample-index\tsample-class-label\tsampleIndex "));
        final int numberOfSamples = allSampleIds.size();
        int splitIndex = 1;
        final int k = options.crossValidationFoldNumber;
        for (int r = 0; r < cvRepeatNumber; r++) {
            assert k <= numberOfSamples : "Number of folds must be less or equal to number of training examples.";

            final int[] foldIndices = assignFolds(options.crossValidationFoldNumber, numberOfSamples, randomAdapter,
                    sampleIdsClass1, allSampleIdsList);
            for (int f = 0; f < k; ++f) { // use each fold as test set while the others are the training set:
                final IntSet trainingSet = new IntArraySet();
                final IntSet testSet = new IntArraySet();
                // an optional fold that can be used for feature selection before training:
                final IntSet featureSelectionSet = new IntArraySet();
                final int featureSelectionFoldIndex = f >= 1 ? f - 1 : k - 1;
                for (int i = 0; i < numberOfSamples; i++) {   // assign each training example to a fold:
                    if (f == foldIndices[i]) {
                        testSet.add(i);
                    } else {
                        if (createFeatureSelectionFold && featureSelectionFoldIndex == foldIndices[i]) {
                            featureSelectionSet.add(i);
                        } else {
                            trainingSet.add(i);
                        }
                    }
                }
                // sanity checks..
                assert testSet.size() + trainingSet.size() + featureSelectionSet.size() == numberOfSamples : "test set and training set size must add to whole problem size.";
                final IntSet intersection = new IntOpenHashSet();
                intersection.addAll(trainingSet);
                intersection.retainAll(testSet);
                assert intersection.isEmpty() : "test set and training set must never overlap";

                // output the fold assignments:
                if (createFeatureSelectionFold) {
                    for (final int sampleIndex : featureSelectionSet) {
                        options.output.println(String.format("%d\t%d\t%d\t%s\t%s\t%f\t%d",
                                splitIndex, r + 1, f + 1, "feature-selection", allSampleIdsList.get(sampleIndex),
                                getLabel(sampleIndex, sampleIdsClass1, allSampleIdsList), sampleIndex));
                    }
                }
                for (final int sampleIndex : trainingSet) {
                    options.output.println(String.format("%d\t%d\t%d\t%s\t%s\t%f\t%d",
                            splitIndex, r + 1, f + 1, "training", allSampleIdsList.get(sampleIndex),
                            getLabel(sampleIndex, sampleIdsClass1, allSampleIdsList), sampleIndex));
                }
                for (final int sampleIndex : testSet) {
                    options.output.println(String.format("%d\t%d\t%d\t%s\t%s\t%f\t%d",
                            splitIndex, r + 1, f + 1, "test", allSampleIdsList.get(sampleIndex),
                            getLabel(sampleIndex, sampleIdsClass1, allSampleIdsList), sampleIndex));
                }
                splitIndex++;
            }
        }
        options.output.flush();

    }

    public int[] assignFolds(final int k, final int numberOfSamples, final Random randomAdapter,
                             final Set<String> positiveSamples, final ObjectList<String> allSampleIds) {
        final IntList indices = new IntArrayList();
        int numAttempts = 0;
        do {
            indices.clear();
            for (int i = 0; i < numberOfSamples; ++i) {
                //         System.out.println("Assigning instance "+i+ " to fold "+(i % k));
                indices.add(i % k);
            }
            Collections.shuffle(indices, randomAdapter);
            numAttempts++;
            assert numAttempts < 1000 : "too many folds were tried without finding one that meets all the fold constraints.";
        } while (invalidFold(indices, k, positiveSamples, allSampleIds));
        final int[] splitIndex = new int[numberOfSamples];
        indices.toArray(splitIndex);
        return splitIndex;
    }

    private boolean invalidFold(final IntList indices, final int k,
                                final Set<String> positiveSamples, final ObjectList<String> allSampleIds) {
        final int numberOfSamples = allSampleIds.size();
        if (stratification) {
            final double numPositiveInWholeSet = positiveSamples.size();
            final double numNegativeInWholeSet = allSampleIds.size() - positiveSamples.size();
            final double instancesPerFold = numberOfSamples / k;

            for (int currentFoldInspected = 0; currentFoldInspected < k; currentFoldInspected++) {
                int tallyPositive = 0;
                int tallyNegative = 0;
                final DoubleSet labels = new DoubleArraySet();
                int instanceIndex = 0;
                int foldInstanceCount = 0;
                for (final int foldAssigment : indices) {
                    if (foldAssigment == currentFoldInspected) {
                        final double label = getLabel(instanceIndex, positiveSamples, allSampleIds);
                        labels.add(label);
                        tallyNegative += (label == 0 ? 1 : 0);
                        tallyPositive += (label == 1 ? 1 : 0);
                        foldInstanceCount++;
                    }
                    instanceIndex++;
                    //      System.out.println(String.format("fold assignment %d positive# %d negative# %d",foldAssigment, tallyPositive, tallyNegative));
                }
                assert tallyNegative + tallyPositive == foldInstanceCount :
                        "positive and negative label tallies must sum to total number of samples in fold.";

                final int expectedPositiveInFold = (int) Math.round((double) numPositiveInWholeSet /
                        ((double) numberOfSamples) * ((double) foldInstanceCount));
                final int expectedNegativeInFold = (int) Math.round(((double) numNegativeInWholeSet /
                        ((double) numberOfSamples) * (double) foldInstanceCount));

                if (tallyNegative > expectedNegativeInFold + 1 || tallyNegative < expectedNegativeInFold - 1 ||
                        tallyPositive > expectedPositiveInFold + 1 || tallyPositive < expectedPositiveInFold - 1) {
                    LOG.info(String.format("rejecting a fold assignment which violates stratification constraints. " +
                            "Expected %d, found %d negatives, expected %d, found %d positives", expectedNegativeInFold,
                            tallyNegative, expectedPositiveInFold, tallyPositive));
                    return true;
                }
                if (labels.size() < 2) {
                    LOG.info("rejecting a fold assignment which had only one label class in it.");
                    return true;
                }
            }
            return false;
        }
        // accept all folds for now
        return false;
    }

    private double getLabel(final int sampleIndex, final Set<String> positiveSamples, final ObjectList<String> allSampleIds) {
        if (this.task.isRegression()) {
            return ((RegressionTask ) this.task).getLabels().getLabel(allSampleIds.get(sampleIndex));
        }
        return positiveSamples.contains(allSampleIds.get(sampleIndex)) ? 1 : 0;
    }
}
