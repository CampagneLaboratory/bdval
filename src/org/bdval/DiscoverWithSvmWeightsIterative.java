/*
 * Copyright (C) 2007-2009 Institute for Computational Biomedicine,
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

import cern.colt.Timer;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.geo.GEOPlatformIndexed;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.FixedGeneList;
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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Discover biomarkers with the iterative SVM weight approach. A support vector machine is trained with a linear kernel
 * on the training set. Feature weights are then evaluated from the trained model, and features with the N-k largest
 * absolute value of the weight are identified as the features to use in the next round. k is taken such that
 * (N-k/N)=ratio, typically 50%. The process starts over until the number N-k falls below or equal to the desired number
 * of biomarkers (n). When the condition is met, the the n features with the largest absolute weight are written out.
 *
 * @author Fabien Campagne Date: Oct 22, 2007 Time: 6:55:57 PM
 */
public class DiscoverWithSvmWeightsIterative extends DiscoverWithSvmWeights {
    private static final Log LOG = LogFactory.getLog(DiscoverWithSvmWeightsIterative.class);
    private double ratio = 0.5d;

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        super.defineOptions(jsap);
        final Parameter ratioParam = new FlaggedOption("ratio")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(".5")
                .setRequired(true)
                .setLongFlag("ratio")
                .setShortFlag('r')
                .setHelp("The ratio of new number of feature to original number of features,"
                        + " for each iteration.");
        jsap.registerParameter(ratioParam);
    }

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        super.interpretArguments(jsap, result, options);
        ratio = result.getDouble("ratio");
    }

    @Override
    public void process(final DAVOptions options) {
        super.process(options, false);
        int reducedNumProbeset;
        for (final ClassificationTask task : options.classificationTasks) {
            for (GeneList geneList : options.geneLists) {
                int iteration = 1;
                try {
                    int numKept;
                    final List<TranscriptScore> probesetsToOutput =
                            new LinkedList<TranscriptScore>();
                    double[] weights = null;
                    do {
                        System.out.println("Discover markers with SVM weights for " + task);
                        options.trainingPlatform = new GEOPlatformIndexed();
                        final Table processedTable =
                                processTable(geneList, options.inputTable, options,
                                        MicroarrayTrainEvaluate.calculateLabelValueGroups(task));
                        numKept = options.trainingPlatform.getProbeIds().size();
                        reducedNumProbeset = Math.max(numProbesets,
                                (int) (numKept * ratio));

                        scaleFeatures(options, false, processedTable);

                        final ClassificationHelper helper = getClassifier(processedTable,
                                MicroarrayTrainEvaluate.calculateLabelValueGroups(task));
                        final ClassificationProblem scaledProblem = helper.problem;
                        final Timer timer = new Timer();
                        timer.start();

                        final ClassificationModel model =
                                helper.classifier.train(scaledProblem, helper.parameters);

                        timer.stop();

                        LOG.info("trained model in " + timer.seconds() + " seconds");
                        weights = LibSvmUtils.calculateWeights(((LibSvmModel) model).getNativeModel());
                        System.out.println(
                                "input feature number: " + weights.length);
                        final ScoredTranscriptBoundedSizeQueue queue =
                                new ScoredTranscriptBoundedSizeQueue(
                                        reducedNumProbeset);
                        int probesetIndex = 0;
                        for (final double weight : weights) {
                            queue.enqueue(probesetIndex, Math.abs(weight));
                            probesetIndex++;
                        }
                        final ObjectSet<String> reducedProbesetIds =
                                new ObjectOpenHashSet<String>();
                        probesetsToOutput.clear();
                        while (!queue.isEmpty()) {
                            final TranscriptScore probeset = queue.dequeue();
                            final MutableString probesetId =
                                    options.getProbesetIdentifier( probeset.transcriptIndex);
                            if (reducedNumProbeset <= numProbesets) {
                                probesetsToOutput.add(probeset);
                            }
                            reducedProbesetIds.add(probesetId.toString());
                        }
                        //       System.out.println("POTENTIAL probesets to write "
                        //            + probesetsToOutput.size());
                        System.out.println("numKept: " + numKept
                                + " reducedNumber: " + reducedNumProbeset);
                        iteration++;
                        // use as gene list for the next iteration: the restriction of the previous gene list
                        // to those probesets that were found with largest weight in this iteration.
                        geneList =
                                new FixedGeneList(reducedProbesetIds.toArray(
                                        new String[reducedProbesetIds.size()]));
                        options.output.flush();
                    } while (numKept > numProbesets);

                    // Write probesetsToOutput. After the last iteration.
                    System.out.println("Number of probesets to write " + probesetsToOutput.size());
                    for (final TranscriptScore probeset : probesetsToOutput) {
                        printFeature(iteration, weights, probeset, options);
                    }

                } catch (Exception e) {
                    LOG.error(e);
                    e.printStackTrace();
                    System.exit(10);
                }
            }
        }
    }
}
