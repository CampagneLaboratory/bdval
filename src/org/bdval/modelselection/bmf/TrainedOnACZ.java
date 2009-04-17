/*
 * Copyright (C) 2009 Institute for Computational Biomedicine,
 *                    Weill Medical College of Cornell University
 *                    All rights reserved.
 *
 * WEILL MEDICAL COLLEGE OF CORNELL UNIVERSITY MAKES NO REPRESENTATIONS
 * ABOUT THE SUITABILITY OF THIS SOFTWARE FOR ANY PURPOSE. IT IS PROVIDED
 * "AS IS" WITHOUT EXPRESS OR IMPLIED WARRANTY. THE WEILL MEDICAL COLLEGE
 * OF CORNELL UNIVERSITY SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY
 * THE USERS OF THIS SOFTWARE.
 */

package org.bdval.modelselection.bmf;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.bdval.modelselection.BMFCalibrationModel;
import org.bdval.modelselection.ModelSelectionArguments;

/**
 * BMF Calibration model trained on MAQC-II endpoint A, C and Z.
 *
 * @author Fabien Campagne
 *         Date: Apr 15, 2009
 *         Time: 2:12:44 PM
 */
public class TrainedOnACZ extends BMFCalibrationModel {
    public double calibrateEstimate(final ModelSelectionArguments toolsArgs, final String modelId, final Object2DoubleMap modelAttributes) {
        final int actualNumberOfFeaturesInModel = (int) modelAttributes.getDouble("actualNumberOfFeaturesInModel");

        final double norm_auc_cv = modelAttributes.getDouble("norm_auc");
        final double delta_auc_cvcf_cv = modelAttributes.getDouble("delta_auc_cvcf_cv");

        return 0.459989454833173 +
                -0.00083173460079395 * actualNumberOfFeaturesInModel +
                0.536622935579494 * norm_auc_cv +
                match(value(toolsArgs, modelId, "classifier-type"),
                        "KStar", 0.0135523789492576,
                        "LibSVM", 0.0449462916662318,
                        "Logistic", -0.0681672665697239,
                        "LogitBoost", 0.074435720784513,
                        "NaiveBayesUpdateable", -0.118305143431788,
                        "RandomForest", 0.0535380186015092
                )
                + match(value(toolsArgs, modelId, "feature-selection-fold"),
                "false", 0.00810507959138625,
                "true", -0.00810507959138625)
                + match(value(toolsArgs, modelId, "feature-selection-type"),
                "fold-change", 0.0410682769672393,
                "ga-wrapper", 0.0197988254395799,
                "pathways", -0.0326917219922266,
                "RFE", -0.0420895743946066,
                "SVM-weights", 0.00335502231253732,
                "t-test", 0.0105591716674767)
                + match(value(toolsArgs, modelId, "svm-default-C-parameter"),
                "false", -0.0255816480713828,
                "true", 0.0255816480713828
        )
                + 0.614443519922211 * delta_auc_cvcf_cv;
    }
}
