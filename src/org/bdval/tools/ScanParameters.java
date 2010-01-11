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

package org.bdval.tools;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import org.apache.commons.lang.StringUtils;
import org.bdval.DAVOptions;
import org.bdval.DiscoverAndValidate;
import org.bdval.WithProcessMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * A tool to use another mode with various parameter/argument values.
 *
 * @author Fabien Campagne Date: Nov 23, 2007 Time: 4:43:22 PM
 */
public class ScanParameters {

    public static void main(final String[] args) throws JSAPException {
        final ScanParameters tool = new ScanParameters();
        final JSAP jsap = new JSAP();
        tool.defineOptions(jsap);
        final DAVOptions options = new DAVOptions();
        final JSAPResult result = jsap.parse(args);
        tool.interpretArguments(jsap, result, options);
        tool.process(options);
        System.exit(0);
    }

    String arguments;
    String[] values1;
    String[] values2;
    String[] values3;
    private int numberOfVariableArguments;
    String toolClassName;

    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter classNameParam =
                new FlaggedOption("class-name")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(DiscoverAndValidate.class.getName())
                        .setRequired(false)
                        .setLongFlag("class-name")
                        .setHelp("Fully qualified class name for the tool to run with the specified arguments." +
                                "The class must implement WithProcessMethod.");
        jsap.registerParameter(classNameParam);

        final Parameter argumentOptions =
                new FlaggedOption("arguments")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setLongFlag("arguments")
                        .setHelp("Arguments will be provided to the delegate mode for each value of the parameters." +
                                " Use scanf conventions to declare variable parameters (i.e., %d for decimal numbers,"
                                + "%f for floating numbers, %s for strings)");


        jsap.registerParameter(argumentOptions);
        final Parameter valueOneOptions =
                new FlaggedOption("1")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('1')
                        .setLongFlag("1")
                        .setHelp("Comma delimited list of values for the first argument. ");

        jsap.registerParameter(valueOneOptions);
        final Parameter valueTwoOptions =
                new FlaggedOption("2")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(false)
                        .setShortFlag('2')
                        .setLongFlag("2")
                        .setHelp("Comma delimited list of values for the second argument.");
        jsap.registerParameter(valueTwoOptions);

        final Parameter valueThreeOptions =
                new FlaggedOption("3")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(false)
                        .setShortFlag('3')
                        .setLongFlag("3")
                        .setHelp("Comma delimited list of values for the third argument.");
        jsap.registerParameter(valueThreeOptions);
    }

    public void interpretArguments(final JSAP jsap, final JSAPResult result, final DAVOptions options) {
        if (!result.success()) {
            System.out.println("Error parsing command line.");
            System.out.print(jsap.getHelp());
            System.exit(1);
        }
        arguments = result.getString("arguments");
        toolClassName=result.getString("class-name");
        int numValue1 = 1;
        int numValue2 = 1;
        int numValue3 = 1;
        if (result.contains("1")) {
            numberOfVariableArguments = 1;
            values1 = result.getString("1").split("[,]");
            numValue1 = values1.length;
        }
        if (result.contains("2")) {
            numberOfVariableArguments = 2;
            values2 = result.getString("2").split("[,]");
            numValue2 = values2.length;
        }
        if (result.contains("3")) {
            numberOfVariableArguments = 3;
            values3 = result.getString("3").split("[,]");
            numValue3 = values3.length;
        }
        if (values3 != null) {
            if (values2 == null || values1 == null) {
                System.err.println("First two arguments must be defined before the third one can be used.");
            }
        }
        if (values2 != null) {
            if (values1 == null) {
                System.err.println("First argument must be defined before the second one can be used.");
            }
        }
        System.out.println("Run will produce " + numValue1 * numValue2 * numValue3 + " combinations of parameters.");
    }

    public void process(final DAVOptions options) {
        final String[] values = new String[numberOfVariableArguments];
        if (numberOfVariableArguments >= 1) {
            for (final String value1 : values1) {
                values[0] = value1;
                if (numberOfVariableArguments >= 2) {
                    for (final String value2 : values2) {
                        values[1] = value2;
                        if (numberOfVariableArguments == 3) {
                            for (final String value3 : values3) {
                                values[2] = value3;
                                runOnce(values);
                            }
                        } else {
                            runOnce(values);
                        }
                    }
                } else {
                    runOnce(values);
                }
            }
        } else {
            runOnce(values);
        }
        System.exit(0);
    }

    private void runOnce(final String[] values) {
        final StringWriter writer = new StringWriter();
        final PrintWriter pw = new PrintWriter(writer);
        pw.printf(arguments, (Object[]) values);
        pw.flush();

        final String args = writer.getBuffer().toString();
        System.err.println("Arguments: " + arguments);

        System.out.println("Args list separated");
        final List<String> runArgsList = new LinkedList<String>();
        for (final String s : args.split(" ")) {
            if (StringUtils.isNotBlank(s)) {
                runArgsList.add(s);
            }
        }
        final String[] runArguments = runArgsList.toArray(new String[runArgsList.size()]);

        try {

            final Class<?> discoverAndValidateClass = Class.forName(toolClassName);
            final WithProcessMethod instance = (WithProcessMethod) discoverAndValidateClass.newInstance();

            instance.process(runArguments);

        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JSAPException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}

