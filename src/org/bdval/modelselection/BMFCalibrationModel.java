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

package org.bdval.modelselection;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.io.FilenameUtils;

/**
 * @author Fabien Campagne
 * Date: Apr 15, 2009
 * Time: 1:47:46 PM
 */
public abstract class BMFCalibrationModel {
    public static BMFCalibrationModel load(final String modelName) {
        try {
            final Class clazz = Class.forName("org.bdval.modelselection.bmf." + modelName);
            final BMFCalibrationModel bmf = (BMFCalibrationModel) clazz.newInstance();
            return bmf;
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot load BMF calibration implementation. Class not found.");
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            System.err.println("Cannot instanciate BMF calibration implementation.");
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Cannot instanciate BMF calibration implementation.");
            e.printStackTrace();
        }
        return null;
    }

    public abstract double calibrateEstimate(final ModelSelectionArguments toolsArgs,
                                             String modelId,
                                             Object2DoubleMap modelAttributes);

    public double match(final String variableValue, final Object... values) {

        for (int i = 0; i < values.length; i += 2) {
            final Object key = values[i];
            final String keyAsString = key.toString();
            if (keyAsString.equals(variableValue)) {
                final Object d = values[i + 1];
                if (d instanceof Double) {
                    final Double value = (Double) d;
                    return value;
                }
                if (d instanceof Integer) {
                    final Integer value = (Integer) d;
                    return value;
                }
            }
        }
        System.out.println("Returning default value for " + variableValue);
        return Double.NaN;
    }

    public String value(final ModelSelectionArguments toolsArgs, final String modelId, final String variableName) {

        final String value = toolsArgs.modelConditions.get(modelId).get(variableName);
        return normalizeConditionValue(variableName, value);
    }

    public String normalizeConditionValue(final String columnName, final String value) {
        if (value == null || columnName == null) {
            return null;
        }
        if ("sequence-file".equals(columnName)) {
            // remove the path to the sequence file. The path is not useful for downstream analyses
            final String toReplace = value.replaceAll("fs=true", "fs").replaceAll("fs=false", "fs");
            return FilenameUtils.getBaseName(toReplace) + ".sequence";
        } else {
            return value;
        }
    }
}
