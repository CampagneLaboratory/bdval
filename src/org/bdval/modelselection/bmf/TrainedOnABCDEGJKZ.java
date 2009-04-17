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
 * Model trained with MAQC-II endpoints ABCDEGJKZ.
 *
 * @author Fabien Campagne
 * Date: Apr 15, 2009
 * Time: 1:50:32 PM
 */
public class TrainedOnABCDEGJKZ extends BMFCalibrationModel {

    public double calibrateEstimate(final ModelSelectionArguments toolsArgs, final String modelId,

                                    final Object2DoubleMap modelAttributes) {
        final int actualNumberOfFeaturesInModel = (int) modelAttributes.getDouble("actualNumberOfFeaturesInModel");

        final double norm_auc_cv = modelAttributes.getDouble("norm_auc");
        final double delta_auc_cvcf_cv = modelAttributes.getDouble("delta_auc_cvcf_cv");



        return 0.521295622557999 + -0.000461029909413533 * actualNumberOfFeaturesInModel +
                0.30633208149121 * delta_auc_cvcf_cv + 0.460738859272386 *
                norm_auc_cv + match(value(toolsArgs, modelId, "classifier-type"),
                "KStar", -0.0281258372274131,
                "LibSVM", 0.0353014891511363,
                "Logistic", -0.0615956594513561,
                "LogitBoost", 0.0667057072484948,
                "NaiveBayesUpdateable", -0.0608964002308641,
                "RandomForest", 0.0486107005100022

        ) + match(value(toolsArgs, modelId, "feature-selection-fold"),
                "false", -0.00625464874139557,
                "true", 0.00625464874139557

        ) + match(value(toolsArgs, modelId, "feature-selection-type"),
                "fold-change", 0.0518121246849801,
                "ga-wrapper", -0.0340570440558859,
                "pathways", -0.0256477119679852,
                "RFE", -0.0269193446496901,
                "SVM-weights", 0.0112289119698472,
                "t-test", 0.0235830640187339

        ) + match(value(toolsArgs, modelId, "svm-default-C-parameter"),
                "false", -0.0299635958352018,
                "true", 0.0299635958352018

        );

    }
}
