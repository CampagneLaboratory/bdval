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

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.geo.tools.MicroarrayTrainEvaluate;
import edu.cornell.med.icb.learning.ClassificationHelper;
import edu.cornell.med.icb.learning.CrossValidation;
import edu.cornell.med.icb.learning.FeatureScaler;
import edu.cornell.med.icb.tools.svmlight.EvaluationMeasure;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Evaluate an array of performance measures by cross-validation. The number of folds is
 * configurable with the --folds option (e.g., 5, 10 are typical values of the fold parameter).
 *
 * @author Fabien Campagne Date: Oct 19, 2007 Time: 2:42:16 PM
 */
public class CrossValidationMode extends DAVMode {
    private static final Log LOG = LogFactory.getLog(CrossValidationMode.class);
    final CharSequence[] measures = {"auc", "mat", "rmse", "acc", "f", "spec", "sens", "prec", "rec", "MCC"};
    private int cvRepeatNumber;
    final MaqciiHelper maqciiHelper = new MaqciiHelper();
    static final NumberFormat formatter;
    static {
        formatter = new DecimalFormat();
        formatter.setMaximumFractionDigits(2);
    }

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
       final Parameter cvRepeatNumberParam =
            new FlaggedOption("cv-repeats")
                    .setStringParser(JSAP.INTEGER_PARSER)
                    .setDefault("1")
                    .setRequired(false)
                    .setLongFlag("cv-repeats")
                    .setHelp("Number of cross validation repeats. default=1 (does one round of "
                            + "cross-validation). Values larger than one cause the cross "
                            + "validation to be repeated and results averaged over the rounds.");
        jsap.registerParameter(cvRepeatNumberParam);

        maqciiHelper.defineSubmissionFileOption(jsap);
    }

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        super.interpretArguments(jsap, result, options);
        if (result.contains("folds")) {
            final int foldNumber = result.getInt("folds");
            options.crossValidationFoldNumber = foldNumber;
        }
        this.cvRepeatNumber = result.getInt("cv-repeats");

        maqciiHelper.setupSubmissionFile(result, options);
    }

    @Override
    public void process(final DAVOptions options) {
        super.process(options);
        maqciiHelper.printSubmissionHeaders(options);
        for (final ClassificationTask task : options.classificationTasks) {
            printHeaders(options,measures,task);

            for (final GeneList geneList : options.geneLists) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Measuring CV for " + task + " gene list: " + geneList);
                    }
                    final Table processedTable =
                            processTable(geneList, options.inputTable, options,
                                    MicroarrayTrainEvaluate.calculateLabelValueGroups(task));

                    // use the same seed for each gene list comparison
                    // ensures that the same fold is generated. This eliminates
                    // a source of variation that could otherwise confuse comparisons.
                    final EvaluationMeasure measure = measureCV(task, processedTable,
                            options.crossValidationFoldNumber,
                            options.randomSeed, options.scalerClass);

                    printAllStatResults(options, task, geneList, measure);
                    final int numberOfFeatures = processedTable.getColumnNumber() - 1;
                    maqciiHelper. printSubmissionResults(options, measure, numberOfFeatures, cvRepeatNumber);
                } catch (NullPointerException e) {
                    throw e;
                } catch (Exception e) {
                    LOG.fatal(e);
                    System.exit(10);
                }
            }
        }
        if (options.output != null) {
            options.output.flush();
            options.output = null;
        }
        if (options.submissionOutput != null) {
            options.submissionOutput.flush();
            options.submissionOutput = null;
        }
    }

    public static void printAllStatResults(final DAVOptions options, final ClassificationTask task, final GeneList geneList, final EvaluationMeasure measure) {
        options.output.print(options.classiferClass.getCanonicalName());
        options.output.print('\t');
        options.output.print(options.classifierParametersAsString());
        options.output.print('\t');
        options.output.print(task.getDataAsText('\t'));
        options.output.print("\t");
        options.output.print(options.randomSeed);
        options.output.print("\t");
        options.output.print(geneList);
        options.output.print('\t');
        options.output.print(measure.getDataAsText('\t'));
        options.output.print('\n');

        options.output.flush();
    }

    public static void printHeaders(final DAVOptions options, final CharSequence[] measures, final ClassificationTask task) {
        if (options.outputFilePreexist) {
            return;
        }
        options.output.print("ClassifierType");
        options.output.print('\t');
        options.output.print("ClassifierParameters");
        options.output.print('\t');
        options.output.print(task.getHeaders('\t'));
        options.output.print("\t");
        options.output.print("randomSeed");
        options.output.print("\t");
        options.output.print("geneList");
        options.output.print('\t');
        options.output.print(EvaluationMeasure.getHeaders('\t',measures));
        options.output.print('\n');
        options.output.flush();
    }

    protected EvaluationMeasure measureCV(final ClassificationTask task,
                                          final Table processedTable,
                                          final int crossValidationFoldNumber,
                                          final int seed,
                                          final Class<? extends FeatureScaler> scalerClass)
            throws TypeMismatchException, InvalidColumnException {
        final ClassificationHelper helper =
                getClassifier(processedTable, MicroarrayTrainEvaluate.calculateLabelValueGroups(task));

        final EvaluationMeasure measure;
        final RandomEngine randomEngine = new MersenneTwister(seed);
        final CrossValidation crossValidation =
                new CrossValidation(helper.classifier, helper.problem, randomEngine);
        crossValidation.setRepeatNumber(cvRepeatNumber);
        crossValidation.evaluateMeasures(measures);

        crossValidation.setScalerClass(scalerClass);

        measure = crossValidation.crossValidation(crossValidationFoldNumber);
        if (LOG.isDebugEnabled()) {
            LOG.debug("measure: " + measure);
        }
        return measure;
    }
}
