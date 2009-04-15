/*
 * Copyright (C) 2006-2009 Institute for Computational Biomedicine,
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

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Post-process statistics from MicroArrayTrainEvaluate, to calculate
 * performance rank for each gene list.
 *
 * @author Fabien Campagne Date: Mar 5, 2006 Time: 1:24:28 PM
 */
public class CalculateRanks {
    private final Set<String> geneListNames;
    private boolean filterWithShuffleTest;

    public CalculateRanks() {
        super();
        geneListNames = new HashSet<String>();
    }

    public static void main(final String[] args) throws IOException {
        final CalculateRanks processor = new CalculateRanks();
        processor.proccess(args);
    }

    private void proccess(final String[] args) throws IOException {
        // create the Options
        final Options options = new Options();

        // help
        options.addOption("h", "help", false, "print this message. This program compares the performance measures obtained " +
                "with each gene list and determines the rank of each gene list.");

        // input file name
        final Option inputOption = new Option("i", "input", true,
                "specify the path to the statistics file created by MicroarrayTrainEvaluate.");
        inputOption.setArgName("file");
        inputOption.setRequired(true);
        options.addOption(inputOption);

        // output file name
        final Option outputOption = new Option("o", "output", true,
                "specify the destination file where post-processed statistics will be written");
        outputOption.setArgName("file");
        outputOption.setRequired(true);
        options.addOption(outputOption);

        // tallies file name
        final Option talliesOption = new Option("t", "tallies", true,
                "specify the file where tallies of rank per gene list will be written");
        talliesOption.setArgName("file");
        talliesOption.setRequired(false);
        options.addOption(talliesOption);

        // filter by shuffle test P-value
        final Option filterShuffleTestOption = new Option("f", "filter-shuffle-test", false,
                "indicate that only those results that pass the shuffle significance test should be used to calculate ranks and tallies.");
        filterShuffleTestOption.setRequired(false);

        options.addOption(filterShuffleTestOption);

        // parse the command line arguments
        CommandLine line = null;
        try {
            // create the command line parser
            final CommandLineParser parser = new BasicParser();
            line = parser.parse(options, args, true);


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
        this.filterWithShuffleTest = line.hasOption("f");
        if (this.filterWithShuffleTest) {
            System.out.println("Filtering classification results by shuffle test.");
        }
        postProcess(line.getOptionValue("i"), line.getOptionValue("o"), line.getOptionValue("t"));
    }

    private void postProcess(final String inputFilename,
                             final String outputFilename, final String talliesFilename)
            throws IOException {
        final PrintWriter outputWriter =
                new PrintWriter(new FileWriter(outputFilename));
        PrintWriter talliesWriter = null;
        if (talliesFilename != null) {
            talliesWriter = new PrintWriter(new FileWriter(talliesFilename));
        }

        postProcess(new FileReader(inputFilename), outputWriter, talliesWriter);
        outputWriter.close();
        if (talliesWriter != null) {
            talliesWriter.close();
        }
    }

    private void postProcess(final FileReader inputReader,
                             final PrintWriter outputWriter, final PrintWriter talliesWriter)
            throws IOException {
        final BufferedReader br = new BufferedReader(inputReader);
        String line;
        final Vector<ClassificationResults> allResults = new Vector<ClassificationResults>();
        final Map<String, List<ClassificationResults>> organizer =
                new HashMap<String, List<ClassificationResults>>();

        final Comparator<ClassificationResults> looAccuracyComparator = new Comparator<ClassificationResults>() {
            public int compare(final ClassificationResults o1, final ClassificationResults o2) {
                return (int) Math.round(o2.getLooPerformance().getAccuracy() - o1.getLooPerformance().getAccuracy());
            }
        };

        // read stats from input file:
        while ((line = br.readLine()) != null) {
            if (line.startsWith("Dataset\t")) {

                continue; // skip headers.
            }

            final ClassificationResults result = ClassificationResults.create(line);
            if (filterWithShuffleTest) {
                if (result.getShuffleGEP() < 5) {
                    allResults.add(result);
                }
            } else {
                allResults.add(result);
            }

            geneListNames.add(result.getGeneListName());
            final String key = result.getClassificationUnique();

            List<ClassificationResults> resultForClassification =
                    organizer.get(key);
            if (resultForClassification == null) {
                resultForClassification = new Vector<ClassificationResults>();
            }
            resultForClassification.add(result);
            organizer.put(key, resultForClassification);
        }

        final NumberFormat formatter = new DecimalFormat();
        formatter.setMaximumFractionDigits(2);

        // calculate ranks and output:
        {
            ClassificationResults.writeHeader(outputWriter, filterWithShuffleTest);
            final Iterator<List<ClassificationResults>> iterator = organizer.values().iterator();
            while (iterator.hasNext()) {
                final List<ClassificationResults> resultsForClassification = iterator.next();
                // first sort by accuracy:
                Collections.sort(resultsForClassification, looAccuracyComparator);
                // then, iterate the list and assign ranks:
                int rank = 0;
                double previousPerformanceValue = -1;
                for (final ClassificationResults singleResult : resultsForClassification) {
                    final double currentPerformanceValue = singleResult.getLooPerformance().getAccuracy();
                    if (currentPerformanceValue != previousPerformanceValue) {
                        rank++;
                        previousPerformanceValue = currentPerformanceValue;
                    }
                    singleResult.setRank(rank);
                    singleResult.write(outputWriter, formatter);
                }
            }
        }
        // tally ranks per gene list:
        if (talliesWriter != null) {
            talliesWriter.write("Gene List\tRank\tTally\n");
            for (final String geneListName : geneListNames) {
                System.out.println(geneListName + " gene list:");
                final Int2IntMap rankTallies = new Int2IntOpenHashMap();

                final Iterator<ClassificationResults> iterator = allResults.iterator();
                while (iterator.hasNext()) {
                    final ClassificationResults classificationResults = iterator.next();
                    if (!classificationResults.getGeneListName().equals(geneListName)) {
                        continue;
                    }

                    int tally = rankTallies.get(classificationResults.getRank());
                    tally++;
                    rankTallies.put(classificationResults.getRank(), tally);

                }

                final Set<Map.Entry<Integer, Integer>> entries = rankTallies.entrySet();
                for (final Map.Entry<Integer, Integer> entry : entries) {
                    final Object rank = entry.getKey();
                    final Object tally = entry.getValue();
                    System.out.println("rank: " + rank + " tally: " + tally);
                    if (talliesWriter != null) {
                        talliesWriter.write(geneListName);
                        talliesWriter.write("\t");
                        talliesWriter.write(rank.toString());
                        talliesWriter.write("\t");
                        talliesWriter.write(tally.toString());
                        talliesWriter.write("\n");
                    }
                }

            }
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
        formatter.printHelp(MicroarrayTrainEvaluate.class.getName(), options, true);
    }
}
