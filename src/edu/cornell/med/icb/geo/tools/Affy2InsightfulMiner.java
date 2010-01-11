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

import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.ColumnTypeException;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import edu.mssm.crover.tables.readers.AffymetrixTxtFormatReader;
import edu.mssm.crover.tables.readers.SyntaxErrorException;
import edu.mssm.crover.tables.writers.InsightfulMinerTableWriter;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * A tool to convert Affymetrix text files to an insightfulMiner file. This tool
 * supports adding a numerical label to selected columns. Use -class-label
 * Gender,M,-1 -class-label Gender,F,+1 to set label to -1 for samples with
 * Gender=M, and to +1 for samples with Gender=F Run the tool with --help for
 * details about all supported options.
 *
 * @author Fabien Campagne
 */
public class Affy2InsightfulMiner {
    private static final String COLUMN_NAME_SAMPLE_ID = "sample-id";
    private Map<String, Double> sampleLabels;

    private GEOPlatform platform;

    public static void main(final String[] args) {
        final Affy2InsightfulMiner processor = new Affy2InsightfulMiner();
        processor.proccess(args);
    }

    private void proccess(final String[] args) {
        // create the Options
        final Options options = new Options();

        // help
        options.addOption("h", "help", false, "print this message");

        // input file name
        final Option inputOption = new Option("il", "input-list", true,
                "specify the name of the input file list. This file tab-separated with two columns. Each row must indicate: (1) filename for an Affymetrix text file for one sample, (2) sample ID.");
        inputOption.setArgName("input-file-list");
        inputOption.setRequired(true);
        options.addOption(inputOption);

        // output file name
        final Option outputOption = new Option("o", "output", true,
                "specify the destination file");
        outputOption.setArgName("file");
        outputOption.setRequired(true);
        options.addOption(outputOption);
        // label values
        final Option labelOptions = new Option("cl", "class-label", true,
                "specify how to set the label of samples");
        labelOptions.setArgName("attribute,value,label");
        labelOptions.setRequired(false);
        options.addOption(labelOptions);

        // group file names
        final Option groupOptions = new Option("sa", "sample-attributes", true,
                "specify a file that associates attributes to samples. The file is tab delimited. The first column of this file is the the sample ID. Additional columns indicate the value of the attributes named in the first line (name of sample ID is can be arbitrary and will be ignored).");
        groupOptions.setArgName("file");
        groupOptions.setRequired(false);
        options.addOption(groupOptions);

        // default label value
        final Option defaultLabelOption = new Option("dl", "default-label", true,
                "Specify the label to use for columns that are not identified by -l -g pairs. Default value is zero.");
        groupOptions.setArgName("double-value");
        groupOptions.setRequired(false);

        // platform description
        final Option platformDescriptionFileOption = new Option("pd", "platform-description", true,
                "The platform description is a GEO platform description file that is used to link probe set ids to genbank identifiers. When a platform description file is provided, the second column in the output will contain the desired indentifier (default genbank).");
        groupOptions.setArgName("file");
        groupOptions.setRequired(false);
        options.addOption(platformDescriptionFileOption);

        // parse the command line arguments
        CommandLine line = null;
        double defaultLabelValue = 0;
        try {
            // create the command line parser
            final CommandLineParser parser = new BasicParser();
            line = parser.parse(options, args, true);
            if ((line.hasOption("cl") && !line.hasOption("sa")) ||
                    (line.hasOption("sa") && !line.hasOption("cl"))) {
                System.err.println("Options -class-label and -sample-attributes must be used together.");
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
            readInputList(line.getOptionValue("il"));
            readSampleAttributes(line.getOptionValue("sa"));
            final String platformFilename = line.getOptionValue("pd");
            System.out.println("Reading platformFileContent description file " + platformFilename);
            platform = new GEOPlatform();
            platform.read(platformFilename);
            System.out.println("Successfully read " + platform.getProbesetCount() + " probe set -> secondary identifier pairs.");

            readAndAssembleSamples(line.getOptionValues("cl"), defaultLabelValue, line.getOptionValue("o"));

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(10);
        }
    }


    private double defaultLabelValue;

    private void readAndAssembleSamples(final String[] classLabels, final double defaultLabelValue, final String outputFilename) throws IOException, TypeMismatchException, InvalidColumnException {
        final Map<String, Vector<Double>> assemblingBench = createAssemblingBench();
        this.defaultLabelValue = defaultLabelValue;
        parseClassLabels(classLabels);
        // create an output table
        final Table outputTable;
        outputTable = new ArrayTable();
        int identifierIdIndex = -1;
        int probesetIdIndex = -1;
        probesetIdIndex = outputTable.addColumn("ID_REF", String.class);
        identifierIdIndex = outputTable.addColumn("IDENTIFIER", String.class);

        for (final String sampleIdentifier : sampleIdentifiers) {
            // add a column for each sample identifier.
            // This is where we will store the Signal for the sample.
            outputTable.addColumn(sampleIdentifier, String.class);
        }

        calculateSampleLabels(outputTable);
        moveDataToOutput(assemblingBench, outputTable, probesetIdIndex, identifierIdIndex);

        writeToInsightfulMiner(outputFilename, outputTable);

    }

    private void calculateSampleLabels(final Table outputTable) throws TypeMismatchException {
        // Set the label for each sample:
        if (sampleLabels != null) {
            outputTable.appendObject(0, "label");
            outputTable.appendObject(1, "label");
            for (int i = 0; i < sampleIdentifiers.length; i++) {
                outputTable.appendObject(i + 2, calculateLabel(sampleIdentifiers[i]));
            }
        }
    }

    private void parseClassLabels(final String[] classLabels) throws TypeMismatchException, InvalidColumnException {
        if (classLabels == null) {
            return;
        }
        System.out.println("Parsing class labels");
        // for each sample id, associate the value of the label
        sampleLabels = new HashMap<String, Double>();

        for (final String classLabelDirective : classLabels) {
            final String[] tokens = classLabelDirective.split(",");
            final String attributeName = tokens[0];
            final String attributeValue = tokens[1];
            final String labelValue = tokens[2];

            final Table.RowIterator ri = sampleAttributes.firstRow();
            while (!ri.end()) {

                final String sampleId = (String) sampleAttributes.getValue(0, ri);
                if (!sampleAttributes.isColumn(attributeName)) {
                    System.err.println("The attribute " + attributeName + " is not defined in the sample attribute file.");
                    System.exit(1);
                }
                final int attributeColumnIndex = sampleAttributes.getColumnIndex(attributeName);

                if (attributeValue.equals(
                        sampleAttributes.getValue(attributeColumnIndex, ri))) {

                    sampleLabels.put(sampleId, Double.parseDouble(labelValue));
                }
                ri.next();
            }
        }


    }

    private String calculateLabel(final String sampleIdentifier) {
        Double label;
        label = sampleLabels.get(sampleIdentifier);
        if (label == null) {
            label = defaultLabelValue;
        }
        return Double.toString(label);
    }

    private Map<String, Vector<Double>> createAssemblingBench() {
        // the assemblingBench will map a probeset id to a list of signal intensities (represented as strings)
        final Map<String, Vector<Double>> assemblingBench = new HashMap<String, Vector<Double>>();
        final AffymetrixTxtFormatReader reader = new AffymetrixTxtFormatReader();
        for (int i = 0; i < filenames.length; i++) {
            final String filename = filenames[i];
            try {
                System.out.println("Reading (sample id:" + sampleIdentifiers[i] + " sample file:" + filename + ")");
                final Table sampleTable = reader.read(new FileReader(filename));
                appendSignal(assemblingBench, sampleTable, sampleIdentifiers[i]);
            } catch (SyntaxErrorException e) {
                e.printStackTrace();
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Sample input file " + filename
                        + "  could not be found. Details may follow." + e.getMessage());
                System.exit(1);
            }
        }
        return assemblingBench;
    }

    private void writeToInsightfulMiner(final String outputFilename,
                                        final Table outputTable) throws IOException {
        System.out.println("Writing output (" + outputFilename + ")");
        // write outputTable to InsightfulMiner format:
        Writer outputFileWriter = null;
        try {
            outputFileWriter = new FileWriter(outputFilename);
            InsightfulMinerTableWriter.write(outputTable, null, outputFileWriter, null);
        } finally {
            IOUtils.closeQuietly(outputFileWriter);
        }
    }

    private void moveDataToOutput(final Map<String, Vector<Double>> assemblingBench, final Table outputTable, final int probesetIdIndex, final int identifierIdIndex) {
        // move data to the output table:
        final Iterator keys = assemblingBench.keySet().iterator();

        try {
            while (keys.hasNext()) {
                final String probesetID = (String) keys.next();

                outputTable.appendObject(probesetIdIndex, probesetID);
                String secondaryIdentifier = "";
                if (platform != null) {
                    secondaryIdentifier = platform.getGenbankId(probesetID);
                }
                outputTable.appendObject(identifierIdIndex, secondaryIdentifier);
                int columnIndex = identifierIdIndex + 1;
                for (final double value : assemblingBench.get(probesetID)) {
                    outputTable.appendObject(columnIndex++, Double.toString(value));
                }
            }
        } catch (TypeMismatchException e) {
            System.err.println("An error occurred transferring data to output table. Details may be provided below. \n" + e.getMessage());
        }
    }
// for each row of the table, append the Signal at the end of the Vector corresponding to the probeset.

    private void appendSignal(final Map<String, Vector<Double>> assemblingBench, final Table sampleTable, final String sampleIdentifier) {

        final Table.RowIterator ri = sampleTable.firstRow();

        try {
            final int signalColumnIndex = sampleTable.getColumnIndex("Signal");
            final int probeSetColumnIndex = sampleTable.getColumnIndex("Probe Set Name");
            while (!ri.end()) {

                final String probesetId = (String) sampleTable.getValue(probeSetColumnIndex, ri);
                Vector<Double> signalValues = assemblingBench.get(probesetId);
                if (signalValues == null) {
                    signalValues = new Vector<Double>();
                }
                final Double signalValue = sampleTable.getDoubleValue(signalColumnIndex, ri);

                signalValues.add(signalValue);
                assemblingBench.put(probesetId, signalValues);

                ri.next();
            }
        } catch (TypeMismatchException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new InternalError();
        } catch (InvalidColumnException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new InternalError();
        }
    }


    private void readSampleAttributes(final String sampleAttributeFileName) throws IOException {
        if (sampleAttributeFileName == null) {
            return;
        }
        sampleAttributes = new ArrayTable();
        final BufferedReader br = new BufferedReader(new FileReader(sampleAttributeFileName));
        final String headerLine = br.readLine();
        String[] tokens = headerLine.split("\t");
        sampleAttributes.addColumn(COLUMN_NAME_SAMPLE_ID, String.class);
        for (int i = 1; i < tokens.length; i++) {
            final String columnName = tokens[i];
            sampleAttributes.addColumn(columnName, String.class);
        }
        String line;
        final int columnIndex = 0;
        while ((line = br.readLine()) != null) {
            tokens = line.split("\t");
            for (int i = 0; i < tokens.length; i++) {
                final String dataValue = tokens[i];
                try {
                    sampleAttributes.parseAppend(i, dataValue);
                } catch (ColumnTypeException e) {
                    System.err.println("Error while parsing sample attribute files:");
                    System.err.println("Cannot append value " + dataValue + " to column " + sampleAttributes.getIdentifier(columnIndex));
                }

            }


        }

    }

    private String[] filenames;
    private String[] sampleIdentifiers;
    private Table sampleAttributes;

    private void readInputList(final String inputListFilename) throws IOException {

        final Vector<String> names = new Vector<String>();
        final Vector<String> ids = new Vector<String>();
        String line;

        final BufferedReader br = new BufferedReader(new FileReader(inputListFilename));
        while ((line = br.readLine()) != null) {
            final String[] tokens = line.split("\t");
            names.add(tokens[0]);
            ids.add(tokens[1]);


        }
        if (names.size() != ids.size()) {
            System.err.println("Error reading input file list (" + inputListFilename + "): the number of identifiers does not match the number of filenames.");
            System.exit(1);
        }
        final int datasetSize = ids.size();

        filenames = new String[datasetSize];
        sampleIdentifiers = new String[ids.size()];

        for (int i = 0; i < filenames.length; i++) {
            filenames[i] = names.get(i);
            sampleIdentifiers[i] = ids.get(i);
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
        formatter.printHelp(Affy2InsightfulMiner.class.getName(), options, true);
    }

}
