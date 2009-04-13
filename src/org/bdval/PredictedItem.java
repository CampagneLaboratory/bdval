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

/**
 * Information about the prediction results for an item/sample.
 *
 * @author Fabien Campagne
 *         Date: Apr 6, 2008
 *         Time: 10:29:46 AM
 */
public class PredictedItem {
    /**
     * Id of the split to which the samples/items belong.
     */
    int splitId;
    /**
     *
     * Type of split to which the samples/items belong.
     */
    String splitType;
    /**
     * Id of the split repeat to which the samples/items belong.
     */
    int repeatId;
    /**
     * Name of the model whose predictions are tested.
     */
    String modelFilenamePrefixNoPath;
    /**
     * Index of the sample for which the prediction was made.
     */
    int sampleIndex;
     /**
     * Identifier of the sample for which the prediction was made.
     */
    String sampleId;
    /**
     * Value of the decision value. Some classifiers output a probability, some do not. The larger the decision
     * value (in absolute value) the stronger the confidence in the prediction.
     *
     */
    double decision;
    /**
     * Predicted class label, in symbolic form (i.e., tumor instead of +1 in numeric form). Class symbols match
     * the information entered in the task file.
     */
    String symbolicClassLabel;
    /**
     * Probability that the prediction is correct, according to the model. Do not confuse with
     * the probability that the sample belongs to the positive class.
     */
    double probability;
    /**
     * Probability that the item belongs to class 1.
     */
    double probabilityOfClass1;
    /**
     * Value is the symbolic label of the true label for the sample (obtained from true-label
     * file provided to --mode predict).
     */
    String trueLabel;
    /**
     * Value is "correct" if the prediction agrees with the true label, "incorrect" otherwise.
     */
    String predictionCorrectIncorrect;
    /**
     * Value is zero if the true label belongs to the negative class, and 1 if the true label
     * belongs to the positive class.
     */
    public double numericTrueLabel;

    /**
     * The number of features in the model.
     */
    public int modelNumFeatures;

    public PredictedItem() {
        super();
    }

    public PredictedItem(final int splitId, final String splitType, final int repeatId, final String modelFilenamePrefixNoPath, final int sampleIndex,
                         final String sampleId, final double decision, final String symbolicClassLabel, final double probability,
                         final double probabilityOfClass1, final String trueLabel, final double numericTrueLabel,
                         final String predictionCorrectIncorrect, final int modelNumFeatures) {
        super();

        this.splitId = splitId;
        this.splitType = splitType;
        this.repeatId = repeatId;
        this.modelFilenamePrefixNoPath = modelFilenamePrefixNoPath;
        this.sampleIndex = sampleIndex;
        this.sampleId = sampleId;
        this.decision = decision;
        this.symbolicClassLabel = symbolicClassLabel;
        this.probability = probability;
        this.probabilityOfClass1 = probabilityOfClass1;
        this.trueLabel = trueLabel;
        this.numericTrueLabel = numericTrueLabel;
        this.predictionCorrectIncorrect = predictionCorrectIncorrect;
        this.modelNumFeatures = modelNumFeatures;
    }

    public static String getHeaders() {
        return "#splitId\tsplitType\trepeatId\tmodelFilenamePrefix\tsampleIndex\tsampleId\tprobabilityOfClass1\t" +
                "predictedSymbolicLabel\tprobabilityOfPredictedClass\tprobabilityClass1\ttrueLabel\t" +
                "numericTrueLabel\tcorrect\tmodelNumFeatures";
    }

    public String format() {
        return String.format("%d\t%s\t%d\t%s\t%d\t%s\t%f\t%s\t%f\t%f\t%s\t%f\t%s\t%d",
                splitId,
                splitType,
                repeatId,
                modelFilenamePrefixNoPath,
                sampleIndex,
                sampleId,
                decision,
                symbolicClassLabel,
                probability,
                probabilityOfClass1,
                trueLabel,
                numericTrueLabel,
                predictionCorrectIncorrect,
                modelNumFeatures);

    }


}

