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
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.io.IOUtils;
import org.bdval.tools.convert.IDataFormatter;
import org.bdval.tools.convert.OptionsSupport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Read the Cornell maqcii data file, writing a leming-cologne data file.
 * @author Kevin Dorff
 */
public class LemingStandardDataFormatter implements IDataFormatter {

    /** The output writer. */
    private final PrintWriter out;

    /** The toLeming TSV writer. */
    private final TsvToFromMap toLemingTsv;

    /**
     * A map of sampleId to the data for that sampleId.
     */
    private final Map<String, Map<String, String>> sampleIdToDataMap =
        new Object2ObjectLinkedOpenHashMap<String, Map<String, String>>();

    /**
     * Create a new data formatter.
     * @param options the optiosn to assist with writing the data.
     * @param endpoint the endpoint this formatter is writing
     * @throws IOException problem writing data
     */
    public LemingStandardDataFormatter(
            final CornellToLemingOptions options, final String endpoint) throws IOException {
        super();
        this.out = new PrintWriter(
                new File(OptionsSupport.filenameFromFile(options.getOutputDirectory())
                        + "/" + options.getDatePrefix()
                        + "-validation-predictions-template-format-cornell-" + endpoint + ".txt"));
        this.toLemingTsv = TsvToFromMapMaqciiFactory.getMapForType(
                TsvToFromMapMaqciiFactory.TsvToFromMapType.LEMING_FORMAT_STANDARD);
    }

    /**
     * Given a single line of prediction, convert to the new data format.
     * @param cornellData the prediction data
     * @throws java.io.IOException error writing the data
     */
    public void convertLine(final Map<String, String> cornellData) throws IOException {
        if (cornellData == null) {
            // Comment line
            return;
        }

        /*
        FROM: CORNELL
        "SampleID1",
        "SampleID2",
        "OrganizationCode",
        "DatasetCode",
        "EndpointCode",
        "MAQCII ModelID",
        "OrganizationSpecificModelID",
        "DecisionValue",
        "SymbolicClassPrediction",
        "Threshold"

        TO: LEMING STANDARD (NON-COLOGNE)
        "Column Heading (in the Normalized Array Data File)",
        "Raw Array Data File",
        "OrganizationCode",
        "DatasetCode",
        "EndpointCode"

         */

        final String sampleId = cornellData.get("SampleID1");
        Map<String, String> sampleData = sampleIdToDataMap.get(sampleId);
        if (sampleData == null) {
            sampleData = new Object2ObjectOpenHashMap<String, String>();
            sampleIdToDataMap.put(sampleId, sampleData);
            sampleData.put("Column Heading (in the Normalized Array Data File)", sampleId);
            sampleData.put("Raw Array Data File", cornellData.get("SampleID2"));
            sampleData.put("OrganizationCode", "Cornell");
            sampleData.put("DatasetCode", cornellData.get("DatasetCode"));
            sampleData.put("EndpointCode", cornellData.get("EndpointCode"));
        }
        final String modelId = cornellData.get("MAQCII ModelID");
        toLemingTsv.addColumn(modelId);
        sampleData.put(modelId, cornellData.get("DecisionValue"));
    }

    /**
     * Close the writer.
     */
    public void close() {
        toLemingTsv.writeHeader(out);
        for (final Map<String, String> line : sampleIdToDataMap.values()) {
            toLemingTsv.writeDataFromMap(out, line);
        }

        IOUtils.closeQuietly(out);
    }
}
