/*
 * Copyright (C) 2007-2009 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.biomarkers;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.StringParser;
import com.martiansoftware.jsap.Switch;
import edu.cornell.med.icb.iterators.IteratorIterable;
import edu.mssm.crover.cli.CLI;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SequenceMode exists to read a sequence-file and sequentially run
 * several other DAVModes.
 *
 * @author Kevin Dorff Date: Mar 31, 2007
 */
public class SequenceMode extends DAVMode {
    /**
     * The logger to use.
     */
    private static final Log LOG = LogFactory.getLog(SequenceMode.class);

    private static final Pattern ADD_OPTION_PATTERN =
            Pattern.compile("addoption[\\s]+(required|optional|switch)[\\s]*"
                    + ":[\\s]*([a-zA-Z0-9-]*?)[\\s]*:[\\s]*(.*)");

    private static final Pattern DEF_VARIABLE_PATTERN =
            Pattern.compile("def[\\s]+([a-zA-Z0-9-]+?)[\\s]*=[\\s]*(.*)");

    /**
     * On the incoming arguments, these arguments will
     * be completely ignored.
     */
    public static final Set<String> FLAGS_TO_IGNORE;
    static {
        FLAGS_TO_IGNORE = new HashSet<String>();
        FLAGS_TO_IGNORE.add("-m");
        FLAGS_TO_IGNORE.add("--mode");
        FLAGS_TO_IGNORE.add("--sequence-file");
        FLAGS_TO_IGNORE.add("-o");
        FLAGS_TO_IGNORE.add("--output");
        FLAGS_TO_IGNORE.add("-g");
        FLAGS_TO_IGNORE.add("--gene-lists");
        FLAGS_TO_IGNORE.add("--gene-list");
    }

    /**
     * The file that the sequence lines will be read from to be executed.
     */
    private String sequenceFilename;

    /**
     * The lines from the sequence file.
     */
    private List<String> sequenceLines;

    /**
     * Ids that can be appended to any sequence command line (DAVMode jsap id's).
     */
    private Set<String> appendableIds;

    /**
     * The lines from the sequence file.
     */
    private Map<String, String> variables;

    /**
     * The jsap results so we can interrogate how they were defined.
     */
    private JSAPResult jsapResult;

    /**
     * The jsap as currently configured.
     */
    private JSAP jsapConfig;

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    @SuppressWarnings("unchecked")
    public void defineOptions(final JSAP jsap) throws JSAPException {
        jsapConfig = jsap;

        appendableIds = new HashSet<String>();
        for (final String id : new IteratorIterable<String>(jsapConfig.getIDMap().idIterator())) {
            LOG.debug("Adding appendable-id " + id);
            appendableIds.add(id);
        }

        readSequenceFile();
        final Parameter sequenceFileParam = new FlaggedOption("sequence-file")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setLongFlag("sequence-file")
                .setHelp("Specify the sequence file to use.");
        jsap.registerParameter(sequenceFileParam);
    }

    /**
     * Interpret the options from the command line argument
     * parser.
     *
     * @param jsap    the command line argument parser
     * @param result  the parsed results.
     * @param options the current Discover and Validate options.
     */
    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        jsapResult = result;
    }

    /**
     * Reads the sequence file and executes each sequence lines.
     *
     * @param options the incoming DAVoptions
     */
    @Override
    public void process(final DAVOptions options) {
        replaceVariableArguments();
        replaceVariableVariable();

        final List<String> linesToExecute = new LinkedList<String>();
        for (final String sequenceLine : sequenceLines) {
            linesToExecute.add(replaceArguments(sequenceLine));
        }

        // Execute all valid sequence lines
        for (final String lineToExecute : linesToExecute) {
            executeSequenceLine(lineToExecute);
        }
    }

    /**
     * Take a given jsap id that should exist within the jsap configuration
     * and return the flags for that given id. Up to two flags can be returned,
     * if there are two, the long flag will always be first.
     *
     * @param id the id of one of the configured options
     * @return the flags for the option (favoring the long flag)
     */
    private List<String> getJsapFlagsForId(final String id) {
        final List<String> flagsForId = new LinkedList<String>();
        final Parameter paramObj = jsapConfig.getByID(id);
        if (paramObj instanceof FlaggedOption) {
            final FlaggedOption flagOpt = (FlaggedOption) paramObj;
            final String longFlag = flagOpt.getLongFlag();
            if (longFlag != null) {
                flagsForId.add("--" + longFlag);
            }
            // Only add the short if the long isn't available.
            final char c = flagOpt.getShortFlag();
            if (c != JSAP.NO_SHORTFLAG) {
                flagsForId.add("-" + String.valueOf(c));
            }
        } else if (paramObj instanceof Switch) {
            final Switch switchOpt = (Switch) paramObj;
            final String longFlag = switchOpt.getLongFlag();
            if (longFlag != null) {
                flagsForId.add("--" + longFlag);
            }
            // Only add the short if the long isn't available.
            final char c = switchOpt.getShortFlag();
            if (c != JSAP.NO_SHORTFLAG) {
                flagsForId.add(String.valueOf(c));
            }
        }
        return flagsForId;
    }

    /**
     * Return the value of a variable, as defined in the sequence file processed.
     *
     * @param variableName Name of the variable queried.
     * @return the value of a variable, as defined in the sequence file.
     */
    public String getValue(final String variableName) {
        return variables.get(variableName);
    }

    /**
     * Replace variables and arguments in the sequenceLine.
     *
     * @param sequenceLineIncoming the sequenceLine to replace variables/arguments.
     * @return the sequenceLine ready to execute
     */
    @SuppressWarnings("unchecked")
    private String replaceArguments(final String sequenceLineIncoming) {
        String sequenceLine = sequenceLineIncoming;

        // Replace "def" variables
        for (final String variable : variables.keySet()) {
            // Replace any variables in the current sequence line
            final String value = variables.get(variable);
            final String variableDecorated = "%" + variable + "%";
            if (sequenceLine.contains(variableDecorated)) {
                LOG.debug("Replacing variable " + variableDecorated + " with " + value);
                sequenceLine = StringUtils.replace(
                        sequenceLine, variableDecorated, value);
            }
        }

        // Replace arguments
        for (final String id : new IteratorIterable<String>(jsapConfig.getIDMap().idIterator())) {
            final List<String> jsapFlags = getJsapFlagsForId(id);
            if (jsapFlags.isEmpty()) {
                LOG.error("Could not get long or short flag for id=" + id);
                System.exit(10);
            }
            final String primaryJsapFlag = jsapFlags.get(0);
            if (FLAGS_TO_IGNORE.contains(primaryJsapFlag)) {
                continue;
            }
            final Parameter paramObj = jsapConfig.getByID(id);
            if (paramObj instanceof Switch) {
                if (jsapResult.getBoolean(id)) {
                    if (sequenceLineContainsParameter(sequenceLine, jsapFlags)) {
                        LOG.debug("NOT Appending already existing switch " + primaryJsapFlag);
                    } else {
                        if (appendableIds.contains(id)) {
                            LOG.debug("Appending switch " + primaryJsapFlag);
                            sequenceLine += " " + primaryJsapFlag;
                        } else {
                            LOG.debug("NOT Appending because id " + id + " isn't appendable.");
                        }
                    }
                } else {
                    LOG.debug("Ignoring non-supplied jsap switch id " + id);
                }
            } else if (paramObj instanceof FlaggedOption) {
                // A flag switch exists. Pass it along.
                final FlaggedOption flagOpt = (FlaggedOption) paramObj;
                if (jsapResult.contains(id)) {
                    final String stringVal = jsapOptionToString(jsapResult, flagOpt);
                    if (sequenceLine.contains("%" + id + "%")) {
                        LOG.debug("Replacing %" + id + "% with " + stringVal);
                        sequenceLine = StringUtils.replace(
                                sequenceLine, "%" + id + "%", stringVal);
                    }
                    if (sequenceLineContainsParameter(sequenceLine, jsapFlags)) {
                        LOG.debug("NOT Appending already existing flag " + primaryJsapFlag);
                    } else {
                        if (appendableIds.contains(id)) {
                            if (StringUtils.isNotBlank(stringVal)) {
                                final String appendOption = " " + primaryJsapFlag + " " + stringVal;
                                LOG.debug("Appending " + appendOption);
                                sequenceLine += appendOption;
                            } else {
                                LOG.debug("NOT Appending because no value was found for " + id);
                            }
                        } else {
                            LOG.debug("NOT Appending because id " + id + " isn't appendable.");
                        }
                    }
                } else {
                    LOG.debug("Ignoring non-supplied jsap flag id " + id);
                }
            }
        }
        checkArguments(sequenceLine);
        return sequenceLine.trim();
    }

    /**
     * Replace variable values with those from the command line, if necessary.
     */
    @SuppressWarnings("unchecked")
    private void replaceVariableArguments() {
        for (final String varName : variables.keySet()) {
            final String varOldValue = variables.get(varName);
            String varNewValue = varOldValue;

            // Replace arguments
            for (final String id : new IteratorIterable<String>(jsapConfig.getIDMap().idIterator())) {
                final List<String> jsapFlags = getJsapFlagsForId(id);
                if (jsapFlags.isEmpty()) {
                    LOG.error("Could not get long or short flag for id=" + id);
                    System.exit(10);
                }
                final Parameter paramObj = jsapConfig.getByID(id);

                if (paramObj instanceof FlaggedOption) {
                    // A flag switch exists. Pass it along.
                    final FlaggedOption flagOpt = (FlaggedOption) paramObj;
                    if (jsapResult.contains(id)) {
                        final String stringVal = jsapOptionToString(jsapResult, flagOpt);
                        if (varNewValue.contains("%" + id + "%")) {
                            LOG.debug("Replacing variable %" + id + "% with " + stringVal);
                            varNewValue = StringUtils.replace(
                                    varNewValue, "%" + id + "%", stringVal);
                        }
                    }
                }
            }
            if (!varOldValue.equals(varNewValue)) {
                variables.put(varName, varNewValue);
                LOG.debug(String.format("Updated variable named %s with new value %s",
                        varName, varNewValue));
            }
        }
    }

    /**
     * Replace variable values with those from other variables, if necessary.
     */
    private void replaceVariableVariable() {
        for (final String varName : variables.keySet()) {
            final String varOldValue = variables.get(varName);
            String varNewValue = varOldValue;

            for (final String otherVarName : variables.keySet()) {
                if (varName.equals(otherVarName)) {
                    continue;
                }
                final String otherVar = "%" + otherVarName + "%";
                if (varNewValue.contains(otherVar)) {
                    varNewValue = StringUtils.replace(
                            varNewValue, otherVar, variables.get(otherVarName));
                }
            }

            if (!varOldValue.equals(varNewValue)) {
                variables.put(varName, varNewValue);
                LOG.debug(String.format("Updated variable named %s with new value %s",
                        varName, varNewValue));
            }
        }
    }

    /**
     * Return true any of the specified parameters (flags/switches) already
     * exists in the sequenceLine.
     *
     * @param sequenceLine the sequence line to check.
     * @param jsapFlags    the flags to check for
     * @return true if the specified parameter already exists in sequenceLine
     */
    public boolean sequenceLineContainsParameter(
            final String sequenceLine, final List<String> jsapFlags) {
        // Pad with spaces... important
        final String line = " " + sequenceLine + " ";

        for (final String jsapFlag : jsapFlags) {
            if (line.contains(" " + jsapFlag + " ")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verify that the sequenceLine doesn't contain any %x% variables
     * that should have been replaced.
     *
     * @param sequenceLine the sequenceLine to check.
     */
    @SuppressWarnings("unchecked")
    private void checkArguments(final String sequenceLine) {
        for (final String id : new IteratorIterable<String>(jsapConfig.getIDMap().idIterator())) {
            final String flagVar = "%" + id + "%";
            if (sequenceLine.contains(flagVar)) {
                LOG.error("Sequence line to be executed contains the unresolved value " + flagVar);
                System.exit(10);
            }
        }
    }

    /**
     * Execute a sequence line.
     *
     * @param lineToExecute the line to execute.
     */
    private void executeSequenceLine(final String lineToExecute) {
        // Split it so we can pass it to DiscoverAndValidate.main(newArgs)
        // TODO - we can't always split - this breaks paths with spaces in the name :-(
        final String[] newArgs = StringUtils.split(lineToExecute, ' ');
        LOG.info("Running Sequence Item:" + lineToExecute);
        try {
            DiscoverAndValidate.main(newArgs);
            System.gc();
        } catch (JSAPException e) {
            LOG.error("JSAPException Error running sequence line " + lineToExecute, e);
            e.printStackTrace();
            System.exit(10);
        } catch (IllegalAccessException e) {
            LOG.error("IllegalAccessException Error running sequence line " + lineToExecute, e);
            e.printStackTrace();
            System.exit(10);
        } catch (InstantiationException e) {
            LOG.error("InstantiationException Error running sequence line " + lineToExecute, e);
            e.printStackTrace();
            System.exit(10);
        }
    }

    /**
     * If you define a jsap flag as a DOUBLE, jsap wants you to read it as
     * a double. For the current situation, we only want the string values -
     * this will use the appropriate getter but then convert it to a String
     * for easier use. This isn't implemented for every possible version
     * of jsap StringParser. If other types are used, this will need to be
     * extended.
     *
     * @param jsapResult the jsapResult which holds a parsed command line
     * @param flagOpt    the flag we are currently getting a value for
     * @return the string version of that value
     */
    public static String jsapOptionToString(final JSAPResult jsapResult, final FlaggedOption flagOpt) {
        final StringParser parser = flagOpt.getStringParser();
        if (parser == JSAP.STRING_PARSER) {
            return jsapResult.getString(flagOpt.getID());
        } else if (parser == JSAP.INTEGER_PARSER) {
            return String.valueOf(jsapResult.getInt(flagOpt.getID()));
        } else if (parser == JSAP.DOUBLE_PARSER) {
            return String.valueOf(jsapResult.getDouble(flagOpt.getID()));
        } else if (parser == JSAP.BOOLEAN_PARSER) {
            return String.valueOf(jsapResult.getBoolean(flagOpt.getID()));
        } else if (parser == JSAP.CLASS_PARSER) {
            return jsapResult.getClass(flagOpt.getID()).getName();
        } else {
            return jsapResult.getString(flagOpt.getID());
        }
    }

    /**
     * When reading the sequence file, a line may look like
     * def variable=value
     * this will try to read such a line and place the definition
     * of the variable inside the variables variable
     *
     * @throws JSAPException error adding JSAP options
     */
    @SuppressWarnings("unchecked")
    private void readSequenceFile() throws JSAPException {
        sequenceFilename = CLI.getOption(getOriginalArgs(), "--sequence-file", null);
        if (sequenceFilename != null) {
            List<String> fileLines = null;
            try {
                fileLines = (List<String>) FileUtils.readLines(new File(sequenceFilename));
            } catch (IOException e) {
                LOG.error("Error reading sequence-file " + sequenceFilename, e);
                System.exit(10);
            }

            sequenceLines = new LinkedList<String>();
            variables = new HashMap<String, String>();
            for (final String fileLine : fileLines) {
                if (StringUtils.isNotBlank(fileLine)) {
                    if (fileLine.startsWith("def ")) {
                        defineVariable(fileLine);
                    } else if (fileLine.startsWith("addoption ")) {
                        addJsapOption(jsapConfig, fileLine, sequenceFilename);
                    } else if (!fileLine.startsWith("#")) {
                        sequenceLines.add(fileLine);
                    }
                }
            }
        }
    }

    /**
     * When reading the sequence file, a line may look like
     * def variable=value
     * this will try to read such a line and place the definition
     * of the variable inside the variables variable
     *
     * @param jsap         the jsap command line processor being used
     * @param sequenceLine the line to try to parse a variable from
     * @param sequenceFilename the filename to add the sequence line from
     * @throws JSAPException error adding JSAP options
     */
    public static void addJsapOption(final JSAP jsap, final String sequenceLine,
                                     final String sequenceFilename) throws JSAPException {
        final Matcher matcher = ADD_OPTION_PATTERN.matcher(sequenceLine);
        final boolean matchFound = matcher.find();

        if (!matchFound) {
            final String errorMessage = String.format(
                    "Sequence file %s require line that is not in the right format. "
                            + "Format should be "
                            + "'require required|optional|switch:longoption:description' "
                            + "but is %s", sequenceFilename, sequenceLine);
            LOG.error(errorMessage);
            return;
        }

        final String optionType = matcher.group(1);
        final String optionLongFlag = matcher.group(2);
        final String optionDescription = matcher.group(3);

        if ("required".equals(optionType) || "optional".equals(optionType)) {
            // Add a flag
            LOG.debug(String.format("Correctly matched (flag) addoption '%s' : '%s' : '%s'",
                    optionType, optionLongFlag, optionDescription));
            final Parameter newFlag = new FlaggedOption(optionLongFlag)
                    .setStringParser(JSAP.STRING_PARSER)
                    .setRequired("required".equals(optionType))
                    .setLongFlag(optionLongFlag)
                    .setHelp(optionDescription);
            jsap.registerParameter(newFlag);
        } else if ("switch".equals(optionType)) {
            // Add a switch
            LOG.debug(String.format("Correctly matched (switch) addoption '%s' : '%s' : '%s'",
                    optionType, optionLongFlag, optionDescription));
            final Parameter newSwitch = new Switch(optionLongFlag)
                    .setLongFlag(optionLongFlag)
                    .setHelp(optionDescription);
            jsap.registerParameter(newSwitch);
        } else {
            LOG.error("Unknown option type: " + optionType);
        }
    }

    /**
     * When reading the sequence file, a line may look like
     * def variable=value
     * this will try to read such a line and place the definition
     * of the variable inside the variables variable
     *
     * @param sequenceLine the line to try to parse a variable from
     */
    private void defineVariable(final String sequenceLine) {
        final Matcher matcher = DEF_VARIABLE_PATTERN.matcher(sequenceLine);
        final boolean matchFound = matcher.find();

        if (!matchFound) {
            LOG.error(String.format("Sequence file %s "
                    + " def line should be in the format 'def x=value' but is ' %s",
                    sequenceFilename, sequenceLine));
            return;
        }
        final String variable = matcher.group(1);
        final String value = matcher.group(2);
        variables.put(variable, value);
        LOG.debug(String.format("Correctly matched the variable '%s' = '%s'",
                variable, value));
    }
}
