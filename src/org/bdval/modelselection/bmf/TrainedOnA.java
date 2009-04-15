/*
 *
 * Copyright (C) 2003-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 * All rights reserved.
 */

package org.bdval.modelselection.bmf;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.bdval.modelselection.BMFCalibrationModel;
import org.bdval.modelselection.ModelSelectionArguments;


/**
 * BMF Calibration model trained on MAQC-II endpoint A.
 * @author: Fabien Campagne
 * Date: Apr 15, 2009
 * Time: 2:08:38 PM
 */
public class TrainedOnA extends BMFCalibrationModel {
    public double calibrateEstimate(ModelSelectionArguments toolsArgs, String modelId, Object2DoubleMap modelAttributes) {
        final int actualNumberOfFeaturesInModel = (int) modelAttributes.getDouble("actualNumberOfFeaturesInModel");

        final double norm_auc_cv = modelAttributes.getDouble("norm_auc");
        final double delta_auc_cvcf_cv = modelAttributes.getDouble("delta_auc_cvcf_cv");

        final double predictedPerformance =
                0.767927506304831 + 0.181936624689305 *

                        norm_auc_cv +
                        match(value(toolsArgs, modelId, "num-features"),
                                "10", 0,
                                "100", -0.0473561020046038,
                                "20", -0.0123113279593483,
                                "25", (-0.0123113279593483 + -0.0163080380584613) / 2, // interpolation 20-30
                                "30", -0.0163080380584613,
                                "40", -0.0214867084942386,
                                "5", -0.0357679053833812,
                                "50", -0.0442070722823977,
                                "60", -0.0404108150384205,
                                "70", -0.0413638663144752,
                                "80", -0.0483091727091295,
                                "90", -0.0424415575819764
                        ) + match(value(toolsArgs, modelId, "sequence-file"),
                        "baseline.sequence", -0.0302242788384328,
                        "foldchange-genetic-algorithm.sequence", 0.0374926767655907,
                        "foldchange-svmglobal.sequence", 0.0274883784897423,
                        "foldchange-svmiterative.sequence", 0.0344998499588745,
                        "genetic-algorithm.sequence", -0.0540407956129042,
                        "minmax-svmglobal.sequence", 0.00742958932280811,
                        "svmiterative.sequence", -0.0379254012191334,
                        "ttest-genetic-algorithm.sequence", 0.0773731693501089,
                        "ttest-svmglobal.sequence", 0.0522108551145124,
                        "ttest-svmiterative.sequence", 0.0598489166105302,
                        "ttest-weka-classifier-fs=false.sequence", -0.0721306589138169,
                        "ttest-weka-classifier-fs=true.sequence", -0.0567160544813172,
                        "tuneC-baseline.sequence", -0.0453062465465625) +
                        match(value(toolsArgs, modelId, "feature-selection-fold"),
                                "false", 0,
                                "true", 0)
                        + -0.0414908150437834 * delta_auc_cvcf_cv;
        return predictedPerformance;
    }
}
