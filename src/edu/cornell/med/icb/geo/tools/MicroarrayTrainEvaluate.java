/*
 * Copyright (C) 2006-2010 Institute for Computational Biomedicine,
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

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import edu.cornell.med.icb.learning.tools.svmlight.EvaluationMeasure;
import edu.cornell.med.icb.learning.tools.svmlight.SVMLightDriver;
import edu.cornell.med.icb.tissueinfo.similarity.clustering.Cluster;
import edu.mssm.crover.tables.*;
import edu.mssm.crover.tables.readers.ColumbiaTmmReader;
import edu.mssm.crover.tables.readers.GeoDataSetReader;
import edu.mssm.crover.tables.readers.SyntaxErrorException;
import edu.mssm.crover.tables.readers.WhiteheadResReader;
import edu.mssm.crover.tables.treatments.AggregateClusters;
import edu.mssm.crover.tables.writers.LibSVMWriter;
import edu.mssm.crover.tables.writers.SVMLightWriter;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.cli.*;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;


/**
 * Train and evaluate an SVM with microarray and patient data. Reads a RES file, a list of classifications to perform,
 * process data for classification, train and evaluate SVMs for each classification, output the results in a summary
 * file.
 *
 * @author Fabien Campagne Date: Feb 27, 2006 Time: 4:41:58 PM
 */
public class MicroarrayTrainEvaluate {
    private HashMap<String, String> probesetId2Genbank;
    private GEOPlatform platform;
    private RandomEngine randomGenerator;
    private final boolean shuffleLabelTest = true;
    private int countShuffleLabelTests = 500;
    private int randomBatchSize;
    private final int DEFAULT_RANDOM_BATCH_SIZE = 10;
    private boolean adjustSignalToFloorValue;
    private static final float DEFAULT_SIGNAL_FLOOR_VALUE_SINGLE_CHANNEL = 300;
    private static final float DEFAULT_SIGNAL_FLOOR_VALUE_TWO_CHANNELS = 1.15f;
    private float signalFloorValue;
    private boolean svmLight;
    private int timeout;
    private boolean oneChannelArray;
    public static final String IDENTIFIER_COLUMN_NAME = "ID_REF";
    private NumberFormat formatter;
    private ObjectList<Cluster> clusters;


    public static void main(final String[] args) {
        final MicroarrayTrainEvaluate processor = new MicroarrayTrainEvaluate();
        processor.proccess(args);
    }

    private void proccess(final String[] args) {
        // create the Options
        final Options options = new Options();

        // help
        options.addOption("h", "help", false, "print this message. When -r is used with a non zero integer n, " +
                "a set of n random gene lists is evaluated for each line of the task list. The evaluation statistics " +
                "reported is the number of random gene lists that obtained the same or better leave-one-out performance than "
                +
                "the gene list from the task list. Accuracy is used to evaluate performance for random trials. ");

        // input file name
        final Option inputOption = new Option("i", "input", true,
                "specify a GEO or RES data set file (GDS file or whitehead RES format)");
        inputOption.setArgName("file");
        inputOption.setRequired(true);
        options.addOption(inputOption);

        // output file name
        final Option outputOption = new Option("o", "output", true,
                "specify the destination file where statistics will be written");
        outputOption.setArgName("file");
        outputOption.setRequired(true);
        options.addOption(outputOption);

        // task list file name
        final Option tasksFilenameOption = new Option("t", "task-list", true,
                "specify the file with the list of tasks to perform. This file is tab separated");
        tasksFilenameOption.setArgName("file");
        tasksFilenameOption.setRequired(true);
        options.addOption(tasksFilenameOption);

        // condition identifiers file name
        final Option conditionsIdsFilenameOption = new Option("c", "conditions", true,
                "specify the file with the mapping condition-name column-identifier (tab separated, on mapping per line).");
        conditionsIdsFilenameOption.setArgName("file");
        conditionsIdsFilenameOption.setRequired(true);
        options.addOption(conditionsIdsFilenameOption);

        // gene lists file name
        final Option geneListsFilenameOption = new Option("g", "gene-list", true,
                "specify the file with the gene lists (one per line).");
        geneListsFilenameOption.setArgName("file");
        geneListsFilenameOption.setRequired(true);
        options.addOption(geneListsFilenameOption);

        // Geo platform description file name
        final Option platformDescriptionFilenameOption = new Option("p", "platform", true,
                "specify the platform description file(s) (GEO GPL file format). Multiple platorm files may be provided "
                        +
                        " if separated by coma (e.g., file1,file2). Each file is read in the order provided.");
        platformDescriptionFilenameOption.setArgName("file,file");
        platformDescriptionFilenameOption.setRequired(true);
        options.addOption(platformDescriptionFilenameOption);

        // Number of random runs  (picking probesets randomly that are not in the gene list)
        final Option randomRunCountOption = new Option("r", "random-run-count", true,
                "Number of random runs to execute for each random gene list.");
        randomRunCountOption.setArgName("number");
        randomRunCountOption.setRequired(false);
        options.addOption(randomRunCountOption);

        // Number of probeset to randomly select for each random run
        final Option batchSizeRandomRun = new Option("b", "batch-size-random-run", true,
                "Number of probeset to randomly select for each random run. Default is " + DEFAULT_RANDOM_BATCH_SIZE +
                        ".");
        batchSizeRandomRun.setArgName("number");
        batchSizeRandomRun.setRequired(false);
        options.addOption(batchSizeRandomRun);

        // Number of shuffle runs (shuffling the training label).
        final Option shuffleRunCountOption = new Option("u", "shuffle-run-count", true,
                "Number of shuffle runs (label permutations) to execute for each training set.");
        shuffleRunCountOption.setArgName("number");
        shuffleRunCountOption.setRequired(false);
        options.addOption(shuffleRunCountOption);

        // Minimal signal value for signal floor adjustment.
        final Option floorSignalValueOption = new Option("f", "floor", true,
                "Specify a floor value for the signal. If a signal is lower than the floor, it is set to the floor.");
        floorSignalValueOption.setArgName("number");
        floorSignalValueOption.setRequired(false);
        options.addOption(floorSignalValueOption);

        // distinguish between one channel array and two channel array
        final Option channelOption = new Option("a", "two-channel-array", false,
                "Indicate that the data is for a two channel array. This flag affects how the floor value is interpreted."
                        +
                        "For two channel arrays, values on the array are set to 1.0 if (Math.abs(oldValue-1.0)+1)<=floorValue, "
                        +
                        "whereas for one channel array the condition becomes: oldValue<=floorValue.");
        channelOption.setRequired(false);
        options.addOption(channelOption);

        // Minimal signal value for signal floor adjustment.
        final Option svmLightOption = new Option("l", "light", false,
                "Choose svmLight. (default libSVM).");
        svmLightOption.setRequired(false);
        options.addOption(svmLightOption);

        // Number of random runs
        final Option seedOption = new Option("s", "seed", true,
                "Seed for the number generator.");
        seedOption.setArgName("number");
        seedOption.setRequired(false);

        // Timeout for stopping svmlight in seconds if the process does not complete.
        final Option timeoutOption = new Option("x", "timeout", true,
                "Timeout for svmLight training, in seconds (default 3600 seconds/1h).");
        timeoutOption.setArgName("number");
        timeoutOption.setRequired(false);
        options.addOption(timeoutOption);

        final Option clusterFilename =
                new Option("e", "clusters", true, "Name of file that describes clusters of genes. ");
        clusterFilename.setArgName("filename");
        clusterFilename.setRequired(false);
        options.addOption(clusterFilename);

        // parse the command line arguments
        CommandLine line = null;
        final double defaultLabelValue = 0;
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
        try {
            int randomRunCount = 0;
            final String randomCount = line.getOptionValue("r");
            if (randomCount != null) {
                randomRunCount = Integer.parseInt(randomCount);
            }

            if (line.hasOption("l")) {
                this.svmLight = true;
            }
            System.out.println("Using " + (svmLight ? "svmLight" : "libSVM"));
            final String batchSize = line.getOptionValue("b");
            if (batchSize != null) {
                this.randomBatchSize = Integer.parseInt(batchSize);
            } else {
                this.randomBatchSize = DEFAULT_RANDOM_BATCH_SIZE;
            }
            System.out.println("Will do " + randomRunCount + " random runs with batch-size=" + randomBatchSize);

            if (line.hasOption("a")) {
                this.oneChannelArray = false;
            } else {
                this.oneChannelArray = true;
            }

            final String floorOptionValue = line.getOptionValue("f");
            if (floorOptionValue != null) {
                adjustSignalToFloorValue = true;
                if (oneChannelArray) {
                    this.signalFloorValue = DEFAULT_SIGNAL_FLOOR_VALUE_SINGLE_CHANNEL;
                    this.signalFloorValue = Integer.parseInt(floorOptionValue);
                } else {
                    this.signalFloorValue = DEFAULT_SIGNAL_FLOOR_VALUE_TWO_CHANNELS;
                    this.signalFloorValue = Float.parseFloat(floorOptionValue);
                }
                System.out.println("Clipping signal at floor value: " + signalFloorValue);
                System.out.println("This chip has " + (oneChannelArray ? " one channel." : "two channels."));
            }

            int seed = (int) new Date().getTime();
            if (line.getOptionValue("s") != null) {
                seed = Integer.parseInt(line.getOptionValue("s"));
            }
            randomGenerator = new MersenneTwister(seed);

            int timeout = 60 * 60; // in seconds. // 1 hour
            if (line.getOptionValue("x") != null) {
                if (!svmLight) {
                    System.out.println("Error: --timeout option can only be used with --light option.");
                    System.exit(1);
                }
                timeout = Integer.parseInt(line.getOptionValue("x"));
                System.out.println("Timeout set to " + timeout + " second(s).");
            }
            this.timeout = timeout * 1000;

            final ClassificationTask[] tasks =
                    readTasksAndConditions(line.getOptionValue("t"), line.getOptionValue("c"));
            final GeneList[] geneList = readGeneList(line.getOptionValue("p"), line.getOptionValue("g"));
            formatter = new DecimalFormat();
            formatter.setMaximumFractionDigits(2);

            final String uOption = line.getOptionValue("u");
            if (uOption == null) {
                countShuffleLabelTests = 0;
            } else {
                countShuffleLabelTests = Integer.parseInt(uOption);
            }


            System.out.println("Will do " + countShuffleLabelTests + " shuffling runs");
            final File outputFile = new File(line.getOptionValue("o"));
            final boolean outputFilePreexist = outputFile.exists();
            final PrintWriter statWriter =
                    new PrintWriter(new FileWriter(outputFile, true));   // append stats to output.
            if (!outputFilePreexist) {
                ClassificationResults.writeHeader(statWriter, shuffleLabelTest);
            }

            convert(line.getOptionValue("i"), statWriter, tasks, geneList,
                    randomRunCount);
            statWriter.close();
            System.exit(0);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            System.exit(10);
        }
    }


    public GeneList[] readGeneList(final String platformFilename,
                                   final String geneListFilename) throws IOException,
            SyntaxErrorException {
        // read platforms:
        final Vector<GEOPlatform> platforms = new Vector<GEOPlatform>();
        final String[] platformFilenames = platformFilename.split(",");
        for (final String pFilename : platformFilenames) {
            platform = new GEOPlatform();
            if ("dummy".equals(pFilename)) {
                platform = new DummyPlatform();
                platforms.add(platform);
                System.out.println("Will proceed with dummy platform.");
                break;
            }
            System.out.print("Reading platform " + pFilename + ".. ");
            System.out.flush();

            platform.read(pFilename);
            System.out.println("done.");
            System.out.flush();
            platforms.add(platform);
        }

        // read gene list info:
        final BufferedReader geneListReader =
                new BufferedReader(new FileReader(geneListFilename));
        String line;
        final Vector<GeneList> list = new Vector<GeneList>();
        while ((line = geneListReader.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }
            final String[] tokens = line.split("[\t]");
            assert tokens.length >= 1 : "gene list line must have at least 1 field. Line was :" + line;
            final GeneList geneList = GeneList.createList(tokens);
            geneList.setPlatforms(platforms);
            list.add(geneList);
        }
        return list.toArray(new GeneList[list.size()]);
    }

    public static ClassificationTask[] readTasksAndConditions(final String taskListFilename,
                                                              final String conditionIdsFilename) throws IOException {
        return ClassificationTask.parseTaskAndConditions(taskListFilename, conditionIdsFilename);
    }

    /**
     * Read a cids/ conditions file.
     *
     * @param conditionIdsFilename
     * @return
     * @throws IOException
     */
    public static ConditionIdentifiers readConditions(final String conditionIdsFilename) throws IOException {
        return ClassificationTask.readConditions(conditionIdsFilename);
    }


    private void convert(final String inputFileName, final PrintWriter statWriter, final ClassificationTask[] tasks,
                         final GeneList[] geneList, final int randomRepeatCount) {
        Table table = null;

        try {
            if (inputFileName.endsWith(".soft")) {
                final GeoDataSetReader reader = new GeoDataSetReader();
                table = reader.read(new FileReader(inputFileName));
            } else if (inputFileName.endsWith(".res")) {
                final WhiteheadResReader reader = new WhiteheadResReader();
                table = reader.read(new FileReader(inputFileName));
            } else if (inputFileName.endsWith(".tmm")) {
                final ColumbiaTmmReader reader = new ColumbiaTmmReader();
                table = reader.read(new FileReader(inputFileName));
            }

            if (table == null) {
                System.out.println("Unable to read/parse input file (" + inputFileName + ")");
                System.exit(10);
            }
            for (int i = 0; i < tasks.length; i++) {
                final ClassificationTask task = tasks[i];
                for (int j = 0; j < geneList.length; j++) {

                    System.out.print("Processing " + task.getFirstConditionName() + "/" + task.getSecondConditionName()
                            + " with gene list: " + geneList[j].getType());

                    final GeneList currentGeneList = geneList[j];
                    final OneRunResult result = process(table, calculateLabelValueGroups(task), currentGeneList,
                            shuffleLabelTest);  // shuffle if needed
                    System.out.print(" " + result.getSvmResult());
                    final int resultProbesetOverlap = (result.getFeatureTable().getColumnNumber() - 1);
                    System.out.print(" Overlap: " + resultProbesetOverlap);
                    System.out.flush();

                    double countAbovePreviousList = 0;
                    double totalRandomRuns = 0;
                    double percentRandomGreaterOrEqual = 0;
                    if (randomRepeatCount != 0
                            && !(geneList[j] instanceof FullGeneList)) { // do not do random repeats for full array

                        // repeat random n times:

                        for (int r = 0; r < randomRepeatCount; r++) {

                            final RandomGeneList randomGeneList =
                                    new RandomGeneList(resultProbesetOverlap, randomGenerator);
                            randomGeneList.setNumberOfProbesets(randomBatchSize);
                            randomGeneList.excludeGenesFromList(currentGeneList);
                            final OneRunResult resultRandom = processTaskSpecificTable(result.getTaskSpecificTable(),
                                    calculateLabelValueGroups(task), randomGeneList, false
                                    // no shuffling of random runs.
                            );
                            if (resultRandom.getSvmResult().getAccuracy() >= result.getSvmResult().getAccuracy()) {
                                countAbovePreviousList += 1;
                            }
                            totalRandomRuns += 1;
                            System.err.print(".");
                            System.err.flush();
                        }
                        System.err.println();
                        System.err.flush();
                        percentRandomGreaterOrEqual = (countAbovePreviousList / totalRandomRuns) * 100;
                        System.out.println(" Random runs above result: " + formatter.format(percentRandomGreaterOrEqual)
                                + "% (" + countAbovePreviousList + " / " + totalRandomRuns + ")");
                    } else {
                        System.out.println();
                    }
                    outputStatistics(statWriter, inputFileName, task, currentGeneList, percentRandomGreaterOrEqual,
                            result);

                }
                statWriter.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            statWriter.flush();
            System.exit(10);
        }
    }


    private void outputStatistics(final PrintWriter statWriter, final String inputFileName,
                                  final ClassificationTask task,
                                  final GeneList currentGeneList, final double percentRandomGreaterOrEqual,
                                  final OneRunResult result) {
        final int resultProbesetOverlap = result.getOverlap();
        final EvaluationMeasure svmMeasures = result.getSvmResult();
        if (svmMeasures == null) {
            return;
        }
// format is :
// input-filename gene-list-name overlap accuracy precision recall f-1 percentRandomGreaterOrEqual condition1 condition2 n1 n2
        final ClassificationResults data =
                new ClassificationResults(inputFileName, currentGeneList.getType(), resultProbesetOverlap,
                        svmMeasures, percentRandomGreaterOrEqual, task);
        if (shuffleLabelTest) {
            data.setShuffleTestGECounts(result.getCountShuffleGE(), result.getTotalShufflingTests());
        }
        data.write(statWriter, formatter);
    }

    private OneRunResult process(final Table source, final List<Set<String>> labelValueGroups, final GeneList geneList,
                                 final boolean shuffling)
            throws ColumnTypeException, TypeMismatchException, InvalidColumnException, IOException {

        final Table result = filterColumnsForTask(source, labelValueGroups, IDENTIFIER_COLUMN_NAME, getAllSamples(labelValueGroups));

        return processTaskSpecificTable(result, labelValueGroups, geneList, shuffling);

    }

    public static Set<String> getAllSamples(List<Set<String>> labelValueGroups) {
        ObjectOpenHashSet<String> samples = new ObjectOpenHashSet<String>();
        for (Set<String> list : labelValueGroups) {
            samples.addAll(list);
        }
        return samples;
    }

    private OneRunResult processTaskSpecificTable(final Table result, final List<Set<String>> labelValueGroups,
                                                  final GeneList geneList, final boolean shuffling)
            throws TypeMismatchException, InvalidColumnException, ColumnTypeException, IOException {
        final OneRunResult finalResult = new OneRunResult();
        finalResult.setTaskSpecificTable(result);
        int idColumnIndex = 0;
        try {
            idColumnIndex = result.getColumnIndex(IDENTIFIER_COLUMN_NAME);
        } catch (InvalidColumnException e) {
            System.out.println("Column " + IDENTIFIER_COLUMN_NAME
                    + " could not be found in input file. Unable to filter by probeset.");
            System.exit(10);
        }

        // Check that task specific tabel is normalized:
        final int[] columnSelection = normalizeAcrossConditions(result);

        // 2. Filter out rows using gene list.
        final Set<String> validProbesetIds =
                geneList.calculateProbeSetSelection(result, idColumnIndex);

        if (adjustSignalToFloorValue) {
            // Set a floor on signal at signalFloorValue:
            final RowFloorAdjustmentCalculator floorAdjust =
                    new RowFloorAdjustmentCalculator(columnSelection, signalFloorValue,
                            oneChannelArray, false);
            result.processRows(floorAdjust);
        }

        final RowFilter myFilter = new IdentifierSetRowFilter(validProbesetIds, idColumnIndex);
        final Table filtered = result.copy(myFilter);

        // 3. Transpose source.
        final DefineColumnFromRow columnHelper = new DefineColumnFromRow(idColumnIndex);
        Table transposed = filtered.transpose(columnHelper);

        // 4. Aggregate signal as average over each cluster, if clusters are provided.
        transposed = aggregateSignalByCluster(transposed, clusters);

        // 5. Scale values per column (one column=one probeset).
        final ScalingRowProcessor scalingOperation = new ScalingRowProcessor(transposed, IDENTIFIER_COLUMN_NAME);
        transposed.processRows(scalingOperation);   // accumulate scaling factors in first pass.
        scalingOperation.setFirstPass(false);
        transposed.processRows(scalingOperation);   // now apply scaling factors..
        finalResult.setFeatureTable(transposed);

        if (validProbesetIds.size() == 0) {
            // there is no overlap between this gene list and the chip. Stop this evaluation here.
            finalResult.setSvmResult(new EvaluationMeasure());
            return finalResult;
        }
        /* // 5b. Normalize values per row (now one row=one probeset).
                 RowProcessor normalizeOperation = new NormalizeRowProcessor(transposed, identifierColumnName);
                 transposed.processRows(normalizeOperation);
                  */
        EvaluationMeasure measure = null;
        SVMLightWriter svmLightWriter = null;
        LibSVMWriter libSVMWriter = null;
        if (svmLight) {

            // 6. Write SVM training set
            final String svmTrainingSetFileName = "forsvm.dat";
            final PrintWriter fileWriter = new PrintWriter(new FileWriter(new File(svmTrainingSetFileName)));
            svmLightWriter = new SVMLightWriter(transposed, "ID_REF", labelValueGroups, randomGenerator);
            svmLightWriter.write(transposed, fileWriter);

            measure = trainSVMLight(svmTrainingSetFileName);

            //   System.out.print(" " + measure + " ");
        } else { // libSVM:
            // Calculate SumOfSquares sum over all samples x.x:
            final SumOfSquaresCalculatorRowProcessor calculator =
                    new SumOfSquaresCalculatorRowProcessor(transposed, IDENTIFIER_COLUMN_NAME);
            transposed.processRows(calculator);
            // use the svmLight default C value, so that results are comparable:
            final double C = transposed.getRowNumber() / calculator.getSumOfSquares();

            libSVMWriter = new LibSVMWriter(transposed, "ID_REF", labelValueGroups, randomGenerator);
            libSVMWriter.setC(C);
            transposed.processRows(libSVMWriter);
            //     svm_model model = libSVMWriter.trainModel();
            //     double[] weights = LibSvmUtils.calculateWeights(model);

            //     TextIO.storeDoubles(weights, new PrintStream("BR-full-weights-libSvm.txt"));

            measure = libSVMWriter.leaveOneOutEvaluation();


        }

        finalResult.setSvmResult(measure);
        if (shuffling) {
            int countShuffleGE = 0;

            for (int i = 0; i < countShuffleLabelTests; i++) {
                EvaluationMeasure shuffleMeasure = null;
                if (svmLight) {
                    svmLightWriter.shuffleLabels();
                    final String svmTrainingSetFileName = "forsvm-shuffled.dat";
                    final PrintWriter fileWriter = new PrintWriter(new FileWriter(svmTrainingSetFileName));
                    svmLightWriter.write(transposed, fileWriter);
                    shuffleMeasure = trainSVMLight(svmTrainingSetFileName);
                } else {
                    libSVMWriter.shuffleLabels();
                    transposed.processRows(libSVMWriter);
                    shuffleMeasure = libSVMWriter.leaveOneOutEvaluation();
                }
                if (shuffleMeasure.getAccuracy() >= measure.getAccuracy()) {
                    countShuffleGE++;
                }
                System.err.print(".");
                System.err.flush();
            }
            System.err.println();
            System.err.flush();
            finalResult.setShufflingCountGE(countShuffleGE, countShuffleLabelTests);

        }
        return finalResult;
    }

    /**
     * Average signal over the transcripts in a cluster. Signals for Probesets not in a cluster are unchanged.
     *
     * @param table Microarray data, rows are probeset ids, columns are conditions.
     * @return
     */
    public static Table aggregateSignalByCluster(final Table table, final ObjectList<Cluster> clusters)
            throws TypeMismatchException, InvalidColumnException {
        final Table result = new ArrayTable();
        final AggregateClusters processor = new AggregateClusters();
//        processor.aggregate(table, result, clusters, IDENTIFIER_COLUMN_NAME);
        //      return result;
        return table;
    }

    /**
     * @param table
     * @return Indices of the columns that have been normalized.
     * @throws TypeMismatchException
     * @throws InvalidColumnException
     */
    public static int[] normalizeAcrossConditions(final Table table)
            throws TypeMismatchException, InvalidColumnException {
        final int[] columnSelection = getDoubleColumnIndices(table);

        // calculate the mean of each double column:
        final RowSumCalculator sumCalculator = new RowSumCalculator(columnSelection);
        final Int2DoubleMap means = new Int2DoubleOpenHashMap();

        table.processRows(sumCalculator);
        double minMean = Double.MAX_VALUE;
        double maxMean = Double.MAX_VALUE;
        for (final int columnIndex : columnSelection) {
            final double mean = sumCalculator.getSumForColumn(columnIndex) / (double) table.getRowNumber();
            means.put(columnIndex, mean);
            minMean = Math.min(minMean, mean);
            maxMean = Math.min(maxMean, mean);
            //    System.out.println("Column: "+columnIndex + " mean: "+mean);
        }
        // calculate how much each column must be offset to bring the mean of each column to the same value (maxMean)
        final Int2DoubleMap offsets = new Int2DoubleOpenHashMap();
        for (final int columnIndex : columnSelection) {
            final double mean = means.get(columnIndex);
            final double offset = maxMean - mean;
            offsets.put(columnIndex, offset);
        }
        final RowOffsetCalculator offsetRowValuesCalculator = new RowOffsetCalculator(columnSelection, offsets);
        table.processRows(offsetRowValuesCalculator);
        return columnSelection;
    }

    public static int[] getDoubleColumnIndices(final Table table) {
        // select all columns of type double:
        int num = 0;
        for (int i = 1; i < table.getColumnNumber(); i++) {
            if (table.getType(i) == double.class) {
                num++;
            }
        }

        final int[] columnSelection = new int[num];
        int j = 0;
        for (int i = 1; i < table.getColumnNumber(); i++) {
            if (table.getType(i) == double.class) {
                columnSelection[j++] = i;
            }
        }
        return columnSelection;
    }

    private EvaluationMeasure trainSVMLight(final String svmTrainingSetFileName) throws IOException {
        EvaluationMeasure measure = new EvaluationMeasure();
        // 6. Train SVM
        final SVMLightDriver svm = new SVMLightDriver();
        svm.setTimeout(timeout);
        try {
            measure = svm.run(svmTrainingSetFileName);
        } catch (IllegalStateException e) {
            System.out.println("\nsvmlight does not seem to be installed properly." + e);
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("\nsvmlight was interupted abnormally." + e);
            System.exit(1);
        }
        return measure;
    }

    public static Table filterColumnsForTask(final Table source, final List<Set<String>> labelValueGroups,
                                             final String identifierColumnName, Set<String> reduction)
            throws TypeMismatchException, InvalidColumnException {
        final Table result = source.copy();

        // 1. filter out columns were label is not defined.

        final Set<String> keepSet = new HashSet<String>();
        for (final Set<String> labelGroup : labelValueGroups) { // if label value is in a label group, keep this column
            keepSet.addAll(labelGroup);
        }

        keepSet.add(identifierColumnName);
        // keep only those samples also in reduction:
        reduction.add(identifierColumnName);
        keepSet.retainAll(reduction);

        filterColumns(result, keepSet);
        return result;
    }

    /**
     * Calculate the sets of sample ids that correspond to each classification label. For instance, assume tumor-non tumor
     * are the classification labels, this method will return the set of samples that are marked with each label.
     *
     * @param classificationTask
     * @return
     */
    public static List<Set<String>> calculateLabelValueGroups(final ClassificationTask classificationTask) {
        return classificationTask.calculateLabelValueGroups();
    }

    public static void filterColumns(final Table result, final Set<String> keepSet) {
        final Vector<String> toRemove = new Vector<String>();
        for (int i = 0; i < result.getColumnNumber(); i++) {

            final String identifier = result.getIdentifier(i);
            if (!keepSet.contains(identifier)) {
                toRemove.add(identifier);
            }
        }
        for (final String columnIdf : toRemove) {
            try {
                result.removeColumn(columnIdf);
            } catch (InvalidColumnException e) {
                e.printStackTrace();
                assert false : e;
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

