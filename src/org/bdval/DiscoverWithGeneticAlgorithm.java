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
import com.martiansoftware.jsap.Switch;
import edu.cornell.med.icb.geo.GEOPlatformIndexed;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.FixedGeneList;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.geo.tools.MicroarrayTrainEvaluate;
import edu.cornell.med.icb.learning.ClassificationHelper;
import edu.cornell.med.icb.learning.CrossValidation;
import edu.cornell.med.icb.optimization.AbstractSubSetFitnessFunction;
import edu.cornell.med.icb.optimization.OptimizeSubSet;
import edu.cornell.med.icb.optimization.SubSetFitnessFunction;
import edu.cornell.med.icb.tissueinfo.similarity.TranscriptScore;
import edu.cornell.med.icb.tools.svmlight.EvaluationMeasure;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Discover sets of features that maximize a given performance measure, using optimization with
 * genetic algorithms. Classification is performed with a support vector machine (linear or RBF
 * kernel). Starting with the entire set of features presented as input (containing N features),
 * the algorithm optimizes CV10 performance of a N*ratio set of features with a genetic algorithm.
 * (Typical choice for r is ratio r is 0.5 to keep 50% of features at each iteration.) Various
 * parameters of the optimization can affect the computational resources required to carry out the
 * optimization, and how close the found solution is to the optimal solution of the optimization
 * problem. Larger values or population size and number of iterations (see runtime arguments)
 * favor optimal solutions, but increase computational time. As usual with optimization algorithm,
 * there is no guarantee that the optimal solution will be found. In the case of biomarker
 * discovery, that is probably OK, since the fitness function (cross validation F-1 on a
 * finite training set) is also not optimal.
 *
 * @author Fabien Campagne Date: Nov 17, 2007 Time: 11:17:28 PM
 */
public class DiscoverWithGeneticAlgorithm extends DAVMode {
    private static final Log LOG = LogFactory.getLog(DiscoverWithGeneticAlgorithm.class);
    private int numProbesets = 50;

    private double ratio = 0.5d;
    private DAVOptions localOptions;
    private int numOptimizationStepsPerIteration = 100;
    private int populationSize = 20;    // increase to 100-1000
    private FeatureReporting reporter;
    private boolean useRServer;
    private boolean writeGeneListFormat;
    private int cvRepeatNumber;
    private String[] discreteParameters;
    private String optimalParametersFilename;
    private String optimizeMeasureName;


    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter ratioParam = new FlaggedOption("ratio")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(".5")
                .setRequired(true)
                .setLongFlag("ratio")
                .setShortFlag('r')
                .setHelp("The ratio of new number of feature to original number of features, "
                        + "for each iteration.");
        jsap.registerParameter(ratioParam);

        final Parameter numStepsParam = new FlaggedOption("number-of-steps")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("100")
                .setRequired(true)
                .setLongFlag("number-of-steps")
                .setShortFlag('n')
                .setHelp("The number of genetic algorithm evolution steps. Larger values increase "
                        + "the chance that the optimal solution will be found, but "
                        + "increase computation time.");
        jsap.registerParameter(numStepsParam);

        final Parameter populationSizeParam = new FlaggedOption("population-size")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("10")
                .setRequired(true)
                .setLongFlag("population-size")
                .setShortFlag('s')
                .setHelp("Number of chromosomes for genetic algorithm optimization. The larger the population size,"
                        + " the more diversity can be represented in the population, and the more effective"
                        + " cross-over will be at combining successful solutions into a more optimal offspring."
                        + " Larger population sizes are more computationally expensive, since the fitness "
                        + " function must be evaluated for each chromosome at each evolution step. ");
        jsap.registerParameter(populationSizeParam);

        final Parameter discreteParameters = new FlaggedOption("discrete-parameters")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("discrete-parameters")
                .setHelp("A list of discrete classifier parameters to optimize at the same time "
                        + "as the feature set. Parameters must be described in the format "
                        + "param1=value1,value2,...[:param2=value1,value2,...]. "
                        + "For instance, alpha=1,2,3,4:beta=0.2,.5,.33 will optimize the "
                        + "parameters alpha and beta alongside with the feature set. The "
                        + "combination of features and parameter values that optimizes "
                        + "CV performance will be kept. Optimal parameter values will be written "
                        + "to stderr, or to the value of argument --optimal-parameters-out");
        jsap.registerParameter(discreteParameters);

        final Parameter foldNumber =
                new FlaggedOption("folds")
                        .setStringParser(JSAP.INTEGER_PARSER)
                        .setDefault("10")
                        .setRequired(true)
                        .setShortFlag('f')
                        .setLongFlag("folds")
                        .setHelp("Number of cross validation folds. default=10/CV10.");

        jsap.registerParameter(foldNumber);

        final Parameter cvRepeatNumberParam =
                new FlaggedOption("cv-repeats")
                        .setStringParser(JSAP.INTEGER_PARSER)
                        .setDefault("1")
                        .setRequired(false)
                        .setLongFlag("cv-repeats")
                        .setHelp("Number of cross validation repeats. default=1 (does on round of "
                                + "cross-validation). Values larger than one cause the cross "
                                + "validation to be repeated and results averaged over "
                                + "the rounds.");

        jsap.registerParameter(cvRepeatNumberParam);

        final Parameter outputGeneList = new Switch("output-gene-list")
                .setLongFlag("output-gene-list")
                .setHelp("Write features to the output in the tissueinfo gene list format.");
        jsap.registerParameter(outputGeneList);

        final Parameter optimizeWithROCList = new Switch("roc")
                .setLongFlag("roc")

                .setHelp("Optimize the area under the ROC curve. If neither this option nor "
                        + "--maximize is not provided, maximizes the F-1 measure (harmonic mean "
                        + "of precision and recall). Otherwise, the parameter --maximize will "
                        + "name the objective function.");
        jsap.registerParameter(optimizeWithROCList);

        final Parameter numFeatureParam = new FlaggedOption("num-features")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("50")
                .setRequired(true)
                .setLongFlag("num-features")
                .setHelp("Number of features to select.");
        jsap.registerParameter(numFeatureParam);

        final Parameter optimalParameterOutputFile = new FlaggedOption("optimal-parameters-out")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("optimal-parameters-out")
                .setHelp("Name of the file where optimal parameters will be written "
                        + "(as Java properties).");
        jsap.registerParameter(optimalParameterOutputFile);

        final Parameter optimizeParam = new FlaggedOption("maximize")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("maximize")
                .setHelp("Select the objective measure that the GA process will try to maximize. "
                        + "Valid measure names include auc, mat, acc. For a complete list of "
                        + "measure names, see the ROCR documentation.");
        jsap.registerParameter(optimizeParam);
    }

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        super.interpretArguments(jsap, result, options);

        if (result.contains("optimize") && result.getBoolean("roc")) {
            System.err.println("Cannot specify both --maximize and --roc options.");
            System.exit(1);
        } else {
            if (result.getBoolean("roc")) {
                optimizeMeasureName = "auc";
                useRServer = true;
            } else if (result.contains("maximize")) {

                useRServer = true;
                optimizeMeasureName = result.getString("maximize");
                if (!optimizationRequiresR()) {
                    // we do not need RServe to evaluate MCC
                    useRServer = false;
                }
            } else {
                useRServer = false;
                optimizeMeasureName = "F-1";
            }
        }

        numProbesets = result.getInt("num-features");
        ratio = result.getDouble("ratio");
        populationSize = result.getInt("population-size");
        numOptimizationStepsPerIteration = result.getInt("number-of-steps");
        foldNumber = result.getInt("folds");
        System.out.println("CV" + foldNumber + " Ratio=" + ratio + " populationSize=" + populationSize
                + " numberOfSteps=" + numOptimizationStepsPerIteration);
        writeGeneListFormat = result.getBoolean("output-gene-list");
        reporter = new FeatureReporting(writeGeneListFormat);

        this.cvRepeatNumber = result.getInt("cv-repeats");
        this.discreteParameters = parseDiscreteParameters(result);
        this.optimalParametersFilename = result.getString("optimal-parameters-out");

    }

    private String[] parseDiscreteParameters(final JSAPResult result) {
        final String paramDefs = result.getString("discrete-parameters");
        if (paramDefs == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        } else {
            return paramDefs.split("[:]");
        }
    }

    public int foldNumber = 10;

    double alpha;
    GEOPlatformIndexed inputIdentifiers;

    @Override
    public void process(final DAVOptions options) {
        super.process(options);
        //final Table inputTable = options.inputTable;
        System.out.println("Optimizing " + (this.optimizeMeasureName));
        for (final ClassificationTask task : options.classificationTasks) {
            for (final GeneList geneList : options.geneLists) {
                int reducedNumProbeset;
                int iteration = 1;
                System.out.println("Processing gene list: " + geneList.toString());
                try {
                    int numKept;
                    GeneList activeGeneList = geneList;
                    IntSet sourceSet = new IntArraySet();
                    IntSet previousSubset = null;
                    IntSet fitestSubSet = null;
                    double[] fitestParamValues = ArrayUtils.EMPTY_DOUBLE_ARRAY;
                    options.trainingPlatform = new GEOPlatformIndexed();
                    Table processedTable =
                            processTable(activeGeneList,
                                    options.inputTable, options,
                                    MicroarrayTrainEvaluate.calculateLabelValueGroups(
                                            task));
                    // try to free some memory (we cannot call processTable anymore after this:)
                    options.inputTable = null;
                    System.gc();
                    do {
                        System.out.println("Discover markers with GA wrapper for " + task);

                        final EvaluationMeasure evalMeasure;
                        if (iteration == 1) {

                            // FIRST ITERATION

                            final ClassificationHelper helper = getClassifier(processedTable,
                                    MicroarrayTrainEvaluate.calculateLabelValueGroups(
                                            task));
                            final RandomEngine randomEngine = new MersenneTwister(options.randomSeed);
                            final CrossValidation CV = new CrossValidation(helper.classifier, helper.problem, randomEngine);
                            final boolean useR = optimizationRequiresR();
                            CV.useRServer(useR); // user R server only when required.
                            CV.setRepeatNumber(cvRepeatNumber);
                            CV.setScalerClass(options.scalerClass);

                            if (useRServer) {
                                CV.evaluateMeasure(optimizeMeasureName);
                            }
                            //            CV.setScalerClass(PercentileScalingRowProcessor.class);
                            evalMeasure = CV.crossValidation(foldNumber);

                            System.out.println("Initial " + getPerformanceMeasureName() + " measure: " + getOptimizationMeasure(evalMeasure));
                            numKept = options.trainingPlatform.getProbeIds().size();

                            for (int i = 0; i < numKept; i++) {
                                sourceSet.add(i);
                            }
                            inputIdentifiers = options.trainingPlatform;

                            if (numKept <= numProbesets) {
                                LOG.error("Cannot remove probesets, already below the target number.");
                                fitestSubSet = sourceSet;
                                break;
                            }
                        } else {
                            numKept = previousSubset.size();
                            sourceSet = previousSubset;

                        }
                        localOptions = options;
                        System.out.println("Num probeset as input : " + numKept);

                        final SubSetFitnessFunction convergenceCriterion;
                        final Table processedTableConstant = processedTable;
                        convergenceCriterion =
                                new AbstractSubSetFitnessFunction() {
                                    @Override
                                    public double evaluate(final IntSet subset, final double[] paramValues) {
                                        try {

                                            GeneList geneListFromSubset = convertSubsetToGeneList(subset);

                                            Table filteredTable =
                                                    filterTable(options, processedTableConstant,
                                                            geneListFromSubset);

                                            ClassificationHelper helper = getClassifier(filteredTable,
                                                    MicroarrayTrainEvaluate.calculateLabelValueGroups(
                                                            task));
                                            int paramIndex = 0;
                                            for (final String parameterName : getParameterNames(discreteParameters)) {
                                                helper.parameters.setParameter(parameterName, paramValues[paramIndex++]);
                                            }
                                            helper.classifier.setParameters(helper.parameters);
                                            RandomEngine randomEngine = new MersenneTwister(options.randomSeed);

                                            CrossValidation CV = new CrossValidation(helper.classifier, helper.problem,
                                                    randomEngine);
                                            final boolean useR = optimizationRequiresR();

                                            CV.useRServer(useR);
                                            CV.setRepeatNumber(cvRepeatNumber);
                                            CV.setScalerClass(options.scalerClass);
                                            CV.evaluateMeasure(optimizeMeasureName);

                                            final EvaluationMeasure eMeasure = CV.crossValidation(foldNumber);

                                            geneListFromSubset = null;
                                            filteredTable = null;
                                            helper = null;
                                            randomEngine = null;
                                            CV = null;

                                            final double measure = getOptimizationMeasure(eMeasure);
                                            LOG.info(task.getExperimentDataFilename() + " evaluated " +
                                                    getPerformanceMeasureName() + " " + measure);

//                                                double std = getOptimizationMeasureStd(eMeasure);

                                            // return measure -std;
                                            return measure;

                                        } catch (InvalidColumnException e) {
                                            LOG.error(e);
                                            System.exit(10);
                                        } catch (TypeMismatchException e) {
                                            LOG.error(e);
                                            System.exit(10);
                                        }
                                        throw new InternalError("This statement should never be reached.");
                                    }
                                };

                        reducedNumProbeset = Math.max(numProbesets, (int) (numKept * ratio));

                        if (reducedNumProbeset == numKept) {
                            System.out.println("Previous step optimized the same number of probesets already.");
                            break;
                        } else {
                            System.out.println("Optimizing to keep " + reducedNumProbeset + " probesets.");
                        }
                        final OptimizeSubSet optimizationEngine =
                                new OptimizeSubSet(sourceSet,
                                        reducedNumProbeset,
                                        convergenceCriterion,
                                        populationSize,
                                        discreteParameters);
                        optimizationEngine.setModuloProgressReport(2);

                        optimizationEngine.optimize(
                                numOptimizationStepsPerIteration, .001);
                        fitestSubSet =
                                optimizationEngine.getFitestSubset();
                        fitestParamValues = optimizationEngine.getFitestParameterValues();
                        previousSubset = fitestSubSet;
                        // use as gene list for the next iteration: the restriction of the previous gene list
                        // to those probesets that were found with to have the bext F-1 measure in this iteration.

                        activeGeneList = convertSubsetToGeneList(fitestSubSet);
                        System.out.println("numKept: " + numKept + " reducedNumber: " + reducedNumProbeset);
                        if (!writeGeneListFormat) {
                            // print intermediate result for this iteration.
                            printFeatures(options, iteration, fitestSubSet, convergenceCriterion.evaluate(fitestSubSet,
                                    fitestParamValues));
                        }
                        iteration++;

                        final GeneList geneListFromSubset = convertSubsetToGeneList(fitestSubSet);

                        final Table fitestTable =
                                filterTable(options, processedTable,
                                        geneListFromSubset);
                        processedTable = fitestTable;

                        // hint to the JVM that this would be a good time to garbage collect.
                        System.gc();
                    } while (numKept > numProbesets);

                    printFeatures(options, iteration, fitestSubSet, 0);
                    printFitestParamValues(options, fitestParamValues, getParameterNames(discreteParameters));
                } catch (Exception e) {
                    LOG.error(e);
                    e.printStackTrace();
                    System.exit(10);
                }
            }
        }
    }

    private boolean optimizationRequiresR() {
        return !(optimizeMeasureName.equalsIgnoreCase("F-1") || optimizeMeasureName.equalsIgnoreCase("MCC")
                || optimizeMeasureName.equalsIgnoreCase("sens") ||
                optimizeMeasureName.equalsIgnoreCase("spec"));
    }

    private String getPerformanceMeasureName() {
        return optimizeMeasureName;

    }

    private double getOptimizationMeasure(final EvaluationMeasure eMeasure) {
        double result = 0;
        if (!useRServer) {
            result = eMeasure.getF1Average();
        } else {
            result = eMeasure.getPerformanceValueAverage(optimizeMeasureName);
        }

        // Convert NaN -> 0
        result = result != result ? 0 : result;
        if (result < 0) {
            // evaluation measure must be positive..
            result = 0;
        }
        return result;
    }

    private double getOptimizationMeasureStd(final EvaluationMeasure eMeasure) {
        double result = 0;

        if (!useRServer) {
            result = eMeasure.getF1StdDev();
        } else {
            result = eMeasure.getPerformanceValueStd(optimizeMeasureName);
        }
        // Convert NaN -> 0
        return result != result ? 0 : result;
    }

    private void printFitestParamValues(final DAVOptions options, final double[] fitestParamValues, final String[] parameterNames) {
        if (optimalParametersFilename == null) {
            System.err.println("Best CV performance was obtained with the following parameter values :");
            int paramIndex = 0;
            for (final String paramName : parameterNames) {
                System.err.println(paramName + "=" + Double.toString(fitestParamValues[paramIndex++]));
            }
        } else {
            final Properties props = new Properties();
            int paramIndex = 0;

            for (final String paramName : parameterNames) {
                props.setProperty(paramName, fitestParamValues[paramIndex++]);
            }
            try {
                props.setHeader("# Best CV performance was obtained with the following parameter values :");
                props.save(optimalParametersFilename);
            } catch (ConfigurationException e) {
                System.err.println("Cannot write optimal parameters to file.");
                LOG.error("Cannot write optimal parameters to file.", e);
            }
        }
    }

    private String[] getParameterNames(final String[] discreteParameters) {
        final String[] names = new String[discreteParameters.length];
        for (int i = 0; i < discreteParameters.length; i++) {
            names[i] = discreteParameters[i].split("[=]")[0];
        }
        return names;
    }

    private void printFeatures(final DAVOptions options, final int iteration, final IntSet fitestSubSet, final double score) {
        final ObjectSet<String> probesets = convertSubsetToProbesetIds(fitestSubSet);
        for (final String probeset : probesets) {
            final int probesetIndex = options.registerProbeset(probeset);
            final TranscriptScore tsProbeset = new TranscriptScore(score, probesetIndex);

            reporter.reportFeature(iteration, score, tsProbeset, options);
        }
    }


    private GeneList convertSubsetToGeneList(final IntSet subset) {
        final ObjectSet<String> reducedProbesetIds = convertSubsetToProbesetIds(subset);
        return
                new FixedGeneList(reducedProbesetIds.toArray(
                        new String[reducedProbesetIds.size()]));
    }

    private ObjectSet<String> convertSubsetToProbesetIds(final IntSet subset) {
        final ObjectSet<String> reducedProbesetIds =
                new ObjectOpenHashSet<String>();
        for (final int probesetIndex : subset) {

            final MutableString probesetId =
                    inputIdentifiers.getProbesetIdentifier(probesetIndex);
            if (probesetId != null) {
                reducedProbesetIds.add(probesetId.toString());
            } else {
                System.out.println("Cannot map: " + probesetId + "index: " + probesetIndex);
            }
        }
        return reducedProbesetIds;
    }
}
