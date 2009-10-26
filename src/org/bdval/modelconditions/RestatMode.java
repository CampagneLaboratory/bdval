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

package org.bdval.modelconditions;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.learning.CrossValidation;
import edu.cornell.med.icb.learning.tools.svmlight.EvaluationMeasure;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bdval.DAVOptions;
import org.bdval.MaqciiHelper;
import org.bdval.Predict;
import org.bdval.PredictedItems;
import org.bdval.SurvivalMeasures;

import java.io.IOException;
import java.util.*;

/**
 * Calculates statistics from model conditions and a set of results directories.
 *
 * @author Fabien Campagne
 *         Date: Oct 6, 2009
 *         Time: 5:34:23 PM
 */
public class RestatMode extends ProcessModelConditionsMode {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(RestatMode.class);
    private final MaqciiHelper maqciiHelper = new MaqciiHelper();
    private String survivalFileName;
    private DAVOptions davOptions;
    StatsEvaluationType statsEvalType = StatsEvaluationType.STATS_PER_REPEAT;
    static final ObjectSet<CharSequence> evaluationMeasureNames = new ObjectArraySet<CharSequence>();

    static {
        evaluationMeasureNames.addAll(Arrays.asList(org.bdval.Predict.MEASURES));
    }

    @Override
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
                .setDefault("per-test-set")
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
    public void interpretArguments(final JSAP jsap, final JSAPResult jsapResult, final ProcessModelConditionsOptions options) {
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


    ArrayList<String> series = new ArrayList(); // list of series that have been processed
    EvaluationMeasure repeatedEvaluationMeasure = new EvaluationMeasure();
    PredictedItems predictions;
    int numberOfRepeats;
    final List<SurvivalMeasures> survivalMeasuresList = new ArrayList<SurvivalMeasures>();
    static Map<String, double[]> acrossAllFoldsMap = new HashMap<String, double[]>();
    int numberOfFeatures = 0;
    static int numberOfFolds = 0;

    @Override
    public void processSeries(ProcessModelConditionsOptions options, String modelId) {

        // group together models that are in the same series?
        String seriesID = options.modelConditions.get(modelId).get("id-parameter-scan-series");
        if (!(series.contains(seriesID))) {//checks whether a series has been processed before preceding
            series.add(seriesID);
            ArrayList<String> models = extractSeriesModelIds(options, seriesID);


        // for all models in a series get accuracy measures for each fold,
            for (int t = 0; t < models.size(); t++) {
                processOneModelIdPassOne(options, models.get(t));
            }

            // across the series, record mininum theta_k across each fold

            // map that stores the minimum theta_k for each fold
            Map<String, Double[]> foldMins = new HashMap<String, Double[]>();
            Double[] compare = new Double[numberOfFolds];

            for (int z = 1; z < models.size(); z++) { 
                int pos = 0; // maintains the position in the model accuracies array
                double[] modelAccuracies = acrossAllFoldsMap.get(models.get(z));

                for (int r = 0; r < numberOfRepeats; r++) {
                    for (int c = 0; c < numberOfFolds; c++) {

                        double now = modelAccuracies[pos];

                        if (compare[c] == null) {
                            compare[c] = now;
                        } else {
                            if (now > compare[c]) {
                                compare[c] = now;
                            }
                        }
                        pos++;
                    }
                }

                foldMins.put(seriesID, compare);

            }

            // now calculate bias for this  series
            evaluateSeriesBias(options, seriesID, models, acrossAllFoldsMap, foldMins);

        }
    }

    private ArrayList<String> extractSeriesModelIds(ProcessModelConditionsOptions options, String seriesID) {

        Set<String> allKeys = options.modelConditions.keySet();
        // model-Id's associated with model-conditions file

        String[] keys = (String[]) allKeys.toArray(new String[allKeys.size()]);
        ArrayList<String> models = new ArrayList(); // arraylist of models in the same series

        for (int i = 0; i < keys.length; i++) {
            String otherseriesId = options.modelConditions.get(keys[i]).get("id-parameter-scan-series");
            if (otherseriesId.equals(seriesID)) {
                models.add(keys[i]);
            }
        }
        System.out.println("models in same series " + models.toString());
        System.out.println("# models in " + seriesID + " series = " + models.size());
         return models;
    }

    private void evaluateSeriesBias(ProcessModelConditionsOptions options, String seriesID, ArrayList<String> models, Map<String, double[]> acrossAllFoldsMap, Map<String, Double[]> foldMins) {
        double bias = 0.0;
        for (String model : models) {
            int key = 0;
            for (int rpt = 0; rpt < numberOfRepeats; rpt++) {
                for (int f = 0; f < numberOfFolds; f++) {
                    double theta_all_k = (1 - (acrossAllFoldsMap.get(model)[key] / 100));
                    double theta_only_k = (1.0 - (foldMins.get(seriesID)[f] / 100));
                    bias += (theta_all_k - theta_only_k);

                    System.out.println("bias total " + bias);
                }
                bias /= numberOfFolds;

                davOptions.modelId = model;


            }
            key++;
            bias /= numberOfRepeats;
            System.out.println("bias for " + davOptions.modelId + " is " + bias);
            processOneModelIdPassTwo (davOptions.modelId, bias);
        }


    }


    @Override
    public void processOneModelIdPassOne
            (
                    final ProcessModelConditionsOptions options,
                    final String modelId) {

        predictions = loadPredictions(modelId);

        if (predictions != null) {
            numberOfFeatures = predictions.modelNumFeatures();

            System.out.println("Processing predictions(first pass) for model id  " + modelId);
            davOptions.crossValidationFoldNumber = predictions.getNumberOfFolds();

            davOptions.datasetName = getDatasetName(modelId);
            maqciiHelper.setLabel(constructLabel(modelId));
            davOptions.seriesModelId = options.modelConditions.get(modelId).get("id-parameter-scan-series");
            numberOfRepeats = predictions.getNumberOfRepeats();
            repeatedEvaluationMeasure = new EvaluationMeasure();

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

            evaluateAccuracyPerTestSet(modelId, predictions, numberOfRepeats, evaluationMeasureNames);

        }
    }

   
    public void processOneModelIdPassTwo(String modelId, Double bias) {
        predictions = loadPredictions(modelId);
        if (predictions != null) {
            System.out.println("Processing predictions(second pass) for model id  " + modelId);


            final int numberOfFeatures = predictions.modelNumFeatures();
            repeatedEvaluationMeasure = new EvaluationMeasure();

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

            davOptions.modelId = modelId;
            repeatedEvaluationMeasure.addValue("bias", bias);
            maqciiHelper.printSubmissionHeaders(davOptions, survivalFileName != null);
            maqciiHelper.printSubmissionResults(davOptions, repeatedEvaluationMeasure,
                    numberOfFeatures, numberOfRepeats, survivalMeasuresList);

        }

    }


    public static void evaluatePerformanceMeasurePerTestSet(
            final PredictedItems predictions,
            final String survivalFileName,
            final List<SurvivalMeasures> survivalMeasuresList,
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
                    averageMeasuresPerReplicates(repeatedEvaluationMeasure, allSplitsInARepeatMeasure);
                    LOG.trace(String.format("repeatId: %d %s", repeatId, allSplitsInARepeatMeasure.toString()));
                }
            }
        }
    }

    public void evaluateAccuracyPerTestSet(final String modelId,
                                           final PredictedItems predictions,
                                           final int numberOfRepeats,
                                           final ObjectSet<CharSequence> evaluationMeasureNames) {
        DoubleList modelAcc = new DoubleArrayList();


        // Collect one evaluation measure per split test set of cross-validation.
        for (int repeatId = 1; repeatId <= numberOfRepeats; repeatId++) {

            if (predictions.containsRepeat(repeatId)) {
                final int maxSplitId = predictions.getNumberOfSplitsForRepeat(repeatId);
                numberOfFolds = predictions.splitIdsForRepeat(repeatId).size();

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
                    modelAcc.add(allSplitsInARepeatMeasure.getAccuracy());

                }
            }
            acrossAllFoldsMap.put(modelId, modelAcc.toDoubleArray());
        }
    }


    public static void evaluatePerformanceMeasurePerRepeat(
            final PredictedItems predictions,
            final String survivalFileName,
            final List<SurvivalMeasures> survivalMeasuresList,
            final int numberOfRepeats,
            final ObjectSet<CharSequence> evaluationMeasureNames,
            final EvaluationMeasure repeatedEvaluationMeasure) {
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

                averageMeasuresPerReplicates(repeatedEvaluationMeasure, allSplitsInARepeatMeasure);
                LOG.trace(String.format("repeatId: %d %s", repeatId, allSplitsInARepeatMeasure.toString()));
            }
        }
    }

    private static void averageMeasuresPerReplicates
            (final EvaluationMeasure repeatedEvaluationMeasure,
             final EvaluationMeasure allSplitsInARepeatMeasure) {
        for (final CharSequence measure : evaluationMeasureNames) {
            repeatedEvaluationMeasure.addValue(measure,
                    allSplitsInARepeatMeasure.getPerformanceValueAverage(measure.toString()));

            final String binaryName = ("binary-" + measure).intern();
            repeatedEvaluationMeasure.addValue(binaryName,
                    allSplitsInARepeatMeasure.getPerformanceValueAverage(binaryName));
            final String zeroThresholdName = (measure + "-zero").intern();
            repeatedEvaluationMeasure.addValue(binaryName,
                    allSplitsInARepeatMeasure.getPerformanceValueAverage(zeroThresholdName));
        }
    }


    public enum StatsEvaluationType {
        STATS_PER_REPEAT,
        STATS_PER_SPLIT,
    }
}
