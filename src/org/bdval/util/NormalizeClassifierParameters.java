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

package org.bdval.util;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import edu.cornell.med.icb.maps.LinkedHashToMultiTypeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Split classifier parameters into distinct columns. This program takes as input a tab delimited file with
 * modelId, classifier-type and classifier-parameter columns and splits the classifier-type column into
 * separate columns, whose id is prefixed with the classifier-type column value. 
 *
 * @author Fabien Campagne
 */
public class NormalizeClassifierParameters {

    private String inputFilename;
    private String outputFilename;

    /**
     * Main method.
     *
     * @param args the command line arguments
     * @throws com.martiansoftware.jsap.JSAPException
     *                             problem parsing the command line
     * @throws java.io.IOException problem read/writing the files
     */
    public static void main(final String[] args) throws JSAPException, IOException {
        final NormalizeClassifierParameters work = new NormalizeClassifierParameters();
        work.process(args);
    }


    /**
     * Process the command line, read the input file, write the output file.
     *
     * @param args the command line arguments
     * @throws com.martiansoftware.jsap.JSAPException
     *                             problem parsing the command line
     * @throws java.io.IOException problem read/writing the files
     */
    @SuppressWarnings("unchecked")
    private void process(final String[] args) throws JSAPException, IOException {
        if (!processCommandLine(args)) {
            return;
        }

        final File inputFile = new File(inputFilename);
        if (!inputFile.exists()) {
            return;
        }

        TsvToFromMap tsvToFromMap = null;
        tsvToFromMap = TsvToFromMap.createFromTsvFile(new File(inputFilename));

        final File outputFile = new File(outputFilename);
        if (outputFile.exists()) {
            // This file cannot be appended to
            outputFile.delete();
        }
        final PrintWriter outputWriter = new PrintWriter(
                new FileWriter(outputFile));


        Object2ObjectMap<String, Object2ObjectMap<String, String>> map = new Object2ObjectOpenHashMap<String, Object2ObjectMap<String, String>>();

        int lineNumber = 0;
        ObjectArrayList<String> modelIds = new ObjectArrayList<String>();

        for (final String line : new TextFileLineIterator(inputFilename)) {
            if (lineNumber > 0) {
                LinkedHashToMultiTypeMap<String> data = tsvToFromMap.readDataToMap(line);
                System.out.println("data: " + data);
                final String modelId = data.get("modelId");
                modelIds.add(modelId);
                for (String column : data.keySet()) {
                    Object2ObjectMap<String, String> columnMap = setupColumnMap(map, column);

                    columnMap.put(modelId, data.get(column));

                }

            }
            lineNumber++;


        }
        List<String> columns;

        columns = tsvToFromMap.getColumnHeaders();
        for (String modelId : modelIds) {
            String params = map.get("classifier-parameters").get(modelId);
            String classifier = map.get("classifier-type").get(modelId);
            String[] tokens = params.split("[,]");
            for (String token : tokens) {
                String key_value[] = token.split("[=]");

                String key = key_value[0];
                if (key.equals("wekaClass")) continue;
                String value = key_value.length > 1 ? key_value[1] : "DEFINED";
                Object2ObjectMap<String, String> columnMap = setupColumnMap(map, classifier + key);
                columnMap.put(modelId, value);

            }

            System.out.println("params:" + params);
        }

        map.remove("classifier-parameters");
        columns = new ArrayList<String>(map.keySet());
        Collections.sort(columns);

        writeData(outputWriter, columns, modelIds, map);
        outputWriter.flush();
        IOUtils.closeQuietly(outputWriter);
    }

    private Object2ObjectMap<String, String> setupColumnMap(Object2ObjectMap<String, Object2ObjectMap<String, String>> map, String column) {
        Object2ObjectMap<String, String> columnMap = map.get(column);
        if (columnMap == null) {
            columnMap = new Object2ObjectOpenHashMap<String, String>();
            columnMap.defaultReturnValue("N/A");
            map.put(column, columnMap);
        }
        return columnMap;
    }


    /**
     * Write a line of data to the output file.
     *
     * @param outputWriter the writer to write to
     * @param columns      the columns that exist in the output file
     * @param valueMap     the line of data to write to the output file
     */
    private void writeData(final PrintWriter outputWriter,
                           final List<String> columns,
                           ObjectArrayList<String> modelIds,
                           final Object2ObjectMap<String, Object2ObjectMap<String, String>> valueMap) {

        int colNum = 0;
        for (String column : columns) {
            if (colNum++ > 0) {
                outputWriter.print("\t");
            }
            outputWriter.print(column);
        }
        outputWriter.println();

        for (String modelId : modelIds) {
            colNum = 0;
            for (final String column : columns) {
                if (colNum++ > 0) {
                    outputWriter.print("\t");
                }

                Object2ObjectMap<String, String> columnData = valueMap.get(column);

                if (columnData == null) {
                    outputWriter.print("");
                } else {
                    final String dataItem = columnData.get(modelId);
                    if (dataItem != null) {
                        outputWriter.print(dataItem);
                    } else {
                        outputWriter.print("");
                    }
                }


            }
            outputWriter.println();
        }
    }


    /**
     * Process the command line. If this returns true, the command line
     * was parsed and all options have been read and set.
     *
     * @param args the command line arguments
     * @return true of the command line parsed without problems.
     * @throws com.martiansoftware.jsap.JSAPException
     *          problem parsing the command line
     */
    private boolean processCommandLine(final String[] args) throws JSAPException {
        final JSAP jsap = new JSAP();

        final Parameter inputFilenameOption = new FlaggedOption("input")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setShortFlag('i')
                .setLongFlag("input")
                .setHelp("Input filename (such as model-conditions.txt)");
        jsap.registerParameter(inputFilenameOption);

        final Parameter outputFilenameOption = new FlaggedOption("output")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setShortFlag('o')
                .setLongFlag("output")
                .setHelp("Output filename (such as model-conditions-columns.txt)");
        jsap.registerParameter(outputFilenameOption);

        final JSAPResult result = jsap.parse(args);
        if (!result.success()) {
            System.err.println(jsap.getHelp());
            return false;
        }

        inputFilename = result.getString("input");
        outputFilename = result.getString("output");
        return true;
    }

}