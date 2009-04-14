/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.bdval;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.geo.tools.MicroarrayTrainEvaluate;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import edu.mssm.crover.tables.readers.SyntaxErrorException;
import edu.mssm.crover.tables.readers.UnsupportedFormatException;
import edu.mssm.crover.tables.writers.InsightfulMinerTableWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Combine clinical data and high-throughput data table.
 */
public class CombineDataMode extends DAVMode {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(CombineDataMode.class);

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter inputFilenameOption = new FlaggedOption("input")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false) //no high throughput data is ok, i.e. clinical data only
                .setShortFlag('i')
                .setLongFlag("input")
                .setHelp("Input filename. This file contains the highthroughput measurement data used to"
                        + " discover markers.");
        jsap.registerParameter(inputFilenameOption);

        final Parameter clinicalFilenameOption = new FlaggedOption("clinical-data")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false) //no clinical data is ok,
                        // .setShortFlag('') //no short flags
                .setLongFlag("clinical-data")
                .setHelp("Input the clinical data file name. This file contains the measurement data used to"
                        + " discover markers.");
        jsap.registerParameter(clinicalFilenameOption);

        final Switch outputStatsFromGeneListSwitch = new Switch("transpose-data")
                .setLongFlag("transpose");
        jsap.registerParameter(outputStatsFromGeneListSwitch);


        final Parameter outputFlag = new FlaggedOption("output")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setShortFlag('o')
                .setLongFlag("output")
                .setHelp("Name of the output file for the combined data. Output is printed to the console when this "
                        + "flag is absent or when the value \"-\" is given.");
        jsap.registerParameter(outputFlag);

        final Parameter selectOpt = new UnflaggedOption("select") //select fields of clinical data
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setGreedy(true)
                .setHelp("Select the clinical variables needed. On default all clincial varaibles will be in.  this "
                        + "flag is absent or when the value \"-\" is given.");
        jsap.registerParameter(selectOpt);

        // TODO: add help
    }

    private Table clinicalInputTable;
    private String clinicalInput;
    private String [] variables;

    private void setupClinicalInput(final JSAPResult result) {
        final String clinicalInput = result.getString("clincal-data");

        try {
            clinicalInputTable = readInputFile(clinicalInput);
        } catch (IOException e) {
            System.err.println("Cannot read input file \"" + clinicalInput + "\"");
            LOG.fatal("Cannot read input file \"" + clinicalInput + "\"", e);
            System.exit(10);
        } catch (UnsupportedFormatException e) {
            System.err.println("The format of the input file \"" + clinicalInput
                    + "\" is not supported.");
            LOG.fatal("The format of the input file \"" + clinicalInput
                    + "\" is not supported.", e);
            System.exit(10);
        } catch (SyntaxErrorException e) {
            System.err.println("A syntax error was found in the input file \""
                    + clinicalInput + "\"");
            LOG.fatal("A syntax error was found in the input file \""
                    + clinicalInput + "\"", e);
            System.exit(10);
        }
    }

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        if (result.contains("input")) {
            setupInput(result, options);
        }
        if (result.contains("clinical-data")) {
            setupClinicalInput(result);
        }

        if (!result.contains("input") && !result.contains("clinical-data")) {
            System.err.println("at least one input data file has to be provided");
            LOG.fatal("A syntax error was found in the input file" );
            System.exit(10);
        }


        if (result.contains("select")) { //the user didn't provide any variables, then include all
            variables = result.getStringArray("select");
            //TODO: include all thevaraibles
        } else {
            ;//TODO: include variables
        }


//        if (result.contains("report-max-probes")) {
//            maxProbesToReport = result.getInt("report-max-probes");
//            LOG.info("Output will be restricted to the " + maxProbesToReport
//                    + " probesets with smaller t-test result.");
//        }
    }

    @Override
    public void process(final DAVOptions options) {
        super.process(options);

        for (final ClassificationTask task : options.classificationTasks) {
            for (final GeneList geneList : options.geneLists) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Combine for " + task);
                    }
                    final Table processedTable =  processTable(geneList, options.inputTable,
                            options, MicroarrayTrainEvaluate.calculateLabelValueGroups(task));

                    addLabelColumn(processedTable,
                            MicroarrayTrainEvaluate.calculateLabelValueGroups(task));
                    InsightfulMinerTableWriter.writeData(processedTable, options.output);
                    options.output.flush();
                } catch (Exception e) {
                    LOG.fatal(e);
                    System.exit(10);
                }
            }
        }
    }

    private void addLabelColumn(final Table processedTable, final List<Set<String>> labelValueGroups) throws InvalidColumnException {
        final HashMap<Set<String>, Integer> groupToCodedLabel = new HashMap<Set<String>, Integer>();
        if (labelValueGroups.size() != 2) {
            throw new IllegalArgumentException("Classification requires exactly two label groups.");
        }
        final Iterator<Set<String>> it = labelValueGroups.iterator();
        final Set<String> labelGroup0 = it.next();  // negative class
        final Set<String> labelGroup1 = it.next();  // positive class

        groupToCodedLabel.put(labelGroup0, -1);
        groupToCodedLabel.put(labelGroup1, 1);
        final int labelColumnIndex = processedTable.addColumn("label", double.class);

        final String[] idRefs = processedTable.getStrings("ID_REF");

        for (final String id : idRefs) {
            try {
                processedTable.appendDoubleValue(labelColumnIndex, recodeLabel(id, labelValueGroups, groupToCodedLabel));
            } catch (TypeMismatchException e) {
                LOG.warn(e);
            }
        }
    }

    protected int recodeLabel(final String labelValue, final List<Set<String>> labelValueGroups,
                              final HashMap<Set<String>, Integer> groupToCodedLabel) {
        for (final Set<String> labelGroup : labelValueGroups) {
            if (labelGroup.contains(labelValue)) {
                return groupToCodedLabel.get(labelGroup);
            }
        }

        throw new IllegalArgumentException("Label value " + labelValue
                + " must match a label group.");
    }
}
