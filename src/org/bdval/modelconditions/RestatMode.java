package org.bdval.modelconditions;

import com.martiansoftware.jsap.*;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import edu.cornell.med.icb.learning.tools.svmlight.EvaluationMeasure;
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
            StatsMode.StatsEvaluationType statsEvalType = StatsMode.StatsEvaluationType.STATS_PER_REPEAT;
            final List<SurvivalMeasures> survivalMeasuresList = new ArrayList<SurvivalMeasures>();
            switch (statsEvalType) {
                case STATS_PER_REPEAT:
                    StatsMode.evaluatePerformanceMeasurePerRepeat(predictions, null, survivalMeasuresList,
                            numberOfRepeats, evaluationMeasureNames, repeatedEvaluationMeasure);
                    break;
                case STATS_PER_SPLIT:
                    StatsMode.evaluatePerformanceMeasurePerTestSet(predictions, null, survivalMeasuresList,
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


}
