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

package org.bdval.tools.convert.maqcii;

import edu.cornell.med.icb.io.TsvToFromMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bdval.tools.convert.IDataFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Read the MAQCII predictions file, write it as a Cornell formatted file.
 * @author Kevin Dorff
 */
public class CornellDataFormatter implements IDataFormatter {

    /** The output writer. */
    private final PrintWriter out;

    /** The options to use to help translate the data for writing. */
    private final PredictionsToCornellOptions options;

    /** The toCornell TSV writer. */
    private final TsvToFromMap toCornellTsv;

    /** The data for the Cornell formatted output file. */
    private final Map<String, String> data = new Object2ObjectOpenHashMap<String, String>();

    private final Set<String> uniqueModelSamples = new HashSet<String>();

    /**
     * Create a new data formatter.
     * @param optionsVal the optiosn to assist with writing the data.
     * @throws IOException error, probably writing the header to the output file
     */
    public CornellDataFormatter(final PredictionsToCornellOptions optionsVal) throws IOException {
        super();
        this.options = optionsVal;
        this.out = new PrintWriter(options.getOutputFile());
        this.toCornellTsv = TsvToFromMapMaqciiFactory.getMapForType(
                TsvToFromMapMaqciiFactory.TsvToFromMapType.CORNELL_FORMAT);
        toCornellTsv.writeHeader(out);
    }

    /**
     * Given a single line of prediction, convert to the new data format.
     * @param predictionData the prediction data
     * @throws IOException error writing the data
     */
    public void convertLine(final Map<String, String> predictionData) throws IOException {
        if (predictionData == null) {
            // Comment line
            return;
        }

        data.clear();
        data.put("SampleID1", options.resolveSample(predictionData.get("sampleId")));
        data.put("SampleID2", predictionData.get("sampleId"));
        data.put("OrganizationCode", "Cornell");
        final String orgSpecificModelId = extractCornellModelId(predictionData);
        data.put("OrganizationSpecificModelID", orgSpecificModelId);
        data.put("MAQCII ModelID", options.resolveModel(orgSpecificModelId));
        data.put("DatasetCode", predictionData.get("DatasetCode"));
        data.put("EndpointCode", predictionData.get("EndpointCode"));
        data.put("DecisionValue",
                String.valueOf(calculateDecisionValue(predictionData)));
        data.put("SymbolicClassPrediction", predictionData.get("predictedSymbolicLabel"));
        data.put("Threshold", "0");

        if (options.isOmitUnknown() && ("UNKNOWN".equals(data.get("SampleID1")))) {
            throw new IOException("!! OMITTING UNKNOWN 'SampleID1' for "
                    + orgSpecificModelId + " named " + predictionData.get("sampleId"));
        }
        if (options.isOmitUnknown() && ("UNKNOWN".equals(data.get("MAQCII ModelID")))) {
            System.out.println("!! OMITTING UNKNOWN for 'MAQCII ModelID' for "
                    + orgSpecificModelId);
            return;
        }
        final String key = data.get("SampleID1") + "-" + data.get("MAQCII ModelID");
        if (uniqueModelSamples.contains(key)) {
            throw new IOException("!! Error, model-sample " + key
                    + " because it has been seen before!");
        } else {
            uniqueModelSamples.add(key);
        }

        // Write the data to the output
        toCornellTsv.writeDataFromMap(out, data);
    }

    /**
     * Given a line of prediction data in a map. return the decision value.
     *
     * @param predictionData the line of prediction data
     * @return the decision value
     */
    public static double calculateDecisionValue(
            final Map<String, String> predictionData) {
        final double scale = Double.parseDouble(predictionData.get("probabilityOfClass1"));
        final double value = Double.parseDouble(predictionData.get("probabilityOfPredictedClass"));
        return scale * value;
    }

    /**
     * Extract the cornell model id from the predictions MODEL_FILENAME_PREFIX column in the
     * prediction data.
     *
     * @param predictionData the line of prediction data.
     * @return the cornell model id
     */
    public static String extractCornellModelId(final Map<String, String> predictionData) {
        final String modelFilenamePrefix = predictionData.get("modelFilenamePrefix");
        final String[] parts = StringUtils.split(modelFilenamePrefix, '-');
        final String modelId = parts[parts.length - 1];
        return StringUtils.substringBefore(modelId, ".zip");
    }

    /**
     * Close the writer.
     */
    public void close() {
        IOUtils.closeQuietly(out);
    }
}
