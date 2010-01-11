/*
 * Copyright (C) 2007-2010 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.geo;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import edu.cornell.med.icb.cli.UseModality;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Fabien Campagne Date: Aug 16, 2007 Time: 2:21:24 PM
 */
public class GeoScanMode extends UseModality<GeoScanOptions> {
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        super.defineOptions(jsap);
        if (jsap.getByID("input") == null) {
            final String inputListFlagName = "input";

            final FlaggedOption inputListFlag =
                    new FlaggedOption(inputListFlagName)
                            .setStringParser(JSAP.STRING_PARSER)
                            .setDefault(JSAP.NO_DEFAULT)
                            .setRequired(true)
                            .setShortFlag('i')
                            .setLongFlag("input");
            inputListFlag.setHelp("Soft family filename for input.");
            jsap.registerParameter(inputListFlag);
        }
        if (jsap.getByID("output") == null) {
            final String inputListFlagName = "output";

            final FlaggedOption outputListFlag =
                    new FlaggedOption(inputListFlagName)
                            .setStringParser(JSAP.STRING_PARSER)
                            .setDefault(JSAP.NO_DEFAULT)
                            .setRequired(false)
                            .setShortFlag('o')
                            .setLongFlag("output");
            outputListFlag.setHelp(
                    "Output filename. Out will be written to stdout if options is not active.");
            jsap.registerParameter(outputListFlag);
        }
        if (jsap.getByID("format-adapter") == null) {
            final String formatAdapterFlagName = "format-adapter";

            final FlaggedOption formatAdapterFlag =
                    new FlaggedOption(formatAdapterFlagName)
                            .setStringParser(JSAP.STRING_PARSER)
                            .setDefault(JSAP.NO_DEFAULT)
                            .setRequired(false)
                            .setShortFlag('f')
                            .setLongFlag("format-adapter");
            formatAdapterFlag.setHelp(
                    "Class name for the format adapter. A format adapter is a class that implements interface"
                            +
                            "edu.cornell.icb.geo.FormatAdapter. The class makes it possible to customize a scan for each type of GEO"
                            +
                            " soft format that can be encountered.");
            jsap.registerParameter(formatAdapterFlag);
        }
    }

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final GeoScanOptions options) {
        if (!result.success()) {
            System.out.println("Error parsing command line.");
            System.exit(1);
        }

        final String output = result.getString("output");
        if (output == null) {
            options.output = new PrintWriter(System.out);
            System.err.println("Output will be written to stdout");
        } else {
            try {
                options.output = new PrintWriter(output);
                System.out.println("Output will be written to file " + output);
            } catch (IOException e) {
                System.err.println(
                        "Cannot create output file for filename " + output);
            }
        }

        final String inputFilename = result.getString("input");
        options.softFilename = inputFilename;
        String adapterClassname = result.getString("format-adapter");
        if (adapterClassname == null) {
            adapterClassname = DefaultSignalAdapter.class.getCanonicalName();
        }
        try {
            final Class clazz = Class.forName(adapterClassname);
            options.formatAdapter = (FormatAdapter) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot instantiate format adapter class "
                    + adapterClassname);
            System.exit(10);
        } catch (IllegalAccessException e) {
            System.err.println(
                    "Cannot access format adapter class " + adapterClassname);
            System.exit(10);
        } catch (InstantiationException e) {
            System.err.println("Cannot instantiate format adapter class "
                    + adapterClassname);
            System.exit(10);
        }

    }

    @Override
    public void process(final GeoScanOptions options) {
    }
}
