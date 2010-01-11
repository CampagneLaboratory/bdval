/*
 * Copyright (C) 2005-2010 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.geo.tools;

import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.readers.GeoDataSetReader;
import edu.mssm.crover.tables.readers.SyntaxErrorException;
import edu.mssm.crover.tables.writers.InsightfulMinerTableWriter;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A tool to convert a Geo GDS file to an insightfulMiner file. This tool
 * supports adding a numerical label to selected columns. Use -l 1 -group
 * columns-set-file to specify labels and groups. The options can be repeated in
 * pair as many times as needed. User: campagne Date: Aug 31, 2005 Time: 1:40:44
 * PM
 */
public class GDS2InsightfulMiner {
    public static void main(final String[] args) {
        final GDS2InsightfulMiner processor = new GDS2InsightfulMiner();
        processor.proccess(args);
    }

    private void proccess(final String[] args) {
        // create the Options
        final Options options = new Options();

        // help
        options.addOption("h", "help", false, "print this message");

        // input file name
        final Option inputOption = new Option("i", "input", true,
                "specify a GEO data set file (GDS file)");
        inputOption.setArgName("file");
        inputOption.setRequired(true);
        options.addOption(inputOption);

        // output file name
        final Option outputOption = new Option("o", "output", true,
                "specify the destination file");
        outputOption.setArgName("file");
        outputOption.setRequired(true);
        options.addOption(outputOption);
        // label values
        final Option labelOptions = new Option("l", "label", true,
                "specify a label to tag a set of columns");
        labelOptions.setArgName("double-value");
        labelOptions.setRequired(false);
        options.addOption(labelOptions);

        // group file names
        final Option groupOptions = new Option("g", "group", true,
                "specify a file that named columns associated to a label. Each -group option must match a -label option. Matching is done according to the order on the command line. Each line of the group file identifies a column in the GEO data set that is to be labeled according to the corresponding label");
        groupOptions.setArgName("file");
        groupOptions.setRequired(false);
        options.addOption(groupOptions);

        // default label value
        final Option defaultLabelOption = new Option("dl", "default-label", true,
                "Specify the label to use for columns that are not identified by -l -g pairs. Default value is zero.");
        groupOptions.setArgName("double-value");
        groupOptions.setRequired(false);
        options.addOption(defaultLabelOption);
        // parse the command line arguments
        CommandLine line = null;
        double defaultLabelValue = 0;
        try {
            // create the command line parser
            final CommandLineParser parser = new BasicParser();
            line = parser.parse(options, args, true);
            if ((line.hasOption("l") && !line.hasOption("g")) ||
                    (line.hasOption("g") && !line.hasOption("l"))) {
                System.err.println("Options -label and -group must be used together.");
                System.exit(10);
            }
            if (line.hasOption("l") && line.getOptionValues("l").length != line.getOptionValues("g").length) {
                System.err.println("The number of -label and -group options must match exactly.");
                System.exit(10);
            }
            if (line.hasOption("dl")) {
                defaultLabelValue = Double.parseDouble(line.getOptionValue("dl"));
            }

        } catch (ParseException e) {
            System.err.println(e.getMessage());
            usage(options);
            System.exit(1);
        }
        // print help and exit
        if (line.hasOption("h")) {
            usage(options);
            System.exit(0);
        }
        try {
            final Map<Double, Set<String>> labels = readLabelGroups(line.getOptionValues("l"), line.getOptionValues("g")); //labels
            convert(line.getOptionValue("i"), line.getOptionValue("o"), labels, defaultLabelValue);
            System.exit(0);
        } catch (FileNotFoundException e) {

            System.err.println("Error opening file: \n");
            printGroups(line);
        } catch (IOException e) {
            System.err.println("An error occurred reading one of the group files:\n");
            printGroups(line);
        }


    }

    private void printGroups(final CommandLine line) {
        final String[] groups = line.getOptionValues("g");
        for (int i = 0; i < groups.length; i++) {
            final String group = groups[i];
            System.err.println("group: " + group);
        }
    }

    private Map<Double, Set<String>> readLabelGroups(final String[] labels, final String[] groups) throws IOException {
        final Map<Double, Set<String>> labelsGroups = new HashMap<Double, Set<String>>();
        if (groups == null || labels == null) {
            return labelsGroups;
        }
        // load each group file
        int groupIndex = 0;
        for (final String group : groups) {
            final BufferedReader br = new BufferedReader(new FileReader(group));
            // each line in the group file is a column identifier
            final Set<String> columnSet = new HashSet<String>();
            String line;
            while ((line = br.readLine()) != null) {
                columnSet.add(line.trim());
            }

            final double label = Double.parseDouble(labels[groupIndex]);
            labelsGroups.put(label, columnSet);
            groupIndex++;
        }
        return labelsGroups;

    }


    private void convert(final String inputFileName, final String outputFileName,
                         final Map<Double, Set<String>> labels, final double defaultLabel) {
        final GeoDataSetReader reader = new GeoDataSetReader();
        try {
            for (final Double label : labels.keySet()) {
                reader.labelColumnGroup(label, labels.get(label));
            }
            reader.setDefaultLabelValue(defaultLabel);
            final Table table = reader.read(new FileReader(inputFileName));
            InsightfulMinerTableWriter.write(table, null, new FileWriter(outputFileName), outputFileName);
        } catch (SyntaxErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Print usage message for main method.
     *
     * @param options Options used to determine usage
     */
    private void usage(final Options options) {
        // automatically generate the help statement
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(GDS2InsightfulMiner.class.getName(), options, true);
    }

}
