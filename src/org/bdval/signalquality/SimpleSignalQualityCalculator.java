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

package edu.cornell.med.icb.biomarkers.signalquality;

import edu.cornell.med.icb.R.script.RDataObjectType;
import edu.cornell.med.icb.R.script.RScript;
import edu.cornell.med.icb.biomarkers.BDVModel;
import edu.cornell.med.icb.maps.LinkedHashToMultiTypeMap;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Signal Quality Calculator.
 *
 * @author Kevin Dorff
 */
public class SimpleSignalQualityCalculator extends BaseSignalQualityCalculator {

    /**
     * The logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(SimpleSignalQualityCalculator.class);

    /** The script used when writing the first, pvalues file. */
    private RScript dataToPvalueScript;


    /** Line of data to output. */
    final LinkedHashToMultiTypeMap<String> data = new LinkedHashToMultiTypeMap<String>();

    /** List of all features. */
    final Set<String> allFeaturesSet = new ObjectArraySet<String>();
    private ComputeRatioRank computeRatioRank;

    /**
     * Compute the signal quality between the two sets of data.
     * Input maps have a key of the probe id, such as "AA799301_Probe1"
     * Map value per key is a double[] of values.
     * <p/>
     * One idea Fabien had is to write a TSV file that contains
     * ModelID  Feature  p-Value
     * for all models and then do various post processing on that one
     * file using a separate tool instead of doing the entire calculation here?
     *
     * @param model             the model we are writing
     * @param modelId           the model id we are calculating the signal quality for
     * @param classToDataMapMap map of classes + "-training"/"-validation" to the
     * map of feature to raw data.
     */
    @Override
    public void calculatePValues(
            final BDVModel model, final String modelId,
            final String[] allClasses,
            final Map<String, Map<String, double[]>> classToDataMapMap) {

        // Call calculate in AbstractSignalQualityCalculator first
        super.calculatePValues(model, modelId, allClasses, classToDataMapMap);

        // System.out.println("(simple) Calculating signal quality for model " + modelId);

        // Calculate pValues and write them

        // Aquire the ENITRE feature set from both training and validation
        allFeaturesSet.clear();
        for (final String sampleClass : allClasses) {
            final Map<String, double[]> trainingDataMap = classToDataMapMap.get(
                    sampleClass + "-training");
            final Map<String, double[]> validationDataMap = classToDataMapMap.get(
                    sampleClass + "-validation");
            allFeaturesSet.addAll(trainingDataMap.keySet());
            allFeaturesSet.addAll(validationDataMap.keySet());
        }

        for (final String featureId : allFeaturesSet) {

            double[] trainingData = null;
            double[] validationData = null;
            try {
                data.clear();
                data.put("model-id", modelId);
                data.put("feature", featureId);
                for (int classIndex = 0; classIndex < allClasses.length; classIndex++) {
                    final String classId = allClasses[classIndex];
                    final String classIdAppend = "["
                            + BaseSignalQualityCalculator.CLASS_TRANSLATION[classIndex] + "]";
                    trainingData = classToDataMapMap.get(classId + "-training").get(featureId);
                    if (trainingData == null) {
                        trainingData = ArrayUtils.EMPTY_DOUBLE_ARRAY;
                    }
                    validationData = classToDataMapMap.get(classId + "-validation").get(featureId);
                    if (validationData == null) {
                        validationData = ArrayUtils.EMPTY_DOUBLE_ARRAY;
                    }
                    if (trainingData.length == 0 || validationData.length == 0) {
                        if (trainingData.length == 0) {
                            writeData(String.format(
                                    "# modelId=%s, featureId=%s, class=%s has no training data.",
                                    modelId, featureId, classId));
                        }
                        if (validationData.length == 0) {
                            writeData(String.format(
                                    "# modelId=%s, featureId=%s, class=%s has no validation data.",
                                    modelId, featureId, classId));
                        }
                        continue;
                    }
                    // System.out.printf("Training data size = %s Validation data size = %d%n",
                    //         trainingData.length, validationData.length);

                    if (dataToPvalueScript == null) {
                        dataToPvalueScript =
                                RScript.createFromResource("rscripts/data_to_pvalue.R");
                    }

                    dataToPvalueScript.setInput("x", trainingData);
                    dataToPvalueScript.setInput("y", validationData);

                    dataToPvalueScript.setOutput("p_value", RDataObjectType.Double);
                    dataToPvalueScript.setOutput("test_statistic", RDataObjectType.Double);
                    dataToPvalueScript.setOutput("sum_rank_features", RDataObjectType.DoubleArray);
                    dataToPvalueScript.execute();

                    final double pvalue = dataToPvalueScript.getOutputDouble("p_value");
                    final double testStatistic =
                            dataToPvalueScript.getOutputDouble("test_statistic");
                    final double[] sumRankFeatures =
                            dataToPvalueScript.getOutputDoubleArray("sum_rank_features");

                    data.put("p-value" + classIdAppend, pvalue);
                    data.put("test-statistics" + classIdAppend, testStatistic);
                    data.put("t1" + classIdAppend, sumRankFeatures[0]);
                    data.put("t2" + classIdAppend, sumRankFeatures[1]);
                    if (model != null) {
                        data.put("mean" + classIdAppend,
                                model.getTrainingSetMeanValue(featureId));
                        data.put("range" + classIdAppend,
                                model.getTrainingSetRangeValue(featureId));
                    } else {
                        data.put("mean" + classIdAppend, -1d);
                        data.put("range" + classIdAppend, -1d);
                    }
                    data.put("training-values" + classIdAppend, trainingData);
                    data.put("validation-values" + classIdAppend, validationData);
                }
                writeData(data);
            } catch (Exception e) {
                LOG.error("Could not calculate KS-test. "
                        + "Error data written to comment in output file");
                writeData(String.format("# ERROR WITH %s.%s.trainingData=%s",
                        modelId, featureId, ArrayUtils.toString(trainingData)));
                writeData(String.format("# ERROR WITH %s.%s.validationData=%s",
                        modelId, featureId, ArrayUtils.toString(validationData)));
            }
        }
    }

    /**
     * Calculate signal quality.
     *
     * @param modelId the model id we are calculating the signal quality for
     * @param allClasses all the classes that may be represented in
     * @param featuresList the list of features classToDataMap
     * @param classToDataMap map of class + ("p-value"/"t1"/"t2") to that data
     * If a class isn't defined,
     */
    @Override
    public void calculateSignalQuality(
            final String modelId,
            final String[] allClasses,
            final List<String> featuresList,
            final Map<String, DoubleList> classToDataMap) {

        // Call calculate in AbstractSignalQualityCalculator first
        super.calculateSignalQuality(modelId, allClasses, featuresList, classToDataMap);

        try {
            data.clear();
            data.put("model-id", modelId);
            for (final String classId : allClasses) {
                final double[] pValues =
                        classToDataMap.get(classId + "-p-value").toDoubleArray();
                final double[] t1Values =
                        classToDataMap.get(classId + "-t1").toDoubleArray();
                final double[] t2Values =
                        classToDataMap.get(classId + "-t2").toDoubleArray();
                if ((pValues.length + t1Values.length + t2Values.length) == 0) {
                    // The class is not used for this model
                    continue;
                }
                final String classIdAppend = "[" + classId + "]";

                computeRatioRank = new ComputeRatioRank(pValues, t1Values, t2Values).invoke();

                final double[] stoufferVals = computeRatioRank.getStoufferVals();
                final double ratioRankVal = computeRatioRank.getRatioRankVal();
                final double[] fisherVals = computeRatioRank.getFisherVals();

                data.put("stouffer z" + classIdAppend, stoufferVals[0]);
                data.put("stouffer pz" + classIdAppend, stoufferVals[1]);
                data.put("ratio_rank" + classIdAppend, ratioRankVal);
                data.put("fisher f" + classIdAppend, fisherVals[0]);
                data.put("fisher pf" + classIdAppend, fisherVals[1]);
            }
            writeData(data);
        } catch (Exception e) {
            writeData("# ERROR computing data " + e.getMessage());
            LOG.error(e);
            e.printStackTrace();
        }
    }
}
