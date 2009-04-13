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

package edu.cornell.med.icb.biomarkers.tools.convert.maqcii;

import com.martiansoftware.jsap.JSAPException;
import edu.cornell.med.icb.biomarkers.tools.convert.OptionsConfigurationException;
import edu.cornell.med.icb.biomarkers.tools.convert.OptionsSupport;
import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Verify the predictions -> leming conversion.
 * @author Kevin Dorff
 */
public class VerifyConversion {

    /** The options. */
    private VerifyConversionOptions options;

    /** cornell_model_id + cornell_sample_id (key) to decision value map. */
    private Object2DoubleOpenHashMap<String> modelSampleToDecisionMap =
            new Object2DoubleOpenHashMap<String>();

    /**
     * leming_model_id + leming_sample_id (key) to
     * cornell_model_id + cornell_sample_id (value) translation map.
     **/
    private Map<String, String> lemingKeyToCornellKeyMap =
            new Object2ObjectOpenHashMap<String, String>();

    /** The fromPredictions TSV reader. */
    private TsvToFromMap predctionsTsv = TsvToFromMapMaqciiFactory.getMapForType(
            TsvToFromMapMaqciiFactory.TsvToFromMapType.PREDICTION_FORMAT);

    /**
     * Verify the previous conversion of Prediction->Cornell->Leming.
     * @param args the command line arguments
     * @throws java.io.IOException error reading / writing
     */
    public static void main(final String[] args) throws IOException {
        VerifyConversion tool = null;
        try {
            tool = new VerifyConversion(args);
        } catch (OptionsConfigurationException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (JSAPException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        tool.process();
        System.exit(0);
    }

    /**
     * Create the Object to Verify the previous conversion of
     * Prediction->Cornell->Leming.
     * @param args the command line arguments
     * @throws OptionsConfigurationException error converting the options
     * @throws JSAPException jsap error reading the arguments
     * @throws IOException error reading files for the options
     */
    public VerifyConversion(final String[] args)
            throws OptionsConfigurationException, JSAPException, IOException {
        super();
        options = new VerifyConversionOptions(args);
        System.out.println(options.toString());
    }

    /**
     * Execute the verification with the specified options.
     * @throws IOException error reading input or writing output
     */
    public void process() throws IOException {
        // Read all of the prediction data into a map
        System.out.println("Reading all predictions files ");
        for (final File file : options.getAllPredictionsFiles()) {
            if (!file.toString().endsWith(".txt")) {
                continue;
            }
            addPredictionFileToMap(file);
        }

        // Verify Cornell data
        verifyCornellFile(options.getCornellFile());

        // Verify all of the leming files
        for (final File file : options.getAllLemingFiles()) {
            verifyLemingFile(file);
        }
        System.out.println("Done.");
    }

    /**
     * Read a single prediction file, add the data to the modelSampleToDecisionMap.
     * This will also verify that the data (modelId + sampleId) is unique.
     * @param file the prediction file to read
     * @throws IOException error reading the prediction file
     */
    private void addPredictionFileToMap(final File file) throws IOException {
        System.out.println("Reading file " + file.toString());
        int lineNo = 0;
        for (final String line : new TextFileLineIterator(file)) {
            lineNo++;
            final Map<String, String> predictionData = predctionsTsv.readDataToMap(line);
            if (predictionData == null) {
                // comment
                continue;
            }
            final String modelId = CornellDataFormatter.extractCornellModelId(predictionData);
            final String sampleId = predictionData.get("sampleId");
            final String key = modelId + "-" + sampleId;
            if (modelSampleToDecisionMap.get(key) != null) {
                throw new IOException("!! ERROR model-sample must be unique in the prediction files"
                    + " but it isn't... " + key + " on line " + lineNo);
            }
            modelSampleToDecisionMap.put(modelId + "-" + sampleId,
                    CornellDataFormatter.calculateDecisionValue(predictionData));
        }
    }

    /**
     * Verify the Cornell file. Makes sure each (modelId+SampleId) is unique plus
     * that each decision value matches the decision value in the predictions files.
     * This will will also add to the lemingKeyToCornellKeyMap to translate leming
     * (modelId+SampleId) to Cornell (modelId+SampleId) as that will be needed when
     * reading the leming formatted files.
     * @param cornellFile the cornell file to read
     * @throws IOException error reading or verifying the Cornell data
     */
    private void verifyCornellFile(final File cornellFile) throws IOException {
        final TsvToFromMap cornellTsv = TsvToFromMapMaqciiFactory.getMapForType(
            TsvToFromMapMaqciiFactory.TsvToFromMapType.CORNELL_FORMAT);
        int lineNumber = 0;
        System.out.println("Verifying cornell file " +
                OptionsSupport.filenameFromFile(cornellFile));
        for (final String line : new TextFileLineIterator(cornellFile)) {
            lineNumber++;
            if (lineNumber == 1) {
                continue;
            }
            final Map<String, String> cornellData = cornellTsv.readDataToMap(line);
            if (cornellData == null) {
                // comment line
                continue;
            }
            final double decisionValue;
            try {
                decisionValue = Double.parseDouble(cornellData.get("DecisionValue"));
            } catch (NumberFormatException e) {
                System.out.println("Could not convert to a numeric value "
                        + cornellData.get("DecisionValue") + " line=" + lineNumber);
                throw e;
            }
            final String lemingModelId = cornellData.get("MAQCII ModelID");
            final String lemingSampleId = cornellData.get("SampleID1");
            final String cornellModelId = cornellData.get("OrganizationSpecificModelID");
            final String cornellSampleId = cornellData.get("SampleID2");
            final String cornellKey = cornellModelId + "-" + cornellSampleId;
            final String lemingKey = lemingModelId + "-" + lemingSampleId;
            final double predictionDecisionValue = modelSampleToDecisionMap.getDouble(cornellKey);
            if (predictionDecisionValue != decisionValue) {
                throw new IOException(
                        "Cornell decision values doesn't match prediction decision value "
                        + "c=" + decisionValue + " p=" + predictionDecisionValue
                        + " cornell_key=" + cornellKey + " leming_key=" + lemingKey);
            }

            if (lemingKeyToCornellKeyMap.get(lemingKey) != null) {
                throw new IOException("modelId-sampleId must be unique in the prediction files"
                    + " but it isn't... " + lemingKey);
            }
            lemingKeyToCornellKeyMap.put(lemingKey, cornellModelId + "-" + cornellSampleId);
        }

    }

    /**
     * Verify a single leming file (in either Cologne or non-Cologne format), that it matches
     * the data from the original prediction file. The actual work is done in the alternate
     * version of verifyLemingFile.
     * @param lemingFile the leming file to verify
     * @throws IOException error reading or verifying the file
     */
    private void verifyLemingFile(final File lemingFile) throws IOException {
        final TsvToFromMap lemingTsv = TsvToFromMap.createFromTsvFile(lemingFile);
        final List<String> columns = lemingTsv.getColumnHeaders();
        if (columns.contains("Array_A01_FileName (S3R5)")) {
            verifyLemingFile("cologne",lemingTsv, lemingFile,
                    TsvToFromMapMaqciiFactory.LEMING_FORMAT_COLOGNE_COLUMNS,
                    "Column Heading in the Normalized Array Data File");
        } else {
            verifyLemingFile("standard", lemingTsv, lemingFile,
                    TsvToFromMapMaqciiFactory.LEMING_FORMAT_STANDARD_COLUMNS,
                    "Column Heading (in the Normalized Array Data File)");
        }
    }

    /**
     * Do the actual work of verifying the leming file.
     * @param type the type (cologne or standard)... this is here for human debugging
     * of verification messages
     * @param lemingTsv the fromLeming TSV reader object
     * @param lemingFile the file to read
     * @param nonModelColumns the non-model columsn in the leming file
     * @param sampleColumnName the column name that contains the sampleId in the
     * leming formatted file
     * @throws IOException error reading or verifying the data
     */
    private void verifyLemingFile(
            final String type, final TsvToFromMap lemingTsv, final File lemingFile,
            final String[] nonModelColumns, final String sampleColumnName) throws IOException {
        final List<String> lemingModelIds = lemingTsv.getColumnHeaders();
        for (final String column : nonModelColumns) {
            // Remove the non-model columns, this should leave us with a list of
            // the models in this leming file
            lemingModelIds.remove(column);
        }

        int lineNumber = 0;
        System.out.println("Verifying leming-" + type + " file " +
                OptionsSupport.filenameFromFile(lemingFile));
        for (final String line : new TextFileLineIterator(lemingFile)) {
            lineNumber++;
            if (lineNumber == 1) {
                continue;
            }
            final Map<String, String> lemingData = lemingTsv.readDataToMap(line);
            for (final String lemingModelId : lemingModelIds) {
                final String lemingSampleId =
                        lemingData.get(sampleColumnName);
                final String lemingKey = lemingModelId + "-" + lemingSampleId;
                final String cornellKey = lemingKeyToCornellKeyMap.get(lemingKey);

                final double lemingDecision;
                try {
                    lemingDecision = Double.parseDouble(lemingData.get(lemingModelId));
                } catch (NumberFormatException e) {
                    System.out.println("Could not convert to a numeric value "
                            + lemingData.get(lemingModelId) + " line=" + lineNumber);
                    throw e;
                }

                if (cornellKey == null) {
                    throw new IOException("lemming modelId-sampleId not found in "
                        + "lemingKeyToCornellKeyMap. lemingKey=" + lemingKey);
                }
                final double predictionDecision = modelSampleToDecisionMap.getDouble(cornellKey);

                if (lemingDecision != predictionDecision) {
                    throw new IOException(
                            "Cornell decision values doesn't match prediction decision value "
                            + "l=" + lemingDecision + " p=" + predictionDecision
                            + " cornell_key=" + cornellKey + " leming_key=" + lemingKey);
                }
            }
        }
    }
}
