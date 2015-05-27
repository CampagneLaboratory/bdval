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

import edu.cornell.med.icb.io.TSVReader;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.io.FileReader;
import java.io.IOException;

/**
 * @author Fabien Campagne
 *         Date: Apr 6, 2008
 *         Time: 10:52:06 AM
 */
public class PredictedItems extends ObjectArrayList<PredictedItem> {
    public static ObjectList<PredictedItem> predictions;
    private int numberOfRepeats;
    private int numberOfFolds;

    public boolean isRegressionModel() {
        return isRegressionModel;
    }

    private boolean isRegressionModel;

    public void load(final String filename) throws IOException {
        final TSVReader reader = new TSVReader(new FileReader(filename));
        predictions = new ObjectArrayList<PredictedItem>();
        isRegressionModel=true;
        while (reader.hasNext()) {
            if (reader.isCommentLine() || reader.isEmptyLine()) {
                reader.skip();
            } else {
                final PredictedItem item = new PredictedItem();
                reader.next();
                item.splitId = reader.getInt();
                numberOfFolds=Math.max(item.splitId, numberOfFolds);
                item.splitType = reader.getString();
                item.repeatId = reader.getInt();
                numberOfRepeats = Math.max(item.repeatId, numberOfRepeats);
                item.modelFilenamePrefixNoPath = reader.getString();
                item.sampleIndex = reader.getInt();
                item.sampleId = reader.getString();
                item.decision = reader.getDouble();
                item.symbolicClassLabel = reader.getString();
                item.probability = reader.getDouble();
                item.probabilityOfClass1 = reader.getDouble();
                item.trueLabel = reader.getString();
                item.numericTrueLabel = reader.getDouble();
                item.predictionCorrectIncorrect = reader.getString();
                boolean canParse=false;
                try{
                    double v=Double.parseDouble(item.predictionCorrectIncorrect);
                    item.delta=v;
                    canParse=true;
                }catch (NumberFormatException e) {
                    canParse=false;
                    isRegressionModel=false;
                }
                isRegressionModel&=canParse;
                item.modelNumFeatures = reader.getInt();
                predictions.add(item);

            }
        }

    }

    public int getNumberOfRepeats() {
        return numberOfRepeats;
    }

    /**
     * The returned value indicates the confidence that the predicted item belongs to class 1,
     * according to the classifier. Larger positive values indicate more confidence in class 1 prediction, whereas
     * larger negative values indicate more confidence in class -1 prediction.
     *
     * @param repeatId
     * @return
     */
    public DoubleList getDecisionsForRepeat(final int repeatId) {
        final DoubleList result = new DoubleArrayList();
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId) {
                final double decisionClass1 = item.probability * item.decision;
                // decisionClass1 becomes negative for class -1
                // and positive for class 1, such that decisionClass1(i)>decisionClass1(j) when the classifier expects
                // i to have greater chance to belong to class 1 than to class -1.
                result.add(decisionClass1);
            }
        }
        return result;
    }

    /**
     * Returns decision values for predictions with a given repeatId and splitId.
     *
     * @param repeatId The repeatId for predictions of interest
     * @param splitId  The splitId for predictions of interest
     * @return List of decision values
     */
    public DoubleList getDecisionsForSplit(final int repeatId, final int splitId) {
        final DoubleList result = new DoubleArrayList();
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId && item.splitId == splitId) {
                final double decisionClass1 = item.probability * item.decision;
                // decisionClass1 becomes negative for class -1
                // and positive for class 1, such that decisionClass1(i)>decisionClass1(j) when the classifier expects
                // i to have greater chance to belong to class 1 than to class -1.
                result.add(decisionClass1);
            }
        }
        return result;
    }

    public int getNumberOfSplitsForRepeat(final int repeatId) {
        int maxSplitId = -1;
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId) {

                maxSplitId = Math.max(maxSplitId, item.splitId);
            }
        }
        return maxSplitId;

    }

    /**
     * Returns true labels for predictions with a given repeatId.
     *
     * @param repeatId The repeatId for predictions of interest
     * @return List of decision values
     */
    public DoubleList getTrueLabelsForRepeat(final int repeatId) {
        final DoubleList result = new DoubleArrayList();
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId) {
                result.add(item.numericTrueLabel);
            }
        }
        return result;
    }

    /**
     * Returns true labels for predictions with a given repeatId and splitId.
     *
     * @param repeatId The repeatId for predictions of interest
     * @param splitId  The splitId for predictions of interest
     * @return List of decision values
     */
    public DoubleList getTrueLabelsForSplit(final int repeatId, final int splitId) {
        final DoubleList result = new DoubleArrayList();
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId && item.splitId == splitId) {
                result.add(item.numericTrueLabel);
            }
        }
        return result;
    }

    /**
     * Return a list of sample Ids for predictions in the repeat.
     *
     * @param repeatId The repeatId for predictions of interest
     * @return
     */
    public ObjectList<String> getSampleIDsForRepeat(final int repeatId) {
        final ObjectList<String> result = new ObjectArrayList<String>();
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId) {
                result.add(item.sampleId);
            }
        }
        return result;
        /**
         * Return a list of sample Ids for predictions in the repeat.
         * @param repeatId The repeatId for predictions of interest
         * @param splitId  The splitId for predictions of interest
         * @return
         */}

    public ObjectList<String> getSampleIDsForSplit(final int repeatId, final int splitId) {
        final ObjectList<String> result = new ObjectArrayList<String>();
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId && item.splitId == splitId) {
                result.add(item.sampleId);
            }
        }
        return result;
    }

    public boolean containsRepeat(final int repeatId) {
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId) {
                return true;
            }

        }
        return false;
    }

    public int modelNumFeatures() {
        if (predictions == null || predictions.isEmpty()) {
            return -1;
        } else {
            return predictions.get(0).modelNumFeatures;
        }
    }

    /**
     * Return the valid splidIds for the given repeatId.
     * @param repeatId  The repeatId of interest.
     * @return set of splidIds.
     */
    public IntSet splitIdsForRepeat(final int repeatId) {
        final IntSet result = new IntArraySet();
        for (final PredictedItem item : predictions) {
            if (item.repeatId == repeatId) {
                result.add(item.splitId);
            }
        }
        return result;
    }

    public int getNumberOfFolds() {
        return numberOfFolds;
    }
}
