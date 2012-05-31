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

package org.bdval;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bdval.signalquality.BaseSignalQualityCalculator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Compare the distribution of each feature used in a set specific of biomarker models. Outputs a summary statistic
 * for each model processed. Distribution differences are quantified for feature in the same way as DistributionDifferenceByFeatureMode.
 * Summary statistics are calculated for each model. P-values are summarized with the Stouffer approach. Rank ratio statistics
 * are summarized as an average of rank ratio across all features in a model.
 *
 *
 *
 * @author Kevin Dorff
 */
public class DistributionDifferenceByModelMode extends DAVMode {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(DistributionDifferenceByModelMode.class);

    /** The pValues filename to read. */
    private String pvaluesFilename;

    /** The signal quality calculator object. */
    private BaseSignalQualityCalculator signalQualityCalcObj;

    /** True if we should write extended output. */
    private boolean extendedOutput;

    private TsvToFromMap inputTsvToFromMap;

    /** The maximum number of classes, important for the output file header to be correct. */
    private int maxNumClasses;

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

        final Parameter pvaluesFileOption = new FlaggedOption("pvalues-file")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setLongFlag("pvalues-file")
                .setHelp("The pvalues file generated with signal-quality-pvalues mode.");
        jsap.registerParameter(pvaluesFileOption);

        final Parameter signalQualityCalcClassOption =
                new FlaggedOption("signal-quality-calc-class")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setLongFlag("signal-quality-calc-class")
                        .setHelp("Fully qualified classname for an "
                                + "AbstractSignalQualityCalculator class.");
        jsap.registerParameter(signalQualityCalcClassOption);

        final Parameter extendedOutputOption =
                new FlaggedOption("extended-output")
                        .setStringParser(JSAP.BOOLEAN_PARSER)
                        .setDefault("false")
                        .setRequired(true)
                        .setLongFlag("extended-output")
                        .setHelp("If true, extra output will be included.");
        jsap.registerParameter(extendedOutputOption);
    }

    /**
     * Interpret the command line arguments.
     * @param jsap the JSAP command line parser
     * @param result the results of command line parsing
     * @param options the DAVOptions
     */
    @Override
    public void interpretArguments(
            final JSAP jsap, final JSAPResult result, final DAVOptions options) {
        checkArgumentsSound(jsap, result, false);
        setupPathwayOptions(result, options);

        extendedOutput = result.getBoolean("extended-output");
        pvaluesFilename = verifyFilenameOption(result, "pvalues-file");
        try {
            inputTsvToFromMap = TsvToFromMap.createFromTsvFile(new File(pvaluesFilename));
        } catch (IOException e) {
            LOG.error("Error reading pvalues file header from " + pvaluesFilename, e);
            System.exit(10);
        }
        maxNumClasses = countClassesFromPValuesColumns(inputTsvToFromMap.getColumnHeaders());

        LOG.info("Creating the signal quality calculator... maxNumClasses=" + maxNumClasses);
        signalQualityCalcObj = createCalculator(result);
        if (signalQualityCalcObj == null) {
            System.exit(10);
        }
    }

    /**
     * Given the columns, count the classes specified in the columns. A class named A will
     * exist if column names contain somecolumn[A]. This is not compatible with pvalue
     * files which don't have the classes columns written this way.
     * @param columns the list of columns
     * @return the max number of classes.
     */
    public int countClassesFromPValuesColumns(final List<String> columns) {
        final Set<String> classNames = new ObjectArraySet<String>();
        for (final String column : columns) {
            final int posStart = column.indexOf('[');
            final int posEnd = column.indexOf(']');
            if (posStart == -1 || posEnd == -1) {
                continue;
            }
            classNames.add(column.substring(posStart + 1, posEnd));
        }
        return classNames.size();
    }

    /**
     * Take a string and make a model list from it (string should be commans separated).
     * @param modelListStr the list of models, comma separated
     * @param allValue which string should mark all/none (denote this should return
     * an empty set.
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
     * Verify that a specified filename exists or fail out.
     * @param result the JSAPResults
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
     * Create the calculator object specified by the command line argument
     * signal-quality-calc-class
     * @param result the JSAPResult object
     * @return the newly created and configured calculator object or null if it
     * couldn't be created.
     */
    final BaseSignalQualityCalculator createCalculator(final JSAPResult result) {
        final String classname = result.getString("signal-quality-calc-class");
        try {
            final BaseSignalQualityCalculator calculator =
                    (BaseSignalQualityCalculator) Class.forName(classname).newInstance();
            calculator.configure(result,
                    BaseSignalQualityCalculator.OutputFileHeader.SIGNAL_QUALITIES,
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
     * @param options program options.
     */
    @Override
    public void process(final DAVOptions options) {
        super.process(options);
        LOG.info("Processing " + pvaluesFilename);

        final String[] allClasses = new String[maxNumClasses];
        System.arraycopy(BaseSignalQualityCalculator.CLASS_TRANSLATION,
                0, allClasses, 0, maxNumClasses);

        final Map<String, DoubleList> classToDataMap =
                new Object2ObjectOpenHashMap<String, DoubleList>();
        for (final String classId : allClasses) {
            classToDataMap.put(classId + "-p-value", new DoubleArrayList());
            classToDataMap.put(classId + "-t1", new DoubleArrayList());
            classToDataMap.put(classId + "-t2", new DoubleArrayList());
        }

        try {
            String modelId = null;
            String discriminator = null;

            final List<String> featuresList = new ObjectArrayList<String>();

            int lineNo = 0;
            for (final String line : new TextFileLineIterator(pvaluesFilename)) {
                if (line.startsWith("##")) {
                    // Copy lines that start with ## to the output file
                    signalQualityCalcObj.writeData(line);
                    continue;
                }
                final Map<String, String> data = inputTsvToFromMap.readDataToMap(line);
                if (data == null) {
                    // Likely a comment line
                    continue;
                }
                if (lineNo++ == 0) {
                    // Skip first non-commented line, will be the header
                    continue;
                }

                final String newDiscriminator = data.get("model-id");
                if (discriminator != null && !discriminator.equals(newDiscriminator)) {
                    // We have a new model-class, process the old one
                    signalQualityCalcObj.calculateSignalQuality(
                            modelId, allClasses, featuresList, classToDataMap);

                    // Clear the data for the current model to for the next model
                    featuresList.clear();
                    for (final DoubleList list : classToDataMap.values()) {
                        list.clear();
                    }
                }
                discriminator = newDiscriminator;
                modelId = data.get("model-id");
                featuresList.add(data.get("feature"));

                for (final String classId : allClasses) {
                    final String value = data.get("p-value[" + classId + "]");
                    if (StringUtils.isNotBlank(value)) {
                        // We have data for this class.
                        classToDataMap.get(classId + "-p-value").add(Double.parseDouble(value));
                        classToDataMap.get(classId + "-t1").add(Double.parseDouble(
                                data.get("t1[" + classId + "]")));
                        classToDataMap.get(classId + "-t2").add(Double.parseDouble(
                                data.get("t2[" + classId + "]")));
                    }
                }
            }
            if (featuresList.size() > 0) {
                // Output the last model
                // We have a new model, process the old one
                signalQualityCalcObj.calculateSignalQuality(
                        modelId, allClasses, featuresList, classToDataMap);
            }
        } catch (IOException e) {
            LOG.error("Error reading input file", e);
        } finally {
            signalQualityCalcObj.close();
        }
    }
}
