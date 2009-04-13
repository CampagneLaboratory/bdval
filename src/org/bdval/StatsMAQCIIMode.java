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
import org.bdval.tools.convert.maqcii.TsvToFromMapMaqciiFactory;
import edu.cornell.med.icb.io.TSVReader;
import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import edu.cornell.med.icb.learning.CrossValidation;
import edu.cornell.med.icb.tools.svmlight.EvaluationMeasure;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.io.FastBufferedReader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Calculates statistics for all models in the narrow Cornell prediction format
 * (validation data only).
 *
 * @author Fabien Campagne, Kevin Dorff
 *         Date: Sept 26, 2008
 *         Time: 8:36 AM
 */
public class StatsMAQCIIMode extends Predict {

    /** The logger. */
    private static final Log LOG = LogFactory.getLog(StatsMAQCIIMode.class);

    /** The cornell format input filename. */
    private String cornellFormatInputFilename;

    /** Helps with writing maqcii files. */
    private final MaqciiHelper maqciiHelper = new MaqciiHelper();

    /**
     * The map of trueLabelKey to ClassLabel.
     */
    private Map<String, String> trueLabelKeyToClassLabelMap;

    /** If we should run in sampleWithReplacement mode. */
    private boolean sampleWithReplacement;

    /** If running in sampleWithReplacement mode, how many iterations? */
    private int numberOfBootstrapSamples;

    /**
     * Interpret the command line arguments.
     * @param jsap the command line argument interpreter
     * @param result the result of command line argument parsing
     * @param options the DAVOptions - these are where the configuration information goes
     */
    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        cornellFormatInputFilename = result.getString("predictions-long");
        maqciiHelper.deletePreExistingSubmissionFile(result);
        maqciiHelper.setupSubmissionFile(result, options);
        trueLabelKeyToClassLabelMap = readTrueLabelFile(result.getString("true-labels-file"));
        sampleWithReplacement = result.getBoolean("sample-with-replacement");
        numberOfBootstrapSamples = result.getInt("number-of-bootstrap-samples");
    }

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        // there is no need for task definitions.
        jsap.getByID("task-list").addDefault("N/A");
        // there is no need for condition ids.
        jsap.getByID("conditions").addDefault("N/A");
        // there is no need for random seed.
        jsap.getByID("seed").addDefault("1");
        // there is no need for a gene list. The model has enough information to recreate it.
        jsap.getByID("gene-lists").addDefault("N/A");
        jsap.getByID("input").addDefault("N/A");
        jsap.getByID("platform-filenames").addDefault("N/A");

        final Parameter inputFilenameOption = new FlaggedOption("predictions-long")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setLongFlag("predictions-long")
                .setHelp("Filename that contains the predictions "
                        + "in the long narrow Cornell format distributed by MAQC-II.");
        jsap.registerParameter(inputFilenameOption);

        final Parameter trueLabelsDataFilenameOption = new FlaggedOption("true-labels-file")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setLongFlag("true-labels-file")
                .setHelp("Filename that contains the true labels data");
        jsap.registerParameter(trueLabelsDataFilenameOption);

        final Parameter sampleWithReplacementOption = new FlaggedOption("sample-with-replacement")
                .setStringParser(JSAP.BOOLEAN_PARSER)
                .setDefault("false")
                .setRequired(false)
                .setLongFlag("sample-with-replacement")
                .setHelp("If sample-with-replacement should be used");
        jsap.registerParameter(sampleWithReplacementOption);

        final Parameter numberOfBootstrapSamplesOption = new FlaggedOption(
                "number-of-bootstrap-samples")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("1000")
                .setRequired(false)
                .setLongFlag("number-of-bootstrap-samples")
                .setHelp("The number-of-bootstrap-samples to "
                        + "use if sample-with-replacement is true");
        jsap.registerParameter(numberOfBootstrapSamplesOption);

        maqciiHelper.defineSubmissionFileOption(jsap);
    }

    /**
     * Read the true labels TSV file.
     * @param filename filename to read
     */
    private static Map<String, String> readTrueLabelFile(final String filename) {
        Map<String, String> trueLabelsMap = null;
        try {
            System.out.println("Reading true-labels file " + filename);
            final TsvToFromMap trueLabelsTsv =
                    TsvToFromMapMaqciiFactory.getMapForType(
                            TsvToFromMapMaqciiFactory.TsvToFromMapType.TRUE_LABELS_FORMAT);
            int lineNum = 0;
            trueLabelsMap = new Object2ObjectOpenHashMap<String, String>();
            for (final String line : new TextFileLineIterator(filename)) {
                if (lineNum++ == 0) {
                    // Skip the header row (first row) and comment rows
                    continue;
                }
                final Map<String, String> data = trueLabelsTsv.readDataToMap(line);
                if (data != null) {
                    final TrueLabelLine trueLabelLine = new TrueLabelLine(
                            data.get("SampleId1"), data.get("SampleId2"), data.get("ClassLabel"),
                            data.get("DatasetCode"), data.get("EndpointCode"));
                    trueLabelsMap.put(trueLabelLine.trueLabelMapKey(), trueLabelLine.classLabel);
                }
            }
            System.out.println("... finished. number of lines="
                    + trueLabelsMap.size());
        } catch (IOException e) {
            LOG.error("Error reading true-labels-file. ", e);
            e.printStackTrace();
            System.exit(1);
        }
        return trueLabelsMap;
    }

    /**
     * Perform the processing of the input file.
     * @param options the DAVOptions to use with the processing
     */
    @Override
    public void process(final DAVOptions options) {
        System.out.println("StatsMAQCII");
        System.out.println("------------------------------------");
        System.out.println("Cornell input filename: " + cornellFormatInputFilename);
        System.out.println("Number of True Label keys: "
                + trueLabelKeyToClassLabelMap.keySet().size());
        System.out.println("sampleWithReplacement? " + sampleWithReplacement);
        System.out.println("... numberOfBootstrapSamples? " + numberOfBootstrapSamples);

        maqciiHelper.printSubmissionHeaders(options);

        LOG.info("Calculating statistics for predictions in long narrow Cornell format "
                + cornellFormatInputFilename);
        try {
            final TSVReader reader = new TSVReader(new FastBufferedReader(
                    new FileReader(this.cornellFormatInputFilename)));
            String lastModelId = "";
            final List<PredictionLine> items = new ObjectArrayList<PredictionLine>();
            int lineNum = 0;
            while (reader.hasNext()) {
                reader.next();
                if (lineNum++ == 0) {
                    // Skip the first line
                    continue;
                }
                final String sampleID1 = reader.getString();
                final String sampleID2 = reader.getString();
                final String organizationCode = reader.getString();
                final String datasetCode = reader.getString();
                final String endpointCode = reader.getString();
                final String maqciiModelID = reader.getString();
                final String organizationSpecificModelID = reader.getString();
                final double decisionValue = reader.getDouble();
                final String symbolicClassPrediction = reader.getString();
                final double threshold = reader.getDouble();
                final PredictionLine line = new PredictionLine(sampleID1,
                        sampleID2, organizationCode, datasetCode, endpointCode,
                        maqciiModelID, organizationSpecificModelID, decisionValue,
                        symbolicClassPrediction, threshold);

                if (!maqciiModelID.equals(lastModelId)) {
                    if (items.size() != 0) {
                        System.out.println("Processing model id " + maqciiModelID);
                        processPredictedItems(options, items);
                        items.clear();
                    }
                    lastModelId = maqciiModelID;
                }
                items.add(line);
            }
        } catch (IOException e) {
            LOG.error("Error reading predictions file. ", e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Process all predicted items for a single endpoint.
     * @param options the DAVOptions used to process.
     * @param items the PredctionLine
     */
    private void processPredictedItems(
            final DAVOptions options, final List<PredictionLine> items) {
        final ObjectSet<CharSequence> evaluationMeasureNames = new ObjectArraySet<CharSequence>();
        evaluationMeasureNames.addAll(Arrays.asList(MEASURES));
        EvaluationMeasure repeatedEvaluationMeasure = new EvaluationMeasure();
        final DoubleList decisions = new DoubleArrayList();
        final DoubleList trueLabels = new DoubleArrayList();
        int found = 0;
        int notFound = 0;
        for (final PredictionLine item : items) {
            final Double trueLabel = getTrueLabel(item);
            if (trueLabel != null) {
                found++;
                decisions.add(item.decisionValue);
                trueLabels.add(trueLabel);
            } else {
                notFound++;
            }
        }
        System.out.printf("Found = %d, Not Found = %d%n", found, notFound);

        final EvaluationMeasure allSplitsInARepeatMeasure;
        if (sampleWithReplacement) {
            final ObjectList<double[]> decisionList = new ObjectArrayList<double[]>();
            final ObjectList<double[]> trueLabelList = new ObjectArrayList<double[]>();
            for (int i = 0; i < numberOfBootstrapSamples; i++) {
                final DoubleList sampleDecisions = new DoubleArrayList();
                final DoubleList sampleTrueLabels = new DoubleArrayList();

                buildSample(options, decisions, trueLabels, sampleDecisions, sampleTrueLabels);
                decisionList.add(sampleDecisions.toDoubleArray());
                trueLabelList.add(sampleTrueLabels.toDoubleArray());
            }

            CrossValidation.evaluate(decisionList, trueLabelList, evaluationMeasureNames,
                    repeatedEvaluationMeasure, "", true);
        } else {
            repeatedEvaluationMeasure = CrossValidation.testSetEvaluation(decisions.toDoubleArray(),
                    trueLabels.toDoubleArray(), evaluationMeasureNames, true);
        }

        LOG.info(String.format(" %s", repeatedEvaluationMeasure.toString()));

        final int numberOfFeatures = 0;
        maqciiHelper.setModelId(items.get(0).organizationSpecificModelID);
        maqciiHelper.setEndpointCode(items.get(0).endpointCode);
        maqciiHelper.printSubmissionResults(options,
                repeatedEvaluationMeasure, numberOfFeatures, 0);
        LOG.info(String.format("Overall: %s", repeatedEvaluationMeasure.toString()));
    }

    /**
     * Returns the true label as a 0 or a 1 unless the trueLabel was an N/A or NA,
     * then it returns a null. This will use item.datasetCode, item.endpointCode,
     * item.sampleID1, item.sampleID2.
     * @param item the PredictionLine item to get the true label for
     * @return the true label or null
     */
    private Double getTrueLabel(final PredictionLine item) {
        final String trueLabelKey = item.trueLabelMapKey();
        final String classLabel = trueLabelKeyToClassLabelMap.get(trueLabelKey);
        if (classLabel == null) {
            LOG.error("Could not find true label using key " + trueLabelKey);
            System.exit(1);
            return null;
        }
        if (StringUtils.isNumeric(classLabel)) {
            return Double.parseDouble(classLabel);
        } else {
            LOG.debug("classLabel for true label Key " + trueLabelKey + " was " + classLabel);
            return null;
        }
    }

    class PredictionLine {
        String sampleID1;
        String sampleID2;
        String organizationCode;
        String datasetCode;
        String endpointCode;
        String MAQCIIModelID;
        String organizationSpecificModelID;
        double decisionValue;
        String symbolicClassPrediction;
        double threshold;

        PredictionLine(
                final String sampleID1, final String sampleID2, final String organizationCode,
                final String datasetCode, final String endpointCode, final String MAQCIIModelID,
                final String organizationSpecificModelID, final double decisionValue,
                final String symbolicClassPrediction, final double threshold) {
            super();
            this.sampleID1 = sampleID1;
            // sampleID2 needs to be modified if we are in NB
            if (datasetCode.equals("NB")) {
                this.sampleID2 = TrueLabelLine.fixNbSample2(sampleID2);
            } else {
                this.sampleID2 = sampleID2;
            }
            this.organizationCode = organizationCode;
            this.datasetCode = datasetCode;
            this.endpointCode = endpointCode;
            this.MAQCIIModelID = MAQCIIModelID;
            this.organizationSpecificModelID = organizationSpecificModelID;
            this.decisionValue = decisionValue;
            this.symbolicClassPrediction = symbolicClassPrediction;
            this.threshold = threshold;
        }

        public String trueLabelMapKey() {
            // sampleID2 needs to be modified if we are in NB
            return String.format("%s-%s-%s", sampleID1, sampleID2, endpointCode);
        }
    }

}
