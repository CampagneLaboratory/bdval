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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Describe class here.
 *
 * @author Kevin Dorff
 */
public class RewriteModelConditions {

    private String inputFilename;
    private String outputFilename;

    /**
     * Main method, starts the work.
     *
     * @param args the command line arguments
     * @throws JSAPException problem parsing the command line
     * @throws IOException   problem read/writing the files
     */
    public static void main(final String[] args) throws JSAPException, IOException {
        final RewriteModelConditions work = new RewriteModelConditions();
        work.process(args);
    }


    /**
     * Process the command line, read the input file, write the output file.
     *
     * @param args the command line arguments
     * @throws JSAPException problem parsing the command line
     * @throws IOException   problem read/writing the files
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
        final File outputFile = new File(outputFilename);
        if (outputFile.exists()) {
            // This file CANNOT be appended to!
            outputFile.delete();
        }
        final PrintWriter outputWriter = new PrintWriter(
                new FileWriter(outputFile));

        final List<String> lines = FileUtils.readLines(new File(inputFilename));
        final List<String> columns = new LinkedList<String>();
        for (final String line : lines) {
            final LinkedHashMap<String, String> data = readData(line);
            for (final String column : sortColumns(data.keySet())) {
                if (!columns.contains(column)) {
                    columns.add(column);
                }
            }
        }
        writeHeaders(outputWriter, columns);
        for (final String line : lines) {
            final LinkedHashMap<String, String> data = readData(line);
            writeData(outputWriter, columns, data);
        }
        outputWriter.flush();
        IOUtils.closeQuietly(outputWriter);
    }
    // Force model-id to be the very first column..

    private String[] sortColumns(final Set<String> strings) {
        final String[] result = new String[strings.size()];
        if (strings.contains("model-id")) {
            result[0] = "model-id";
            strings.remove("model-id");
        } else{
            System.err.println("Each line of the model conditions file must contain a model-id");
            System.exit(10);
        }
        int i = 1;

        for (final String columnId : strings) {
            assert i<result.length;
            result[i++] = columnId;
        }
        return result;
    }

    /**
     * Parse the data from a line of input to an (ordered) Map of results.
     *
     * @param line the line to parse
     * @return the pased line of data
     */
    private LinkedHashMap<String, String> readData(final String line) {
        final LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        final String[] items = StringUtils.split(line, "\t");
        for (final String item : items) {
            final String[] parts = StringUtils.split(item, "=", 2);
            data.put(parts[0], parts[1]);
        }
        return data;
    }

    /**
     * Write the headers for the output file.
     *
     * @param outputWriter the writer to write to
     * @param columns      the columns to write as headers
     */
    private void writeHeaders(final PrintWriter outputWriter, final List<String> columns) {
        int colNum = 0;
        for (final String column : columns) {
            if (colNum++ > 0) {
                outputWriter.print("\t");
            }
            outputWriter.print(column);
        }
        outputWriter.println();
    }

    /**
     * Write a line of data to the output file.
     *
     * @param outputWriter the writer to write to
     * @param columns      the columns that exist in the output file
     * @param data         the line of data to write to the output file
     */
    private void writeData(
            final PrintWriter outputWriter,
            final List<String> columns,
            final LinkedHashMap<String, String> data) {

        int colNum = 0;
        for (final String column : columns) {
            if (colNum++ > 0) {
                outputWriter.print("\t");
            }
            final String colData = data.get(column);
            if (colData == null) {
                outputWriter.print("N/A");
            } else {
                outputWriter.print(colData);
            }
        }
        outputWriter.println();
    }

    /**
     * Process the command line. If this returns true, the command line
     * was parsed and all options have been read and set.
     *
     * @param args the command line arguments
     * @return true of the command line parsed without problems.
     * @throws JSAPException problem parsing the command line
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
