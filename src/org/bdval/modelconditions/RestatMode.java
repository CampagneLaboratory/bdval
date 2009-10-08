package org.bdval.modelconditions;

import com.martiansoftware.jsap.*;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import edu.cornell.med.icb.learning.tools.svmlight.EvaluationMeasure;
import edu.cornell.med.icb.learning.CrossValidation;
import org.bdval.*;

/**
 * Calculates statistics from model conditions and a set of results directories.
 *
 * @author Fabien Campagne
 *         Date: Oct 6, 2009
 *         Time: 5:34:23 PM
 */
public class RestatMode extends ProcessModelConditionsMode {
    private final MaqciiHelper maqciiHelper = new MaqciiHelper();
    private String survivalFileName;
    private DAVOptions davOptions;
    StatsEvaluationType statsEvalType = StatsEvaluationType.STATS_PER_REPEAT;


    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter survivalFilenameOption = new FlaggedOption("survival")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("survival")
                .setHelp("Survival filename. This file contains survival data "
                        + "in tab delimited format; column 1: chipID has to match cids and "
                        + "tmm, column 2: time to event, column 3 censor with 1 as event 0 "
                        + "as censor, column 4 and beyond are numerical covariates that "
                        + "will be included in the regression model");
        jsap.registerParameter(survivalFilenameOption);

        final Parameter typeOfSplitHandling = new FlaggedOption("aggregation-method")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault("per-repeat")
                .setRequired(false)
                .setLongFlag("aggregation-method")
                .setHelp("Type of aggregation method. Predictions can be aggregated by repeat of cross-validation " +
                        "(per-repeat, default value of this option), or by test set (per-test-set). ");
        jsap.registerParameter(typeOfSplitHandling);

        maqciiHelper.defineSubmissionFileOption(jsap);
        jsap.getByID("label").addDefault("auto");
        // jsap.getByID("folds").addDefault("0");
    }

    @Override
    public void interpretArguments(JSAP jsap, JSAPResult jsapResult, ProcessModelConditionsOptions options) {
        super.interpretArguments(jsap, jsapResult, options);
        if (jsapResult.contains("survival")) {
            survivalFileName = jsapResult.getString("survival");
            LOG.debug("Survival file:" + survivalFileName);
        }
        final String aggregationMethod = jsapResult.getString("aggregation-method");
        if (aggregationMethod == null) {
            statsEvalType = StatsEvaluationType.STATS_PER_REPEAT;
        } else {
            if ("per-repeat".equalsIgnoreCase(aggregationMethod)) {
                statsEvalType = StatsEvaluationType.STATS_PER_REPEAT;
            } else if ("per-test-set".equalsIgnoreCase(aggregationMethod)) {
                statsEvalType = StatsEvaluationType.STATS_PER_SPLIT;
            } else {
                System.err.println("Cannot parse argument of option --aggregation-method");
                System.exit(1);
            }
        }
        davOptions = new DAVOptions();
        maqciiHelper.setupSubmissionFile(jsapResult, davOptions);
    }

    @Override
    public void processOneModelId(ProcessModelConditionsOptions options, String modelId) {
        //  System.out.println("will process model id:" + modelId);
        final PredictedItems predictions = loadPredictions(modelId);
        if (predictions != null) {
            System.out.println("Processing predictions for model id " + modelId);
            davOptions.crossValidationFoldNumber = predictions.getNumberOfFolds();
            davOptions.datasetName = getDatasetName(modelId);
            maqciiHelper.setLabel(constructLabel(modelId));
            final int numberOfRepeats = predictions.getNumberOfRepeats();
            final ObjectSet<CharSequence> evaluationMeasureNames = new ObjectArraySet<CharSequence>();
            evaluationMeasureNames.addAll(Arrays.asList(org.bdval.Predict.MEASURES));
            final EvaluationMeasure repeatedEvaluationMeasure = new EvaluationMeasure();

            final List<SurvivalMeasures> survivalMeasuresList = new ArrayList<SurvivalMeasures>();
            switch (statsEvalType) {
                case STATS_PER_REPEAT:
                    evaluatePerformanceMeasurePerRepeat(predictions, null, survivalMeasuresList,
                            numberOfRepeats, evaluationMeasureNames, repeatedEvaluationMeasure);
                    break;
                case STATS_PER_SPLIT:
                    evaluatePerformanceMeasurePerTestSet(predictions, null, survivalMeasuresList,
                            numberOfRepeats, evaluationMeasureNames, repeatedEvaluationMeasure);
                    break;
            }

            final int numberOfFeatures = predictions.modelNumFeatures();
            davOptions.modelId = modelId;

            maqciiHelper.printSubmissionHeaders(davOptions, survivalFileName != null);

            maqciiHelper.printSubmissionResults(davOptions, repeatedEvaluationMeasure,
                    numberOfFeatures, numberOfRepeats, survivalMeasuresList);
        }
    }

    public static void evaluatePerformanceMeasurePerTestSet(PredictedItems predictions,
                                                            String survivalFileName, final List<SurvivalMeasures> survivalMeasuresList,
                                                            final int numberOfRepeats,
                                                            final ObjectSet<CharSequence> evaluationMeasureNames,
                                                            final EvaluationMeasure repeatedEvaluationMeasure) {

        // Collect one evaluation measure per split test set of cross-validation.

        for (int repeatId = 1; repeatId <= numberOfRepeats; repeatId++) {
            if (predictions.containsRepeat(repeatId)) {
                final int maxSplitId = predictions.getNumberOfSplitsForRepeat(repeatId);
                for (final int splitId : predictions.splitIdsForRepeat(repeatId)) {
                    final DoubleList decisions = new DoubleArrayList();
                    final DoubleList trueLabels = new DoubleArrayList();
                    final ObjectList<String> sampleIDs = new ObjectArrayList<String>();

                    decisions.addAll(predictions.getDecisionsForSplit(repeatId, splitId));
                    trueLabels.addAll(predictions.getTrueLabelsForSplit(repeatId, splitId));
                    sampleIDs.addAll(predictions.getSampleIDsForSplit(repeatId, splitId));
                    if (decisions.size() == 0) {
                        LOG.fatal("cannot process empty decision list");
                        System.exit(10);
                    }
                    final EvaluationMeasure allSplitsInARepeatMeasure =
                            CrossValidation.testSetEvaluation(decisions.toDoubleArray(),
                                    trueLabels.toDoubleArray(), evaluationMeasureNames, true);

                    if (survivalFileName != null) {
                        try {
                            final SurvivalMeasures survivalMeasures = new SurvivalMeasures(
                                    survivalFileName, decisions, trueLabels,
                                    sampleIDs.toArray(new String[sampleIDs.size()]));
                            survivalMeasuresList.add(survivalMeasures);
                        } catch (IOException e) {
                            LOG.fatal("Error processing input file ", e);
                            System.exit(10);
                        }
                    }
                    for (final CharSequence measure : Predict.MEASURES) {
                        repeatedEvaluationMeasure.addValue(measure,
                                allSplitsInARepeatMeasure.getPerformanceValueAverage(measure.toString()));
                        final String binaryName = ("binary-" + measure).intern();
                        repeatedEvaluationMeasure.addValue(binaryName,
                                allSplitsInARepeatMeasure.getPerformanceValueAverage(binaryName));
                    }
                    LOG.trace(String.format("repeatId: %d %s", repeatId, allSplitsInARepeatMeasure.toString()));
                }
            }
        }
    }

    public static void evaluatePerformanceMeasurePerRepeat(
            PredictedItems predictions, String survivalFileName, final List<SurvivalMeasures> survivalMeasuresList,
            final int numberOfRepeats, final ObjectSet<CharSequence> evaluationMeasureNames, final EvaluationMeasure repeatedEvaluationMeasure) {
        for (int repeatId = 1; repeatId <= numberOfRepeats; repeatId++) {
            if (predictions.containsRepeat(repeatId)) {
                final DoubleList decisions = new DoubleArrayList();
                final DoubleList trueLabels = new DoubleArrayList();
                final ObjectList<String> sampleIDs = new ObjectArrayList<String>();

                decisions.addAll(predictions.getDecisionsForRepeat(repeatId));
                trueLabels.addAll(predictions.getTrueLabelsForRepeat(repeatId));
                sampleIDs.addAll(predictions.getSampleIDsForRepeat(repeatId));
                final EvaluationMeasure allSplitsInARepeatMeasure =
                        CrossValidation.testSetEvaluation(decisions.toDoubleArray(),
                                trueLabels.toDoubleArray(), evaluationMeasureNames, true);

                if (survivalFileName != null) {
                    try {
                        final SurvivalMeasures survivalMeasures = new SurvivalMeasures(
                                survivalFileName, decisions, trueLabels,
                                sampleIDs.toArray(new String[sampleIDs.size()]));
                        survivalMeasuresList.add(survivalMeasures);
                    } catch (IOException e) {
                        LOG.fatal("Error processing input file ", e);
                        System.exit(10);
                    }
                }

                for (final CharSequence measure : Predict.MEASURES) {
                    repeatedEvaluationMeasure.addValue(measure,
                            allSplitsInARepeatMeasure.getPerformanceValueAverage(measure.toString()));
                    final String binaryName = ("binary-" + measure).intern();
                    repeatedEvaluationMeasure.addValue(binaryName,
                            allSplitsInARepeatMeasure.getPerformanceValueAverage(binaryName));
                }
                LOG.trace(String.format("repeatId: %d %s", repeatId, allSplitsInARepeatMeasure.toString()));
            }
        }
    }


    public enum StatsEvaluationType {
        STATS_PER_REPEAT,
        STATS_PER_SPLIT,
    }
}
