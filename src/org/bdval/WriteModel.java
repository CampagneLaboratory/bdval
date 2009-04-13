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
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.geo.tools.MicroarrayTrainEvaluate;
import edu.cornell.med.icb.learning.ClassificationHelper;
import edu.cornell.med.icb.learning.ClassificationProblem;
import edu.mssm.crover.tables.ColumnTypeException;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Train and write a model for each task and gene list.
 *
 * @author Fabien Campagne Date: Oct 23, 2007 Time: 6:14:36 PM
 */
public class WriteModel extends DAVMode {
    private static final Log LOG = LogFactory.getLog(WriteModel.class);
    private String modelPrefix;
    private String[] modelComponentPrefixes;

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result, final DAVOptions options) {
        super.interpretArguments(jsap, result, options);
        modelPrefix = result.contains("model-prefix") ? result.getString("model-prefix") : null;
        final Properties extraParameters = new Properties();
        if (result.contains("use-parameters")) {
            final String filename = result.getString("use-parameters");
            try {
                extraParameters.load(filename);
                final ObjectList<String> allParameters =
                        new ObjectArrayList<String>(options.classifierParameters);
                final Iterator<String> parameterKeyIt = extraParameters.getKeys();
                while (parameterKeyIt.hasNext()) {
                    final String parameterName = parameterKeyIt.next();
                    final double value = extraParameters.getDouble(parameterName);
                    final String added = parameterName + "=" + Double.toString(value);
                    allParameters.add(added);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding parameter from property file: " + added);
                    }
                }
                options.classifierParameters =
                        allParameters.toArray(new String[allParameters.size()]);

            } catch (ConfigurationException e) {
                LOG.error("Cannot load model parameters from file " + filename);
            }
        }
        if (result.contains("consensus-of-models")) {
            final String fileName = result.getString("consensus-of-models");
            Reader reader = null;
            try {
                final ArrayList<String> list = new ArrayList<String>();
                reader = new FileReader(fileName);
                final LineIterator it = new LineIterator(reader);
                while (it.hasNext()) {
                    final String line = (String) it.next();
                    list.add(line);
                }
                modelComponentPrefixes = list.toArray(new String[list.size()]);

            } catch (FileNotFoundException e) {
                LOG.error("Cannot find list of model components for consensus model: " + fileName);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
    }

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter pathways = new FlaggedOption("model-prefix")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("model-prefix")
                .setHelp("Prefix used to construct the model filename."
                );
        jsap.registerParameter(pathways);

        final Parameter optimalParameterInputFile = new FlaggedOption("use-parameters")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("use-parameters")
                .setHelp("Name of a Java properties file with model parameter values. "
                        + "These parameter values are appended, before training, to the "
                        + "classifier parameters provided on the command line.");
        jsap.registerParameter(optimalParameterInputFile);
        final Parameter consensusOfModels = new FlaggedOption("consensus-of-models")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("consensus-of-models")
                .setHelp("List of models to construct a consensus model. Such models pool "
                        + "decisions of a set of component models and predict the consensus "
                        + "decision. The argument must point to a file with one model "
                        + "prefix per line.");
        jsap.registerParameter(consensusOfModels);
    }

    @Override
    public void process(final DAVOptions options) {
        super.process(options);
        for (final ClassificationTask task : options.classificationTasks) {
            for (final GeneList geneList : options.geneLists) {
                final MutableString modelFilePrefix = new MutableString();

                try {
                    if (modelPrefix == null) {
                        modelFilePrefix.append(options.classifierParametersAsString());
                        modelFilePrefix.append('_');
                        modelFilePrefix.append(task.getDataAsText('_'));
                        modelFilePrefix.append('_');
                        modelFilePrefix.append(geneList.getType());
                    } else {
                        modelFilePrefix.append(modelPrefix);
                    }

                    if (modelComponentPrefixes != null) {
                        final BDVModel consensusModel =
                                new ConsensusBDVModel(modelFilePrefix.toString(),
                                modelComponentPrefixes);
                        try {
                            consensusModel.load(options);  // load the juror models before save
                        } catch (ClassNotFoundException e) {
                            LOG.fatal("Problem loading juror models " + modelFilePrefix, e);
                            System.exit(10);
                        }
                        consensusModel.save(options, task, splitPlan, this);

                    } else {
                        final Table processedTable =
                                processTable(geneList, options.inputTable, options,
                                        MicroarrayTrainEvaluate.calculateLabelValueGroups(task));
                        scaleFeatures(options, false, processedTable);

                        final ClassificationHelper helper = getClassifier(processedTable,
                                MicroarrayTrainEvaluate.calculateLabelValueGroups(task));

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Writing model for " + task + " gene list: " + geneList
                                    + " to filename: " + modelFilePrefix + ".model");
                        }

                        final ClassificationProblem scaledProblem = helper.problem;
                        helper.model = helper.classifier.train(scaledProblem, helper.parameters);
                        final BDVModel bdvModel = new BDVModel(modelFilePrefix.toString(),
                                helper, BDVModel.DEFAULT_STORAGE_FORMAT);
                        bdvModel.save(options, task, splitPlan, this);
                    }
                } catch (ColumnTypeException e) {
                    LOG.fatal(e);
                    System.exit(10);
                } catch (TypeMismatchException e) {
                    LOG.fatal(e);
                    System.exit(10);
                } catch (InvalidColumnException e) {
                    LOG.fatal(e);
                    System.exit(10);
                } catch (IOException e) {
                    LOG.fatal("An error occurred writing model " + modelFilePrefix, e);
                    System.exit(10);
                }
            }
        }
    }
}
