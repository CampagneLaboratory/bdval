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

import com.martiansoftware.jsap.JSAPException;
import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import org.bdval.tools.convert.IDataFormatter;
import org.bdval.tools.convert.OptionsConfigurationException;
import org.bdval.tools.convert.OptionsSupport;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Takes a single TSV file in the Cornell format and converts it to a
 * set of files in the Leming format, one file per endpoint. The Leming formatted
 * file is a TSV file.
 * @author Kevin Dorff
 */
public class CornellToLeming {


    /** The options. */
    private final CornellToLemingOptions options;

    /** The fromCornell TSV reader. */
    private TsvToFromMap fromCornellTsv;

    /** Map of endpoint to a IDataFormatter object. */
    private final Map<String, IDataFormatter> endpointToDataFormatterMap;

    /**
     * Convert a set of predictions files to cornell format.
     * @param args the command line arguments
     * @throws java.io.IOException error reading / writing
     */
    public static void main(final String[] args) throws IOException {
        CornellToLeming tool = null;
        try {
            tool = new CornellToLeming(args);
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
    public CornellToLeming(final String[] args)
            throws OptionsConfigurationException, JSAPException, IOException {
        super();
        options = new CornellToLemingOptions(args);
        System.out.print(options);
        endpointToDataFormatterMap = new HashMap<String, IDataFormatter>();
    }

    /**
     * Execute the conversion with the options as defined by interpretArguments.
     * @throws IOException error reading input or writing output
     */
    public void process() {
        try {
            fromCornellTsv = TsvToFromMapMaqciiFactory.getMapForType(
                    TsvToFromMapMaqciiFactory.TsvToFromMapType.CORNELL_FORMAT);
            convertFile(options.getInputFile());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            for (final IDataFormatter formatter : endpointToDataFormatterMap.values()) {
                formatter.close();
            }
        }
    }

    /**
     * Convert the Cornell formatted file to Leming formatted files,
     * one per endpoint.
     * @param inputFile the cornell formatted file
     * @throws IOException error reading or writing.
     */
    private void convertFile(final File inputFile) throws IOException {
        int lineNumber = 0;
        for (final String line : new TextFileLineIterator(inputFile)) {
            if (lineNumber++ == 0) {
                continue;
            }
            try {
                final Map<String, String> data = fromCornellTsv.readDataToMap(line);
                final IDataFormatter formatter = getDataFormatter(data.get("EndpointCode"));
                /*
                if (data.get("EndpointCode").equals("K")
                        && data.get("SampleID1").equals(
                        "RT_0_13_119_ACO3_Lung_07008_Mouse430_2_RSTLRI000_2")) {
                    System.out.println("Problem on " + lineNumber);
                    System.exit(1);
                }
                */
                formatter.convertLine(data);
                if ((lineNumber % 500) == 0) {
                    System.out.println("Read " + lineNumber + " lines");
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

    /** The set of endpoints that need to be in the special "leming-cologne" format. */
    private static final Set<String> COLOGNE_ENDPOINTS = new HashSet<String>();
    static {
        COLOGNE_ENDPOINTS.add("J");
        COLOGNE_ENDPOINTS.add("K");
        COLOGNE_ENDPOINTS.add("L");
        COLOGNE_ENDPOINTS.add("M");
    }

    /**
     * Given a specific endpoint, obtain the appropriate IDataFormatter object.
     * @param endpointCode the endpoint to get the IDataFormatter for
     * @return the IDataFormatter for the given endpoint
     * @throws IOException error obtaining the IDataFormatter
     */
    public IDataFormatter getDataFormatter(final String endpointCode) throws IOException {
        IDataFormatter formatter = endpointToDataFormatterMap.get(endpointCode);
        if (formatter != null) {
            return formatter;
        }
        if (COLOGNE_ENDPOINTS.contains(endpointCode)) {
            formatter = new LemingCologneDataFormatter(options, endpointCode);
        } else {
            formatter = new LemingStandardDataFormatter(options, endpointCode);
        }
        endpointToDataFormatterMap.put(endpointCode, formatter);
        return formatter;
    }

}
