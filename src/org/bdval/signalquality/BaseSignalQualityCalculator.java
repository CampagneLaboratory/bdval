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

package org.bdval.signalquality;

import com.martiansoftware.jsap.JSAPResult;
import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.maps.LinkedHashToMultiTypeMap;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bdval.BDVModel;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for signal quality calculator classes.
 * @author Kevin Dorff
 */
public class BaseSignalQualityCalculator implements Closeable {

    /** The output writer. */
    private PrintWriter outputWriter;

    /** Translation from 0/1, LT/NLT, etc. to a specified value. */
    public static final String[] CLASS_TRANSLATION = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"
    };

    /** The output filename. */
    private String outputFilename;

    /** Object to assist with writing the TSV file. */
    private TsvToFromMap tsvOutput;

    private String classMapComment = "";

    private LinkedHashToMultiTypeMap<String> lastData;

    /**
     * OutputFileHeader enum to specify the column formats of the various output file types.
     */
    public enum OutputFileHeader {
        /** When writing the p-values file, these are the columns. */
        P_VALUES(
                new String[] {"model-id", "feature", "p-value[_X_]", "test-statistics[_X_]",
                        "t1[_X_]", "t2[_X_]"},
                new String[] {"mean[_X_]", "range[_X_]",
                        "training-values[_X_]", "validation-values[_X_]"}),
        /** When writing the qualities file, these are the columns. */
        SIGNAL_QUALITIES(
                new String[] {"model-id", "stouffer z[_X_]", "stouffer pz[_X_]", "ratio_rank[_X_]"},
                new String[] {"fisher f[_X_]", "fisher pf[_X_]"});

        /** The header columns. */
        private final String[] headerColumns;

        /** The extended columns. */
        private final String[] extendedColumns;

        /**
         * Enum constructor.
         * @param headerColumnsVal the header columns
         * @param extendedColumnsVal the extended mode header columns
         */
        OutputFileHeader(final String[] headerColumnsVal, final String[] extendedColumnsVal) {
            this.headerColumns = headerColumnsVal;
            this.extendedColumns = extendedColumnsVal;
        }

        /**
         * Retrieve the list of columns.
         * @param extendedMode if we are outputing in extended mode or not
         * @param maxNumClasses the max number of classes to support (class names are
         * converted to values within CLASS_TRANSLATION).
         * @return the column list
         */
        public List<String> getColumns(final boolean extendedMode, final int maxNumClasses) {
            final List<String> result = new LinkedList<String>();
            for (int classNum = 0; classNum < maxNumClasses; classNum++) {
                for (final String headerColumnName : headerColumns) {
                    if (!headerColumnName.contains("[_X_]")) {
                        if (classNum == 0) {
                            result.add(headerColumnName);
                        }
                    } else {
                        result.add(headerColumnName.replaceAll(
                                "_X_", CLASS_TRANSLATION[classNum]));
                    }
                }
            }
            if (extendedMode) {
                for (int classNum = 0; classNum < maxNumClasses; classNum++) {
                    for (final String headerColumnName : extendedColumns) {
                        if (!headerColumnName.contains("[_X_]")) {
                            if (classNum == 0) {
                                result.add(headerColumnName);
                            }
                        } else {
                            result.add(headerColumnName.replaceAll(
                                    "_X_", CLASS_TRANSLATION[classNum]));
                        }
                    }
                }
            }
            return result;
        }
    }

    public void configure(
            final JSAPResult result, final OutputFileHeader headerEnum,
            final boolean extendedOutput, final int maxNumClasses) throws FileNotFoundException {
        final Properties config = new Properties();
        config.addProperty("output", result.getString("output"));
        configure(config, headerEnum, extendedOutput, maxNumClasses);
    }

    /**
     * Configure the quality calculator.
     * @param config the configuration and/or
     * @param headerEnum which header to write
     * @param extendedOutput if extended output should be written
     * @param maxNumClasses the maximum number of classes
     * @throws FileNotFoundException error creating output file
     */
    public void configure(
            final Properties config, final OutputFileHeader headerEnum,
            final boolean extendedOutput, final int maxNumClasses) throws FileNotFoundException {

        // Close the previous file, if one is open
        close();

        outputFilename = config.getString("output");
        tsvOutput = new TsvToFromMap();
        for (final String column : headerEnum.getColumns(extendedOutput, maxNumClasses)) {
            tsvOutput.addColumn(column);
        }

        if (StringUtils.isBlank(outputFilename) || outputFilename.equals("screen")) {
            System.out.println("Writing output to terminal, not to a file");
            outputWriter = new PrintWriter(System.out);
        } else if (outputFilename.equals("null")) {
            outputWriter = null;
        } else {
            System.out.println("Writing output to " + outputFilename);
            final File outputFile = new File(outputFilename);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            outputWriter = new PrintWriter(outputFile);
        }
        if (outputWriter != null) {
            tsvOutput.writeHeader(outputWriter);
        }
    }

    /**
     * Close the output.
     */
    public void close() {
        if (StringUtils.isNotBlank(outputFilename)
                && (!outputFilename.equals("screen"))
                && (!outputFilename.equals("null"))) {
            IOUtils.closeQuietly(outputWriter);
            outputWriter = null;
        }
    }

    /**
     * Write a line of data.
     * @param data the data to write
     */
    public void writeData(final LinkedHashToMultiTypeMap<String> data) {
        if (outputWriter != null) {
            tsvOutput.writeDataFromMap(outputWriter, data);
        }
        lastData = data;
    }

    public LinkedHashToMultiTypeMap<String> getLastData() {
        return lastData;
    }

    /**
     * Write a pre-formatted line of data.
     * @param data the data to write
     */
    public void writeData(final String data) {
        if (outputWriter != null) {
            outputWriter.println(data);
        }
    }

    /**
     * Base calculate pValues - verify the input data.
     *
     * @param model the model we are writing
     * @param modelId the model id we are calculating the signal quality for
     * @param allClasses the sample class
     * @param classToDataMapMap map of classes + "-training"/"-validation" to the
     * map of feature to raw data.
     */
    public void calculatePValues(
            final BDVModel model, final String modelId,
            final String[] allClasses,
            final Map<String, Map<String, double[]>> classToDataMapMap) {
        assert allClasses != null;
        assert allClasses.length > 0;
        assert classToDataMapMap.keySet().size() == (allClasses.length * 2)
                : String.format("Map size wrong %d should be %d ",
                classToDataMapMap.keySet().size(), allClasses.length * 2);
        for (final String classId : allClasses) {
            final Map<String, double[]> trainingDataMap =
                    classToDataMapMap.get(classId + "-training");
            final Map<String, double[]> validationDataMap =
                    classToDataMapMap.get(classId + "-validation");
            // Features the same in both training and validation
            assert trainingDataMap.keySet().equals(validationDataMap.keySet());
        }
    }

    /**
     * Get the comment regarding the class mapping.
     * @param newClassMapComment the comment regarding the class mapping.
     */
    public void setClassMapComment(final String newClassMapComment) {
        if (this.classMapComment == null || !classMapComment.equals(newClassMapComment)) {
            // Only write the class map comment if it is different from the last one
            this.classMapComment = newClassMapComment;
            writeData(this.classMapComment);
        }
    }

    /**
     * Base calculate signal quality - verify the input data.
     *
     * @param modelId the model id we are calculating the signal quality for
     * @param allClasses all the classes that may be represented in
     * @param featuresList the list of features classToDataMap
     * @param classToDataMap map of class + ("p-value"/"t1"/"t2") to that data
     * If a class isn't defined,
     */
    public void calculateSignalQuality(
            final String modelId,
            final String[] allClasses,
            final List<String> featuresList,
            final Map<String, DoubleList> classToDataMap) {
        assert StringUtils.isNotBlank(modelId);
        assert featuresList != null && featuresList.size() > 0;
        final int size = featuresList.size();
        for (final String classId : allClasses) {
            final DoubleList pValueData = classToDataMap.get(classId + "-p-value");
            final DoubleList t1Data = classToDataMap.get(classId + "-t1");
            final DoubleList t2Data = classToDataMap.get(classId + "-t2");
            final String sizes = String.format(
                    "Sizes are featureList=%d, pValueData=%d, rankTrData=%d, rankVaData=%d",
                    featuresList.size(), pValueData.size(), t1Data.size(), t2Data.size());
            assert ((pValueData.size() + t1Data.size() + t2Data.size() == 0)
                    || ((pValueData.size() == size && t1Data.size() == size
                    && t2Data.size() == size))) : sizes;
        }
    }
}
