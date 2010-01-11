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

package org.bdval;

import cern.colt.Timer;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.geo.tools.MicroarrayTrainEvaluate;
import edu.cornell.med.icb.learning.ClassificationHelper;
import edu.cornell.med.icb.learning.ClassificationModel;
import edu.cornell.med.icb.learning.ClassificationProblem;
import edu.cornell.med.icb.learning.libsvm.LibSvmModel;
import edu.cornell.med.icb.learning.libsvm.LibSvmUtils;
import edu.cornell.med.icb.tissueinfo.similarity.ScoredTranscriptBoundedSizeQueue;
import edu.cornell.med.icb.tissueinfo.similarity.TranscriptScore;
import edu.mssm.crover.tables.Table;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Discover biomarkers with the SVM weight approach. A support vector machine is trained with a
 * linear kernel on the training set. Feature weights are then evaluated from the trained model,
 * and features with the n largest absolute value of the weight are identified as the most
 * important features (a.k.a. the biomarkers).
 *
 * @author Fabien Campagne Date: Oct 22, 2007 Time: 5:45:48 PM
 */
public class DiscoverWithSvmWeights extends DAVMode {
    private static final Log LOG = LogFactory.getLog(DiscoverWithSvmWeights.class);
     private int maxProbesToReport;
    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        super.interpretArguments(jsap, result, options);

        numProbesets = result.getInt("num-features");
        writeGeneListFormat = result.getBoolean("output-gene-list");
        reporter = new FeatureReporting(writeGeneListFormat);
        if (result.contains("report-max-probes")) {
            maxProbesToReport = result.getInt("report-max-probes");
            LOG.info("Output will be restricted to the " + maxProbesToReport
                    + " probesets with larger fold change.");
        }
    }

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter numFeatureParam = new FlaggedOption("num-features")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("400")
                .setRequired(false)
                .setLongFlag("num-features")
                .setShortFlag('n')
                .setHelp("Number of features to select.");
        jsap.registerParameter(numFeatureParam);

        final Parameter outputGeneList = new Switch("output-gene-list")
                .setLongFlag("output-gene-list")
                .setHelp("Write features to the output in the tissueinfo gene list format.");
        jsap.registerParameter(outputGeneList);

        final Parameter reportMaxProbes = new FlaggedOption("report-max-probes")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setLongFlag("report-max-probes")
                .setHelp("Restrict output to the top ranked probes. This option works in "
                        + "conjunction with the num-features and can further restrict the output.");
        jsap.registerParameter(reportMaxProbes);
    }

    int numProbesets = 50;
    boolean writeGeneListFormat;

    @Override
    public void process(final DAVOptions options) {
        process(options, true);
    }

    public void process(final DAVOptions options, final boolean writeGenes) {
        super.process(options);
        for (final ClassificationTask task : options.classificationTasks) {
            for (final GeneList geneList : options.geneLists) {

                try {
                    // force the classifier to be libSVM, irrespective of the command line:
                    options.classifierParameters=new String[0];
                    options.classiferClass=edu.cornell.med.icb.learning.libsvm.LibSvmClassifier.class;

                    System.out.println(
                            "Discover markers with SVM weights for " + task);
                    final Table processedTable =
                            processTable(geneList, options.inputTable,
                                    options, MicroarrayTrainEvaluate.calculateLabelValueGroups(
                                    task));
                    scaleFeatures(options, false, processedTable);
                    final ClassificationHelper helper = getClassifier(processedTable, MicroarrayTrainEvaluate.calculateLabelValueGroups(task));
                    final ClassificationProblem scaledProblem = helper.problem;
                    final Timer timer = new Timer();
                    timer.start();

                    final ClassificationModel model = helper.classifier.train(scaledProblem, helper.parameters);

                    timer.stop();

                    LOG.info("trained model in " + timer.seconds() + " seconds");

                    final double[] weights = LibSvmUtils.calculateWeights(((LibSvmModel) model).getNativeModel());
                    final ScoredTranscriptBoundedSizeQueue queue =
                            new ScoredTranscriptBoundedSizeQueue(numProbesets);
                    int probesetIndex = 0;
                    for (final double weight : weights) {
                        queue.enqueue(probesetIndex, Math.abs(weight));
                        probesetIndex++;
                    }
                    while (!queue.isEmpty()) {
                        final TranscriptScore probeset = queue.dequeue();
                        if (writeGenes) {
                            printFeature(probesetIndex, weights, probeset, options);
                        }
                    }

                    options.output.flush();
                } catch (Exception e) {
                    LOG.error(e);
                    e.printStackTrace();
                    System.exit(10);
                }
            }
        }
    }

    FeatureReporting reporter;

    protected void printFeature(final int iteration, final double[] weights,
                                final TranscriptScore probeset,
                                final DAVOptions options) {
        reporter.reportFeature(iteration, weights, probeset, options);
    }
}
