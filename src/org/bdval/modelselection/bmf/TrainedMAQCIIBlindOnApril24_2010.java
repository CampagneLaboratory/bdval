/*
 * Copyright (C) 2009-2010 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *                         All rights reserved.
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
 * Model trained with all the MAQC-II endpoints, Blind direction. Re-run of April 24 2010.
 * R square is 0.84 over 1237 observations.
 *
 * @author Fabien Campagne
 *         Date: Apr 24 2010, 2010
 */
public class TrainedMAQCIIBlindOnApril24_2010 extends BMFCalibrationModel {

    @Override
    public double calibrateEstimate(final ModelSelectionArguments toolsArgs, final String modelId,

                                    final Object2DoubleMap modelAttributes) {
        final int actualNumberOfFeaturesInModel = (int) modelAttributes.getDouble("actualNumberOfFeaturesInModel");

        final double norm_auc_cv = modelAttributes.getDouble("norm_auc");
        final double delta_auc_cvcf_cv = modelAttributes.getDouble("delta_auc_cvcf_cv");

        return 0.210075091138973 + 0.514412817553082 * norm_auc_cv
                -0.000377923544518397 * actualNumberOfFeaturesInModel

                + match(value(toolsArgs, modelId, "feature-selection-fold"),
                "false", -0.0114757109328514,
                "true", 0.0114757109328514

        ) + 0.2170548590021 * delta_auc_cvcf_cv +
                match(value(toolsArgs, modelId, "classifier-type"),
                        "KStar", -0.00369535805064768,
                        "LibSVM", 0.00193546217956395,
                        "Logistic", -0.0051900918569372,
                        "LogitBoost", 0.020448918064335,
                        "NaiveBayesUpdateable", -0.0211943029966407,
                        "RandomForest", 0.00769537266032658

                )
                + match(value(toolsArgs, modelId, "feature-selection-type"),
                "fold-change", 0.0156731511517315,
                "ga-wrapper", 0.00776942493927286,
                "min-max", -0.0247320671759594,
                "pathways", -0.00990684479171082,
                "RFE", -0.00668161145292082,
                "SVM-weights", 0,
                "t-test", 0.0178779473295866
        );


    }
}