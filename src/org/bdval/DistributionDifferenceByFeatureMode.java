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

package org.bdval;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import org.bdval.signalquality.BaseSignalQualityCalculator;
import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.RecursiveFileListIterator;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import edu.cornell.med.icb.tools.geo.ClassificationTask;
import edu.cornell.med.icb.tools.geo.ConditionIdentifiers;
import edu.cornell.med.icb.util.ICBStringUtils;
import edu.cornell.med.icb.util.ProcessEstimator;
import edu.mssm.crover.tables.ColumnTypeException;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import edu.mssm.crover.tables.readers.SyntaxErrorException;
import edu.mssm.crover.tables.readers.UnsupportedFormatException;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compare the distribution of each feature used in a set specific of biomarker models. Distribution differences are quantified for
 * feature signal between two sample sets (i.e., training set vs. validation set). A P-value (Kolmogorov-smirnov)
 * and ratio of rank statistics is evaluated for each feature in each model processed.
 *
 * @author Kevin Dorff
 */
public class DistributionDifferenceByFeatureMode extends DAVMode {

    /**
     * The logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(DistributionDifferenceByFeatureMode.class);

    /**
     * Map of datsetName to dataset details.
     */
    private Map<String, Map<String, String>> datasetName2DetailsMap;

    /**
     * The signal quality calculator object.
     */
    private BaseSignalQualityCalculator signalQualityCalcObj;

    /**
     * Map of model id's to model filename prefixes.
     */
    private Map<String, String> modelIdToModelPrefixMap;

    /**
     * Map of model id's to model conditions.
     */
    private Map<String, Map<String, String>> modelIdToModelConditionsMap;

    /**
     * Map of datsetName to dataset details.
     */
    private String evalDatasetRoot;

    /**
     * The label used to denote training values in the properties file.
     */
    private String propertiesTrainingLabel;

    /**
     * The label used to denote validation values in the properties file.
     */
    private String propertiesValidationLabel;

    /**
     * Cache tables by filename in memory.
     */
    private Map<String, Table> tableCache = new Object2ObjectOpenHashMap<String, Table>();

    /**
     * Models to exclude from processing.
     */
    private Set<String> excludeModelSet;

    /**
     * True if we should write extended output.
     */
    private boolean extendedOutput;

    /**
     * If true classes will be merged, otherwise the classes will be written separately.
     */
    private boolean mergeClasses = false;

    /**
     * The maximum number of classes, important for the output file header to be correct.
     */
    private int maxNumClasses;

    /**
     * Cache of the filenames to ClassificationTask maps.
     */
    private Map<String, ClassificationTask> filenamesToClassificationTaskMap =
            new Object2ObjectOpenHashMap<String, ClassificationTask>();

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        // No input file using this flag
        jsap.getByID("input").addDefault("N/A");
        // there is no need for task definitions.
        jsap.getByID("task-list").addDefault("N/A");
        // No need for platform-filenames
        jsap.getByID("platform-filenames").addDefault("N/A");
        // there is no need for condition ids.
        jsap.getByID("conditions").addDefault("N/A");

        final Parameter maqciiPropertiesFileOption = new FlaggedOption("maqcii-properties-file")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setLongFlag("maqcii-properties-file")
                .setHelp("The maqcii properties file such as 'maqcii-c.properties'.");
        jsap.registerParameter(maqciiPropertiesFileOption);

        final Parameter modelConditionsFileOption = new FlaggedOption("model-conditions-file")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setLongFlag("model-conditions-file")
                .setHelp("The model-conditions-file such as 'model-conditions.txt'.");
        jsap.registerParameter(modelConditionsFileOption);

        final Parameter modelsDirOption = new FlaggedOption("models-dir")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setLongFlag("models-dir")
                .setHelp("The directory containing models (may be within sub-directories).");
        jsap.registerParameter(modelsDirOption);

        final Parameter modelsListOption = new FlaggedOption("model-list")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault("all")
                .setRequired(false)
                .setLongFlag("model-list")
                .setHelp("The models to process (or 'all' to process all models). "
                        + "Comma separated, such as 'DUDTR,YTNJM'.");
        jsap.registerParameter(modelsListOption);

        final Parameter modelExcludeListOption = new FlaggedOption("model-exclude-list")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault("none")
                .setRequired(false)
                .setLongFlag("model-exclude-list")
                .setHelp("The models to NOT process (or 'none' to process all models). "
                        + "Comma separated, such as 'DUDTR,YTNJM'.");
        jsap.registerParameter(modelExcludeListOption);

        final Parameter signalQualityCalcClassOption =
                new FlaggedOption("signal-quality-calc-class")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setLongFlag("signal-quality-calc-class")
                        .setHelp("Fully qualified classname for an "
                                + "AbstractSignalQualityCalculator class.");
        jsap.registerParameter(signalQualityCalcClassOption);

        final Parameter evalDatasetRootOption =
                new FlaggedOption("eval-dataset-root")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault("-")
                        .setRequired(false)
                        .setLongFlag("eval-dataset-root")
                        .setHelp("The eval-dataset-root directory or specify '-' to use "
                                + "the dataset-root directory specified "
                                + "in the model-conditions file");
        jsap.registerParameter(evalDatasetRootOption);

        final Parameter propertiesTrainingLabelOption =
                new FlaggedOption("properties-training-label")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault("training")
                        .setRequired(false)
                        .setLongFlag("properties-training-label")
                        .setHelp("The label used to denote training "
                                + "values in the properties file.");
        jsap.registerParameter(propertiesTrainingLabelOption);

        final Parameter propertiesValidationLabelOption =
                new FlaggedOption("properties-validation-label")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault("validation")
                        .setRequired(false)
                        .setLongFlag("properties-validation-label")
                        .setHelp("The label used to denote validation "
                                + "values in the properties file.");
        jsap.registerParameter(propertiesValidationLabelOption);

        final Parameter extendedOutputOption =
                new FlaggedOption("extended-output")
                        .setStringParser(JSAP.BOOLEAN_PARSER)
                        .setDefault("false")
                        .setRequired(true)
                        .setLongFlag("extended-output")
                        .setHelp("If true, extra output will be included.");
        jsap.registerParameter(extendedOutputOption);

        final Parameter mergeClassesOption =
                new FlaggedOption("merge-classes")
                        .setStringParser(JSAP.BOOLEAN_PARSER)
                        .setDefault("false")
                        .setRequired(true)
                        .setLongFlag("merge-classes")
                        .setHelp("If true, all classes will be merged.");
        jsap.registerParameter(mergeClassesOption);

        final Parameter maxNumClassesOption =
                new FlaggedOption("max-num-classes")
                        .setStringParser(JSAP.INTEGER_PARSER)
                        .setDefault("2")
                        .setRequired(true)
                        .setLongFlag("max-num-classes")
                        .setHelp("The maximum number of classes (for the output file header)");
        jsap.registerParameter(maxNumClassesOption);
    }

    /**
     * Interpret the command line arguments.
     *
     * @param jsap    the JSAP command line parser
     * @param result  the results of command line parsing
     * @param options the DAVOptions
     */
    @Override
    public void interpretArguments(
            final JSAP jsap, final JSAPResult result, final DAVOptions options) {
        checkArgumentsSound(jsap, result, false);
        setupPathwayOptions(result, options);
        setupRservePort(result, options);
        setupClassifier(result, options);
        setExceptionOnCheckPostFilteringFail(true);

        final String maqciiPropertiesFile = verifyFilenameOption(result, "maqcii-properties-file");
        final String modelConditionsFile = verifyFilenameOption(result, "model-conditions-file");
        final String modelsDir = verifyDirectoryOption(result, "models-dir");
        final String keepModelSetStr = result.getString("model-list");
        final String excludeModelSetStr = result.getString("model-exclude-list");
        extendedOutput = result.getBoolean("extended-output");
        mergeClasses = result.getBoolean("merge-classes");
        maxNumClasses = result.getInt("max-num-classes");
        if (mergeClasses) {
            maxNumClasses = 1;
        }

        propertiesTrainingLabel = result.getString("properties-training-label");
        propertiesValidationLabel = result.getString("properties-validation-label");

        evalDatasetRoot = result.getString("eval-dataset-root");
        if (!evalDatasetRoot.equals("-")) {
            if (!isValidDirectory(evalDatasetRoot)) {
                LOG.fatal("eval-dataset-root must either be '-' or a valid directory");
                System.exit(10);
            }
        } else {
            evalDatasetRoot = null;
        }

        LOG.info("Creating the signal quality calculator...");
        signalQualityCalcObj = createCalculator(result);
        if (signalQualityCalcObj == null) {
            System.exit(10);
        }

        // Populate keepModelSet (empty set for all)
        Set<String> keepModelSet = modelList(keepModelSetStr, "all");
        excludeModelSet = modelList(excludeModelSetStr, "none");

        // Only retain model conditions in keepModelSet
        LOG.info("Reading the model conditions file...");
        modelIdToModelConditionsMap = readModelConditionsFile(modelConditionsFile, keepModelSet);
        LOG.info(String.format("... found model conditions for %d models",
                modelIdToModelConditionsMap.keySet().size()));

        // Only save model prefixes that we have model conditions for
        keepModelSet = modelIdToModelConditionsMap.keySet();
        if (keepModelSet.size() == 0) {
            LOG.fatal("No models to load");
            System.exit(10);
        }
        LOG.info("Scanning the models directory for models to keep...");
        modelIdToModelPrefixMap = scanModelsDirectory(modelsDir, keepModelSet);
        if (modelIdToModelPrefixMap.keySet().size() == 0) {
            LOG.fatal("No models to loaded.");
            System.exit(10);
        }
        keepModelSet = modelIdToModelPrefixMap.keySet();

        //Reduce model conditions to the models we actually have
        reduceMap(modelIdToModelConditionsMap, keepModelSet);

        LOG.info(String.format(
                "... Finished scanning models directory. Found %d models.",
                modelIdToModelPrefixMap.keySet().size()));
        final Set<String> datasetNames =
                extractDatasetNamesFromModelConditions(modelIdToModelConditionsMap);
        LOG.info(String.format(
                "Models exist in %d datasets", datasetNames.size()));

        LOG.debug(String.format("modelIdToModelConditionsMap[%d]%s=",
                modelIdToModelConditionsMap.keySet().size(),
                ArrayUtils.toString(modelIdToModelConditionsMap.keySet())));
        LOG.debug(String.format("modelIdToModelPrefixMap[%d]=%s",
                modelIdToModelPrefixMap.keySet().size(),
                ArrayUtils.toString(modelIdToModelPrefixMap.keySet())));
        LOG.info(String.format("datasetNames[%d]=%s",
                datasetNames.size(),
                ArrayUtils.toString(datasetNames)));

        LOG.info("Reading the maqcii properties file...");
        datasetName2DetailsMap = readMaqciiProperties(maqciiPropertiesFile, datasetNames);
        if (datasetName2DetailsMap == null) {
            System.exit(10);
        }

        LOG.debug(String.format("datasetName2DetailsMap[%d]=%s",
                datasetName2DetailsMap.keySet().size(),
                ArrayUtils.toString(datasetName2DetailsMap)));
    }

    /**
     * Take a string and make a model list from it (string should be commans separated).
     *
     * @param modelListStr the list of models, comma separated
     * @param allValue     which string should mark all/none (denote this should return
     *                     an empty set.
     * @return the set
     */
    public Set<String> modelList(final String modelListStr, final String allValue) {
        final Set<String> resultSet = new ObjectLinkedOpenHashSet<String>();
        if (!StringUtils.isBlank(modelListStr) && !modelListStr.equals(allValue)) {
            final String[] parts = StringUtils.split(modelListStr, ',');
            for (final String part : parts) {
                resultSet.add(part.trim());
            }
        }
        return resultSet;
    }

    /**
     * Reduce a map, keeping only the values specified in keepKeySet.
     *
     * @param map        the map to reduce
     * @param keepKeySet the set of keys to keep.
     */
    public static void reduceMap(final Map<String, ?> map, final Set<String> keepKeySet) {
        final List<String> reduceKeys = new LinkedList<String>();
        for (final String key : map.keySet()) {
            if (!keepKeySet.contains(key)) {
                reduceKeys.add(key);
            }
        }
        for (final String key : reduceKeys) {
            map.remove(key);
        }
    }

    /**
     * Scan the given model directory for model files (files that end in .model).
     * and return a map of model-id's to model filename prefixes. Only model-ids which
     * exist in keepModelIdsSet will be saved into the Map. If keepModelIdsSet is empty
     * or null, ALL models will be retrieved. This will also scan all subdirectories.
     *
     * @param modelDirName    the directory that contains the model files
     * @param keepModelIdsSet the set of model ids to keep (or keep ALL if keepModelIdsSet
     *                        is empty or null)
     * @return the map of model-id's to model filename prefixes
     */
    public static Map<String, String> scanModelsDirectory(
            final String modelDirName, final Set<String> keepModelIdsSet) {
        // Get the list of model prefixes
        final Map<String, String> modelsMap = new Object2ObjectLinkedOpenHashMap<String, String>();
        int numFilesScanned = 0;
        int numModelsFound = 0;
        for (final File file : new RecursiveFileListIterator(new File(modelDirName))) {
            final String candidateFile = getFilename(file);
            if (candidateFile == null) {
                LOG.fatal("Could not determine full path filename for file " + file.toString());
                System.exit(10);
            }
            if (candidateFile.endsWith(".model")) {
                final String modelFilenamePrefix = BDVModel.removeSuffix(candidateFile, ".model");
                final String modelId = modelIdFromPrefix(modelFilenamePrefix);
                if (keepModelIdsSet == null || keepModelIdsSet.size() == 0
                        || keepModelIdsSet.contains(modelId)) {
                    modelsMap.put(modelId, modelFilenamePrefix);
                    numModelsFound++;
                }
            }
            numFilesScanned++;
            if (numFilesScanned % 5000 == 0) {
                LOG.debug(String.format("... Looked at %d files, found %d models",
                        numFilesScanned, numModelsFound));
            }
        }
        return modelsMap;
    }

    /**
     * Given a file, return the filename. If an IOException is raised during
     * getCanonicalPath() it is swallowed and null is returned.
     *
     * @param file File to get the name of
     * @return the full path filename
     */
    public static String getFilename(final File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Verify that a specified filename exists or fail out.
     *
     * @param result    the JSAPResults
     * @param optionKey the key that should contain an existing file
     * @return the filename
     */
    final String verifyFilenameOption(final JSAPResult result, final String optionKey) {
        final String filename = result.getString(optionKey);
        final File file = new File(filename);
        if (!file.exists()) {
            LOG.fatal("Required file named " + filename + " does not exist.");
            System.exit(10);
        }
        if (!file.isFile()) {
            LOG.fatal("Required file named " + filename + " is not a file.");
            System.exit(10);
        }
        if (file.isDirectory()) {
            LOG.fatal("Required file named " + filename + " is not a file.");
            System.exit(10);
        }
        if (!file.canRead()) {
            LOG.fatal("Required file named " + filename + " is not readable.");
            System.exit(10);
        }
        return filename;
    }

    /**
     * Verify that a specified filename exists or fail out.
     *
     * @param result    the JSAPResults
     * @param optionKey the key that should contain an existing file
     * @return the filename
     */
    final String verifyDirectoryOption(final JSAPResult result, final String optionKey) {
        final String dirName = result.getString(optionKey);
        if (!isValidDirectory(dirName)) {
            System.exit(10);
        }
        return dirName;
    }

    public static boolean isValidDirectory(final String dirName) {
        final File file = new File(dirName);
        if (!file.exists()) {
            LOG.fatal("Required directory named " + dirName + " does not exist.");
            return false;
        }
        if (file.isFile()) {
            LOG.fatal("Required directory named " + dirName + " is not a directory.");
            return false;
        }
        if (!file.isDirectory()) {
            LOG.fatal("Required directory named " + dirName + " is not a directory.");
            return false;
        }
        if (!file.canRead()) {
            LOG.fatal("Required directory named " + dirName + " is not readable.");
            return false;
        }
        return true;
    }

    /**
     * Create the calculator object specified by the command line argument
     * signal-quality-calc-class
     *
     * @param result the JSAPResult object
     * @return the newly created and configured calculator object or null if it
     *         couldn't be created.
     */
    final BaseSignalQualityCalculator createCalculator(final JSAPResult result) {
        final String classname = result.getString("signal-quality-calc-class");
        try {
            final BaseSignalQualityCalculator calculator =
                    (BaseSignalQualityCalculator) Class.forName(classname).newInstance();
            calculator.configure(result,
                    BaseSignalQualityCalculator.OutputFileHeader.P_VALUES,
                    extendedOutput, maxNumClasses);
            return calculator;
        } catch (InstantiationException e) {
            LOG.fatal("Error creating ISignalQualityCalculator object", e);
        } catch (IllegalAccessException e) {
            LOG.fatal("Error creating ISignalQualityCalculator object", e);
        } catch (ClassNotFoundException e) {
            LOG.fatal("Error creating ISignalQualityCalculator object", e);
        } catch (FileNotFoundException e) {
            LOG.fatal("Error creating output file", e);
        }
        return null;
    }

    /**
     * The SingleQualityMode D&V mode.
     *
     * @param options program options.
     */
    @Override
    public void process(final DAVOptions options) {
        super.process(options);

        final ProcessEstimator estimator = new ProcessEstimator(modelIdToModelPrefixMap.size());
        for (final String modelId : modelIdToModelPrefixMap.keySet()) {
            if (excludeModelSet.contains(modelId)) {
                estimator.unitCompleted();
                continue;
            }
            final String modelFilenamePrefix = modelIdToModelPrefixMap.get(modelId);
            final Map<String, String> modelConditionsMap = modelIdToModelConditionsMap.get(modelId);
            final String datasetName = modelConditionsMap.get("dataset-name");
            final String datasetRoot;
            if (evalDatasetRoot == null) {
                datasetRoot = modelConditionsMap.get("dataset-root");
            } else {
                datasetRoot = evalDatasetRoot;
            }

            final Map<String, String> datasetDetailsMap =
                    localizeDatasetDetailsMap(datasetName2DetailsMap.get(datasetName), datasetRoot);

            try {
                loadFilesAndCalculateQuality(
                        options, modelId, modelFilenamePrefix, datasetDetailsMap);
            } catch (IllegalArgumentException e) {
                signalQualityCalcObj.writeData("# " + modelId + " error loading table for model: "
                        + e.getMessage());
            } catch (IOException e) {
                signalQualityCalcObj.writeData("# " + modelId + " error reading file: "
                        + e.getMessage());
            } finally {
                // Work completed, estimate time remaining
                final long estimate = estimator.unitCompleted();
                if (estimate == Long.MAX_VALUE) {
                    System.out.println("## Waiting for second data point to estimate time");
                } else {
                    final long finishAt = (new Date().getTime()) + estimate;
                    System.out.printf(
                            "## Processed model %d of %d, Time remaining %s, finish at %s%n",
                            estimator.getUnitsCompleted(), estimator.getTotalUnits(),
                            ICBStringUtils.millis2hms(estimate),
                            DateFormatUtils.format(finishAt, "HH:mm:ss"));
                }
            }
        }

        /** Run for a single model. */
        /*
        */

        signalQualityCalcObj.close();
    }

    /**
     * Perform a signal quality assessment on a single set of files (one model).
     *
     * @param options             the options to run with
     * @param modelId             the model id being processed
     * @param modelFilenamePrefix the model filename prefix
     * @param datasetDetailsMap   the map of dataset details (filenames, etc.)
     * @throws IOException error reading or writing
     */
    public void loadFilesAndCalculateQuality(
            final DAVOptions options,
            final String modelId, final String modelFilenamePrefix,
            final Map<String, String> datasetDetailsMap) throws IOException {

        final String trainingDatasetFilename = datasetDetailsMap.get("training.dataset-file");
        final String validationDatasetFilename = datasetDetailsMap.get("validation.dataset-file");
        final String trainingSamplesFilename = datasetDetailsMap.get("training.test-samples");
        final String validationSamplesFilename = datasetDetailsMap.get("validation.test-samples");
        final String trainingTrueLabelsFilename = datasetDetailsMap.get("training.true-labels");
        final String validationTrueLabelsFilename = datasetDetailsMap.get("validation.true-labels");
        final String tasksFilename = datasetDetailsMap.get("tasks-file");

        LOG.info("Running loadFilesAndCalculateQuality for:");
        LOG.info("  modelId=" + modelId);
        LOG.info("  modelFilenamePrefix=" + modelFilenamePrefix);
        LOG.info("  trainingDatasetFilename=" + trainingDatasetFilename);
        LOG.info("  validationDatasetFilename=" + validationDatasetFilename);
        LOG.info("  trainingSamplesFilename=" + trainingSamplesFilename);
        LOG.info("  validationSamplesFilename=" + validationSamplesFilename);
        LOG.info("  trainingTrueLabelsFilename=" + trainingTrueLabelsFilename);
        LOG.info("  validationTrueLabelsFilename=" + validationTrueLabelsFilename);
        LOG.info("  tasksFilename=" + tasksFilename);

        // Load the tasks and CIDs files using the official way...
        final ClassificationTask tClassTasks = loadCachedTaskAndConditions(
                tasksFilename, trainingTrueLabelsFilename);
        final ClassificationTask vClassTasks = loadCachedTaskAndConditions(
                tasksFilename, validationTrueLabelsFilename);

        ConditionIdentifiers tConditionIdentifiers = tClassTasks.getConditionsIdentifiers();
        ConditionIdentifiers vConditionIdentifiers = vClassTasks.getConditionsIdentifiers();

        final String[] allClasses = tClassTasks.getConditionNames();
        final String[] vClasses = vClassTasks.getConditionNames();
        assert Arrays.equals(allClasses, vClasses);

        final StringBuilder classMapcomment = new StringBuilder();
        if (mergeClasses) {
            classMapcomment.append(String.format("## All classes merge to %s",
                    BaseSignalQualityCalculator.CLASS_TRANSLATION[0]));
        } else {
            for (int i = 0; i < allClasses.length; i++) {
                if (classMapcomment.length() > 0) {
                    classMapcomment.append('\n');
                }
                classMapcomment.append(String.format("## Class %s becomes %s", allClasses[i],
                        BaseSignalQualityCalculator.CLASS_TRANSLATION[i]));
            }
        }
        signalQualityCalcObj.setClassMapComment(classMapcomment.toString());

        final ObjectSet<String> trainingSampleIds = loadSampleIds(trainingSamplesFilename);
        final ObjectSet<String> validationSampleIds = loadSampleIds(validationSamplesFilename);

        try {
            final BDVModel model = new BDVModel(modelFilenamePrefix);

            final boolean scaleFeaturesFromCommandLine = options.scaleFeatures;
            model.load(options);
            // Force scaleFeature to respect the command line option (default is true)
            options.scaleFeatures = scaleFeaturesFromCommandLine;

            assert model.getGeneList() != null : " gene list must not be null";

            final List<Set<String>> trainingLabelValueGroups = new ArrayList<Set<String>>();
            options.inputTable = readMemoryCachedInputFile(trainingDatasetFilename);
            final Table trainingTable = model.loadTestSet(this, options,
                    model.getGeneList(), trainingLabelValueGroups, trainingSampleIds);
            final int trainingFilteredNumberOfSamples = trainingTable.getRowNumber();
            LOG.info("Training dataset has " + trainingFilteredNumberOfSamples + " samples.");

            if (trainingFilteredNumberOfSamples != trainingSampleIds.size()) {
                signalQualityCalcObj.writeData(
                        String.format("# error with model-id=%s - number of samples "
                                + "doesn't match. trainingTable has %d but trainingSampleIds "
                                + "has %d", modelId, trainingFilteredNumberOfSamples,
                                trainingSampleIds.size()));
                return;
            }

            final List<Set<String>> validationLabelValueGroups = new ArrayList<Set<String>>();
            options.inputTable = readMemoryCachedInputFile(validationDatasetFilename);
            final Table validationTable = model.loadTestSet(this, options,
                    model.getGeneList(), validationLabelValueGroups, validationSampleIds);
            final int validationFilteredNumberOfSamples = validationTable.getRowNumber();
            LOG.info("Validation dataset has " + validationFilteredNumberOfSamples + " samples.");

            if (validationFilteredNumberOfSamples != validationSampleIds.size()) {
                signalQualityCalcObj.writeData(
                        String.format("# error with model-id=%s - number of samples "
                                + "doesn't match. validationTable has %d but validationSampleIds "
                                + "has %d", modelId, validationFilteredNumberOfSamples,
                                validationSampleIds.size()));
                return;
            }


            final Map<String, Map<String, double[]>> classToDataMapMap =
                    new Object2ObjectOpenHashMap<String, Map<String, double[]>>();

            System.out.printf("There are %d classes, %s%n",
                    allClasses.length, ArrayUtils.toString(allClasses));
            if (mergeClasses) {
                classToDataMapMap.put("merged-training", retrieveDataAsMap(
                        trainingTable, tConditionIdentifiers, null));
                System.out.println("Loading filtered validation data");
                classToDataMapMap.put("merged-validation", retrieveDataAsMap(
                        validationTable, vConditionIdentifiers, null));

                System.out.printf("Loaded data for model=%s merged classes%n", modelId);

                signalQualityCalcObj.calculatePValues(model, modelId,
                        new String[]{"merged"}, classToDataMapMap);
            } else {
                for (final String classId : allClasses) {
                    // For each CLASS
                    classToDataMapMap.put(classId + "-training", retrieveDataAsMap(
                            trainingTable, tConditionIdentifiers, classId));
                    classToDataMapMap.put(classId + "-validation", retrieveDataAsMap(
                            validationTable, vConditionIdentifiers, classId));

                    System.out.printf("Loaded data for model=%s/class=%s%n", modelId, classId);
                }
                signalQualityCalcObj.calculatePValues(
                        model, modelId, allClasses, classToDataMapMap);
            }

        } catch (IOException e) {
            LOG.error("Error loading model " + modelFilenamePrefix, e);
            System.exit(10);
        } catch (ClassNotFoundException e) {
            LOG.fatal("Error loading model " + modelFilenamePrefix, e);
            System.exit(10);
        } catch (ColumnTypeException e) {
            LOG.fatal("Error processing input file ", e);
            System.exit(10);
        } catch (TypeMismatchException e) {
            LOG.fatal("Error processing input file ", e);
            System.exit(10);
        } catch (InvalidColumnException e) {
            LOG.fatal("Error processing input file ", e);
            System.exit(10);
        } catch (SyntaxErrorException e) {
            LOG.fatal("Error reading dataset file ", e);
            System.exit(10);
        } catch (UnsupportedFormatException e) {
            LOG.fatal("Error reading dataset file ", e);
            System.exit(10);
        }
    }

    /**
     * Load / cache the ClassificationTask for the specified task file and cids file.
     *
     * @param tasksFilename      the task file
     * @param trueLabelsFilename the cids file with the true labels
     * @return a single ClassificationTask object that contains information about
     *         the tasks and sample ids and labels, etc.
     * @throws IOException error reading either the tasks file or the cids file
     */
    private synchronized ClassificationTask loadCachedTaskAndConditions(
            final String tasksFilename, final String trueLabelsFilename) throws IOException {
        final String key = tasksFilename + trueLabelsFilename;
        ClassificationTask classTask = filenamesToClassificationTaskMap.get(key);
        if (classTask == null) {
            final ClassificationTask[] classTaskArray =
                    ClassificationTask.parseTaskAndConditions(tasksFilename, trueLabelsFilename);
            assert classTaskArray.length == 1;
            classTask = classTaskArray[0];
            filenamesToClassificationTaskMap.put(key, classTask);
        }
        return classTask;
    }

    /**
     * Read a table by filename. Cache them in memory. On future reads, get it from the cache.
     *
     * @param fileName Name of the file to read
     * @return A table that contains data read from the input file
     * @throws SyntaxErrorException       if there is an error in the file
     * @throws IOException                if the input file cannot be read
     * @throws UnsupportedFormatException if the file format is not recognized
     */
    private Table readMemoryCachedInputFile(final String fileName) throws
            IOException, SyntaxErrorException, UnsupportedFormatException {
        Table table = tableCache.get(fileName);
        if (table == null) {
            table = readInputFile(fileName);
            if (table != null) {
                tableCache.put(fileName, table);
            }
        }
        return table;
    }

    /**
     * Given a model filename prefix, return the model id (which is after the last
     * "-" in the string).
     *
     * @param modelFilenamePrefix the model filename prefix
     * @return the model id
     */
    private static String modelIdFromPrefix(final String modelFilenamePrefix) {
        final String[] parts = StringUtils.split(modelFilenamePrefix, '-');
        return parts[parts.length - 1];
    }

    /**
     * Load sample id's from a given file.
     *
     * @param sampleFilename the sample id's filename
     * @return the sample id's set
     * @throws IOException error reading file
     */
    public static ObjectSet<String> loadSampleIds(final String sampleFilename) throws IOException {
        if (sampleFilename != null) {
            LOG.info("Reading test sample filename: " + sampleFilename);

            final ObjectSet<String> sampleIds = new ObjectOpenHashSet<String>();

            for (final String line : new TextFileLineIterator(sampleFilename)) {
                final String sampleId = line.trim();
                sampleIds.add(sampleId);
            }
            return sampleIds;
        }
        return null;
    }

    /**
     * Retrieve the data from the table into Map[column_name, double[]].
     *
     * @param table                the input Table which contains the data
     * @param conditionIdentifiers maps sample ids to their class
     * @param classToKeep          data class to read values for, if mergeClasses is true this will
     *                             be ignored
     *                             all classes (mergedClasses)
     * @return data in map form
     */
    private Map<String, double[]> retrieveDataAsMap(
            final Table table, final ConditionIdentifiers conditionIdentifiers,
            final String classToKeep) {
        final Map<String, double[]> data = new Object2ObjectLinkedOpenHashMap<String, double[]>();
        boolean[] keepRowValues = ArrayUtils.EMPTY_BOOLEAN_ARRAY;
        final DoubleList keptValues = new DoubleArrayList();
        int numKeptValues = 0;
        for (int col = 0; col < table.getColumnNumber(); col++) {
            final String colName = table.getIdentifier(col);
            try {
                if (col == 0) {
                    // Column 0 will determine which values we keep
                    final String[] samplesIds = table.getStrings(colName);
                    final int numRows = samplesIds.length;
                    keepRowValues = new boolean[numRows];
                    for (int i = 0; i < numRows; i++) {
                        final String sampleId = samplesIds[i];

                        // Obtain class for the sample in two ways, compare them.
                        final String sampleTrueLabel = conditionIdentifiers.conditionForIdentifier(sampleId);

                        if (sampleTrueLabel == null) {
                            // NEVER keep if the sampleId isn't in the true-labels file
                            keepRowValues[i] = false;
                        } else {
                            if (mergeClasses || sampleTrueLabel.equals(classToKeep)) {
                                keepRowValues[i] = true;
                            }
                            if (keepRowValues[i]) {
                                numKeptValues++;
                            }
                        }
                    }
                } else {
                    keptValues.clear();
                    if (numKeptValues > 0) {
                        final double[] allValues = table.getDoubles(colName);
                        for (int i = 0; i < keepRowValues.length; i++) {
                            if (keepRowValues[i]) {
                                keptValues.add(allValues[i]);
                            }
                        }
                    }
                    data.put(table.getIdentifier(col), keptValues.toDoubleArray());
                }
            } catch (InvalidColumnException e) {
                LOG.error(e);
            }
        }
        return data;
    }

    /**
     * Read the model conditions file into a map of model id's to model conditions values.
     * This can handle both the normal version ("model-conditions.txt") or the columns version
     * ("model-conditions-columns.txt"). It can detect the difference as the first line of the
     * columns version ("model-conditions-columns.txt") starts with "model-id\t".
     *
     * @param modelConditionsFilename the model conditions file
     * @param keepModelIdsSet         the set of model id's to keep. If this set is null or empty
     *                                ALL model conditions will be read
     * @return the map of model id's to model conditions values
     */
    public static Map<String, Map<String, String>> readModelConditionsFile(
            final String modelConditionsFilename, final Set<String> keepModelIdsSet) {
        final File modelConditionsFile = new File(modelConditionsFilename);
        final Map<String, Map<String, String>> modelIdToModelConditionsMap =
                new Object2ObjectLinkedOpenHashMap<String, Map<String, String>>();
        if (!modelConditionsFile.exists()) {
            LOG.fatal("model conditions file " + modelConditionsFilename + " does not exist");
            return null;
        }
        if (!modelConditionsFile.isFile()) {
            LOG.fatal("model conditions file " + modelConditionsFilename + " is not a file");
            return null;
        }
        if (!modelConditionsFile.canRead()) {
            LOG.fatal("model conditions file " + modelConditionsFilename + " unreadable");
            return null;
        }

        try {
            int lineNo = 0;
            Boolean columnsMode = null;
            TsvToFromMap tsvToFromMap = null;
            for (final String line : new TextFileLineIterator(modelConditionsFile)) {
                if (columnsMode == null) {
                    columnsMode = line.startsWith("model-id\t");
                }

                final Map<String, String> modelValues;
                if (columnsMode) {
                    if (tsvToFromMap == null) {
                        tsvToFromMap = TsvToFromMap.createFromTsvFile(modelConditionsFile);
                    }
                    if (lineNo++ == 0 || StringUtils.isBlank(line) || line.startsWith("#")) {
                        continue;
                    }
                    modelValues = tsvToFromMap.readDataToMap(line);
                } else {
                    lineNo++;
                    if (StringUtils.isBlank(line) || line.startsWith("#")) {
                        continue;
                    }
                    modelValues = new Object2ObjectLinkedOpenHashMap<String, String>();
                    final String[] params = StringUtils.split(line, '\t');
                    for (final String param : params) {
                        final String[] parts = StringUtils.split(param, '=');
                        if (parts.length == 2) {
                            modelValues.put(parts[0], parts[1]);
                        } else {
                           // the value may contain a '=' for instance cache-dir=fs=true
                            final MutableString value = new MutableString();
                            for (int i = 1; i < parts.length; i++) {
                                value.append(parts[i]);
                            }
                            modelValues.put(parts[0], value.toString());

                        }
                    }
                }
                if (modelValues == null) {
                    continue;
                }
                final String modelId = modelValues.get("model-id");
                if (modelId == null || modelId.equals("N/A")) {
                    continue;
                }
                if (keepModelIdsSet == null
                        || keepModelIdsSet.size() == 0
                        || keepModelIdsSet.contains(modelId)) {

                    modelIdToModelConditionsMap.put(modelId, modelValues);
                }
            }
            return modelIdToModelConditionsMap;
        } catch (IOException e) {
            LOG.fatal("Error reading model conditions file " + modelConditionsFilename, e);
            return null;
        }
    }

    /**
     * Take the map of model id's to model conditions and extract all of the
     * dataset names.
     *
     * @param modelIdToModelConditionsMap the map of model id's to model conditions
     * @return the set of dataset names
     */
    private static Set<String> extractDatasetNamesFromModelConditions(
            final Map<String, Map<String, String>> modelIdToModelConditionsMap) {
        final Set<String> datasetNames = new ObjectLinkedOpenHashSet<String>();
        for (final String modelId : modelIdToModelConditionsMap.keySet()) {
            final Map<String, String> params = modelIdToModelConditionsMap.get(modelId);
            final String datasetName = params.get("dataset-name");
            if (datasetName != null) {
                datasetNames.add(datasetName);
            }
        }
        return datasetNames;
    }

    /**
     * Read the maqcii properties file (such as maqcii-c.properties) and load the filenames
     * for dataset and samples files for training and validation for each of the datasets
     * listed in datasetNames. The result is a Map[datasetName, Map[key, value]] where key
     * is the which of the files and value is the path to the file.
     *
     * @param maqciiPropertiesFilename the maqcii properties file
     * @param datasetNames             the datasets to obtain the filenames for
     * @return a map so we can determine the dataset and samples files for each specific dataset
     *         name.
     */
    public Map<String, Map<String, String>> readMaqciiProperties(
            final String maqciiPropertiesFilename, final Set<String> datasetNames) {
        final Properties maqciiProperties;
        try {
            maqciiProperties = new Properties(new File(maqciiPropertiesFilename));
        } catch (ConfigurationException e) {
            LOG.fatal("Error reading properties file " + maqciiPropertiesFilename);
            System.exit(10);
            return null;  // To make IDEA happy.
        }

        final Map<String, Map<String, String>> resultsMap =
                new Object2ObjectLinkedOpenHashMap<String, Map<String, String>>();
        // Make sure it has appropriate entries
        // ALL expected data must be accounted for or return null
        for (final String datasetName : datasetNames) {

            final Map<String, String> datasetResultsMap =
                    new Object2ObjectLinkedOpenHashMap<String, String>();
            resultsMap.put(datasetName, datasetResultsMap);

            datasetResultsMap.put("training.dataset-file", readPropertyOneOf(maqciiProperties,
                    datasetName + "." + propertiesTrainingLabel + ".dataset-file",
                    datasetName + ".dataset-file"));
            assert datasetResultsMap.get("training.dataset-file") != null;

            datasetResultsMap.put("validation.dataset-file", readPropertyOneOf(maqciiProperties,
                    datasetName + "." + propertiesValidationLabel + ".dataset-file",
                    datasetName + ".dataset-file"));
            assert datasetResultsMap.get("validation.dataset-file") != null;

            datasetResultsMap.put("training.test-samples", maqciiProperties.getString(
                    datasetName + "." + propertiesTrainingLabel + ".test-samples", null));
            assert datasetResultsMap.get("training.test-samples") != null;

            datasetResultsMap.put("validation.test-samples", maqciiProperties.getString(
                    datasetName + "." + propertiesValidationLabel + ".test-samples", null));
            assert datasetResultsMap.get("validation.test-samples") != null;

            datasetResultsMap.put("training.true-labels", maqciiProperties.getString(
                    datasetName + "." + propertiesTrainingLabel + ".true-labels", null));
            assert datasetResultsMap.get("training.true-labels") != null;

            datasetResultsMap.put("validation.true-labels", maqciiProperties.getString(
                    datasetName + "." + propertiesValidationLabel + ".true-labels", null));
            assert datasetResultsMap.get("validation.true-labels") != null;

            datasetResultsMap.put("tasks-file", maqciiProperties.getString(
                    datasetName + ".tasks-file", null));
            assert datasetResultsMap.get("tasks-file") != null;
        }
        return resultsMap;
    }

    /**
     * Return the first non-null of propertyNames. Returns null of none of the
     * propertyNames returns a value.
     *
     * @param properties    the properties to read from
     * @param propertyNames the list of property names to try to find the first
     *                      non-null value of
     * @return the found property value or null if no values were found
     */
    private static String readPropertyOneOf(
            final Properties properties, final String... propertyNames) {
        for (final String propertyName : propertyNames) {
            LOG.info("Looking for property named " + propertyName);
            final String value = properties.getString(propertyName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Take the datasetDetailsMap and localize by replacing occurances of
     * ${eval-dataset-root} with the value in datasetRoot.
     *
     * @param details     the datasetDetailsMap to localize
     * @param datasetRoot the datasetRoot to use to localize the map
     * @return a localized map
     */
    private Map<String, String> localizeDatasetDetailsMap(
            final Map<String, String> details, final String datasetRoot) {
        final Map<String, String> results = new Object2ObjectOpenHashMap<String, String>();
        for (Map.Entry<String, String> entry : details.entrySet()) {
            results.put(entry.getKey(),
                    entry.getValue().replace("${eval-dataset-root}", datasetRoot));
        }
        return results;
    }
}


