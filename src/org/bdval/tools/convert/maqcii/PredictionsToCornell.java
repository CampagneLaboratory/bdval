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

package org.bdval.tools.convert.maqcii;

import com.martiansoftware.jsap.JSAPException;
import org.bdval.MaqciiHelper;
import org.bdval.tools.convert.IDataFormatter;
import org.bdval.tools.convert.OptionsConfigurationException;
import org.bdval.tools.convert.OptionsSupport;
import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Convert a directory of files from the MAQC-II "predictions" output format to the
 * "Cornell" format. This takes a directory and pulls all the files within that dir
 * including all the files within directories. Output is a single TSV file.
 * @author Kevin Dorff
 */
public class PredictionsToCornell {

    /** The options. */
    private PredictionsToCornellOptions options;

    /** The fromPredictions TSV reader. */
    private TsvToFromMap fromPredictionsTsv;

    /**
     * Convert a set of predictions files to cornell format.
     * @param args the command line arguments
     * @throws IOException error reading / writing
     */
    public static void main(final String[] args) throws IOException {
        PredictionsToCornell tool = null;
        try {
            tool = new PredictionsToCornell(args);
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
     * Create the class to convert the predictions files to cornell format.
     * @param args the command line arguments
     * @throws OptionsConfigurationException error converting the options
     * @throws JSAPException jsap error reading the arguments
     * @throws IOException error reading files for the options
     */
    public PredictionsToCornell(final String[] args)
            throws OptionsConfigurationException, JSAPException, IOException {
        super();
        options = new PredictionsToCornellOptions(args);
        System.out.println(options.toString());
    }

    /**
     * Execute the conversion with the options as defined by interpretArguments.
     * @throws IOException error reading input or writing output
     */
    public void process() throws IOException {
        IDataFormatter dataFormatter = null;
        try {
            dataFormatter = new CornellDataFormatter(options);
            fromPredictionsTsv = TsvToFromMapMaqciiFactory.getMapForType(
                    TsvToFromMapMaqciiFactory.TsvToFromMapType.PREDICTION_FORMAT);
            for (final File inputFile : options.getAllInputFiles()) {
                if (!inputFile.toString().endsWith(".txt")) {
                    continue;
                }
                System.out.println("Processing file: "
                        + OptionsSupport.filenameFromFile(inputFile));
                convertFile(inputFile, dataFormatter);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (dataFormatter != null) {
                dataFormatter.close();
            }
        }
    }

    /**
     * Convert a single predictions formatted file to the cornell format.
     * @param inputFile the predictions file
     * @param dataFormatter the cornell data formatter object
     * @throws IOException error reading/writing
     */
    private void convertFile(
            final File inputFile, final IDataFormatter dataFormatter) throws IOException {
        int lineNumber = 0;
        final MaqciiHelper.MaqciiDataSetDetails details = endpointCodeFromFile(inputFile);
        if (options.isOmitUnknown() && details == MaqciiHelper.MaqciiDataSetDetails.Unknown) {
            // Nothing to do for this
            System.out.println("!! OMITTING UNKNOWN for "
                    + OptionsSupport.filenameFromFile(inputFile));
            return;
        }
        System.out.println("With details type=" + details);
        for (final String line : new TextFileLineIterator(inputFile)) {
            lineNumber++;
            try {
                final Map<String, String> data = fromPredictionsTsv.readDataToMap(line);
                if (data != null) {
                    // Inject the EndpointCode and DatasetCode into the data map
                    // as it is needed when writing in Cornell format.
                    data.put("EndpointCode", details.endpointCode);
                    data.put("DatasetCode", details.dataSetCode);
                    dataFormatter.convertLine(data);
                }
            } catch (IOException e) {
                throw new IOException(String.format("Error reading Predictions file %s, line %d",
                        OptionsSupport.filenameFromFile(inputFile), lineNumber), e);
            } catch (NumberFormatException e) {
                throw new IOException(String.format("Error reading Predictions file %s, line %d",
                        OptionsSupport.filenameFromFile(inputFile), lineNumber), e);
            }
        }
    }

    /**
     * Given a specific file's name, determine the endpoint that file is related to.
     * @param file the file to determine the endpoint for
     * @return the MaqciiDataSetDetails enum, which contains the endpoint information, etc.
     */
    public static MaqciiHelper.MaqciiDataSetDetails endpointCodeFromFile(final File file) {
        final String base = FilenameUtils.getBaseName(OptionsSupport.filenameFromFile(file));
        final String[] parts = StringUtils.split(base, "-");
        return MaqciiHelper.MaqciiDataSetDetails.getByName(parts[0]);
    }
}
