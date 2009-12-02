/*
 * Copyright (C) 2009 Institute for Computational Biomedicine,
 *                    Weill Medical College of Cornell University
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

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.GEOPlatform;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.geo.tools.MicroarrayTrainEvaluate;
import edu.cornell.med.icb.tissueinfo.similarity.ScoredTranscriptBoundedSizeQueue;
import edu.cornell.med.icb.tissueinfo.similarity.TranscriptScore;
import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.Table;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.inference.TTest;
import org.apache.commons.math.stat.inference.TTestImpl;

import java.util.List;
import java.util.Set;

/**
 * Univariate feature selection with T-Test. We compare each feature individually for evidence of
 * differential distribution in the two prediction classes. If the T-Test p-Value is less than
 * the confidence interval, then the feature is selected as an informative biomarker.
 *
 * @author Nyasha Chambwe Date: Septermber 10, 2009 Time: 5:22:02 PM
 */
public class DiscoverWithHubs extends DAVMode {
    private static final Log LOG = LogFactory.getLog(DiscoverWithHubs.class);
    private boolean writeGeneListFormat;
    private int maxProbesToReport;
    private FeatureReporting reporter;
    private double alpha;
    final Object2DoubleMap<MutableString> probesetPvalues =
            new Object2DoubleOpenHashMap<MutableString>();

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        super.interpretArguments(jsap, result, options);

        alpha = result.getDouble("alpha");
        writeGeneListFormat = result.getBoolean("output-gene-list");
        reporter = new FeatureReporting(writeGeneListFormat);
        for (final GEOPlatform platform : options.platforms) {
            maxProbesToReport += platform.getProbesetCount();
        }
        if (result.contains("report-max-probes")) {
            maxProbesToReport = result.getInt("report-max-probes");
            LOG.info("Output will be restricted to the " + maxProbesToReport
                    + " probesets with smaller t-test result.");
        }
    }

    /**
     * Define command line options for this mode.
     *
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter numFeatureParam = new FlaggedOption("alpha")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault("0.05")
                .setRequired(true)
                .setLongFlag("alpha")
                .setShortFlag('n')
                .setHelp("The significance level for the univariate T-test p-value. "
                        + "Default 0.05/5% confidence level.");
        jsap.registerParameter(numFeatureParam);


        final Parameter outputGeneList = new Switch("output-gene-list")
                .setLongFlag("output-gene-list")
                .setHelp("Write features to the output in the tissueinfo gene list format.");
        jsap.registerParameter(outputGeneList);

        final Parameter reportMaxProbes = new FlaggedOption("report-max-probes")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setLongFlag("report-max-probes")
                .setHelp("Restrict output to the top ranked probes. This option works in "
                        + "conjunction with alpha and can further restrict the output.");
        jsap.registerParameter(reportMaxProbes);
    }

    @Override
    public void process(final DAVOptions options) {
        super.process(options);
        //TODO

        for (final ClassificationTask task : options.classificationTasks) {
            for (final GeneList geneList : options.geneLists) {
                final TTest tester = new TTestImpl();
                try {
                    System.out.println("Discover markers with Hubs " + task);
                    options.normalizeFeatures = false;
                    options.scaleFeatures = false;
                    final Table processedTable = processTable(geneList, options.inputTable,
                            options, MicroarrayTrainEvaluate.calculateLabelValueGroups(task));
                    final ArrayTable.ColumnDescription labelColumn =
                            processedTable.getColumnValues(0);
                    assert labelColumn.type
                            == String.class : "label must have type String";
                    final String[] labels = labelColumn.getStrings();

                    final List<Set<String>> labelValueGroups =
                            MicroarrayTrainEvaluate.calculateLabelValueGroups(task);

                    final ScoredTranscriptBoundedSizeQueue selectedProbesets =
                            new ScoredTranscriptBoundedSizeQueue(maxProbesToReport);
                    for (int featureIndex = 1; featureIndex < processedTable
                            .getColumnNumber(); featureIndex++) {

                        final int probesetIndex = featureIndex - 1;
                        final ArrayTable.ColumnDescription cd =
                                processedTable.getColumnValues(featureIndex);
                        assert cd.type == double.class : "features must have type double";
                        final double[] values = cd.getDoubles();
                        final DoubleList positiveLabelValues = new DoubleArrayList();
                        final DoubleList negativeLabelValues = new DoubleArrayList();
                        int index = 0;
                        for (final String label : labels) {
                            if (labelValueGroups.get(0).contains(label)) {
                                negativeLabelValues.add(values[index]);
                            } else {
                                if (labelValueGroups.get(1).contains(label)) {
                                    positiveLabelValues.add(values[index]);
                                }
                            }
                            index++;
                        }
                        double pValue = tester.tTest(
                                positiveLabelValues.toDoubleArray(),
                                negativeLabelValues.toDoubleArray());

                        if (pValue != pValue) {
                            // NaN
                            pValue = 1;
                        }

                        // save in probesetPValues map
                        final MutableString probesetId = options.getProbesetIdentifier(probesetIndex);
                        probesetPvalues.put(probesetId.copy().compact(), pValue);

                        if (pValue <= alpha) {
                            /* System.out.println("featureIndex:" + featureIndex);
                         final MutableString probesetId =
                                 options.getProbesetIdentifier(featureIndex - 1);
                         System.out.println("featureId:" + probesetId);
                         System.out.println("pValue:" + pValue);
                         System.out.println("positive: " + positiveLabelValues.toString());
                         System.out.println("negative: " + negativeLabelValues.toString());*/
                            // The queue keeps items with larger score. Transform the pValue accordingly.
                            selectedProbesets.enqueue(new TranscriptScore(1 - pValue, probesetIndex));
                        }
                    }
                    System.out.println("Selected " + selectedProbesets.size()
                            + " probesets at significance level=" + Double
                            .toString(alpha));
                    while (!selectedProbesets.isEmpty()) {
                        final TranscriptScore feature = selectedProbesets.dequeue();
                        final TranscriptScore probeset =
                                new TranscriptScore(feature.score, feature.transcriptIndex);

                        reporter.reportFeature(0, -(feature.score - 1), probeset, options);
                    }
                } catch (Exception e) {
                    LOG.fatal("Caught exception during T-test", e);
                    System.exit(10);
                }
            }
        }
    }
}
