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
import com.martiansoftware.jsap.Switch;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.GEOPlatform;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.geo.tools.MicroarrayTrainEvaluate;
import edu.cornell.med.icb.stat.MinMaxCalculator;
import edu.cornell.med.icb.tissueinfo.similarity.ScoredTranscriptBoundedSizeQueue;
import edu.cornell.med.icb.tissueinfo.similarity.TranscriptScore;
import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.Table;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Set;

/**
 * Feature selection with min/max statistics. For each feature, we determine the minimum and
 * maximum feature values in the positive and in the negative class. We refer to these values
 * as minL0, minL1, maxL0 and maxL1. We evaluate the quantity (maxL1-maxL0)/(minL1-minL0) and
 * rank features by this quantity.
 *
 * @author Fabien Campagne Date: Oct 23, 2007 Time: 3:04:42 PM
 */
public class DiscoverWithMinMax extends DAVMode {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(DiscoverWithMinMax.class);
    private boolean writeGeneListFormat;
    private int maxProbesToReport;
    private int minMaxObservations;

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        super.interpretArguments(jsap, result, options);

        ratio = result.getDouble("ratio");
        writeGeneListFormat = result.getBoolean("output-gene-list");
        reporter = new FeatureReporting(writeGeneListFormat);
        for (final GEOPlatform platform : options.platforms) {
            maxProbesToReport += platform.getProbesetCount();
        }
        if (result.contains("report-max-probes")) {
            maxProbesToReport = result.getInt("report-max-probes");
            System.out.println("Output will be restricted to the " + maxProbesToReport
                    + " probesets with larger absolute Kendall tau correlation.");
        }
        this.minMaxObservations = result.getInt("min-max-observations");
    }

    FeatureReporting reporter;

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        final Parameter numFeatureParam = new FlaggedOption("ratio")
                .setStringParser(JSAP.DOUBLE_PARSER)
                .setDefault(".5")
                .setRequired(true)
                .setLongFlag("ratio")
                .setHelp("The Min/Max value used as threshold. Default Min/Max>=5.");
        jsap.registerParameter(numFeatureParam);

        final Parameter kParam = new FlaggedOption("min-max-observations")
                       .setStringParser(JSAP.INTEGER_PARSER)
                       .setDefault("1")
                       .setLongFlag("min-max-observations")
                       .setHelp("The number of min/max elements to observe to compute Min/Max statistics.");
               jsap.registerParameter(kParam);

        final Parameter outputGeneList = new Switch("output-gene-list")
                .setLongFlag("output-gene-list")
                .setHelp("Write features to the output in the tissueinfo gene list format.");
        jsap.registerParameter(outputGeneList);
        final Parameter reportMaxProbes = new FlaggedOption("report-max-probes")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setLongFlag("report-max-probes")
                .setHelp("Restrict output to the top ranked probes. This option works in "
                        + "conjunction with ratio and can further restrict the output.");
        jsap.registerParameter(reportMaxProbes);
    }

    double ratio;

    @Override
    public void process(final DAVOptions options) {
        super.process(options);
        for (final ClassificationTask task : options.classificationTasks) {
            for (final GeneList geneList : options.geneLists) {
                try {
                    options.normalizeFeatures = false;
                    options.scaleFeatures = false;
                    final Int2DoubleMap ratios = new Int2DoubleOpenHashMap();
                    System.out.println("Discover markers with min/max statistics " + task);
                    final Table processedTable =
                            processTable(geneList, options.inputTable,
                                    options, MicroarrayTrainEvaluate.calculateLabelValueGroups(
                                    task));
                    final ArrayTable.ColumnDescription labelColumn =
                            processedTable.getColumnValues(0);
                    assert labelColumn.type
                            == String.class : "label must have type String";
                    final String[] labels = labelColumn.getStrings();

                    final List<Set<String>> labelValueGroups =
                            MicroarrayTrainEvaluate
                                    .calculateLabelValueGroups(task);
                    final double[] numericLabels = new double[labels.length];
                    int labelIndex = 0;
                    for (final String label : labels) {
                        numericLabels[labelIndex++] = labelValueGroups.get(0).contains(label) ? 0 : 1;
                    }
                    final ScoredTranscriptBoundedSizeQueue selectedProbesets =
                            new ScoredTranscriptBoundedSizeQueue(maxProbesToReport);
                    for (int featureIndex = 1; featureIndex < processedTable
                            .getColumnNumber(); featureIndex++) {

                        final int probesetIndex = featureIndex - 1;


                        final double[] trimedValues = processedTable.getDoubles(processedTable.getIdentifier(featureIndex));
                        final double[] trimedLabels = new double[processedTable.getRowNumber()];
                        System.arraycopy(numericLabels, 0, trimedLabels, 0, trimedLabels.length);

                        final double minMax = MinMaxCalculator.compute(trimedValues, trimedLabels, minMaxObservations);

                        if (minMax >= ratio) {
                            //System.out.println("pValue:" + pValue);
                            //System.out.println("positive: " + positiveLabelValues.toString());
                            //System.out.println("negative: " + negativeLabelValues.toString());
                            final MutableString probesetId =
                                    options.getProbesetIdentifier(probesetIndex);
                            LOG.debug("Selecting feature MinMax " + probesetId + " minMax= " + minMax);
                            selectedProbesets.enqueue(probesetIndex, minMax);
                            ratios.put(probesetIndex, minMax);
                        }
                    }
                    System.out.println("Selected " + selectedProbesets.size()
                            + " probesets with minMax >=" + Double.toString(ratio));
                    while (!selectedProbesets.isEmpty()) {
                        final TranscriptScore feature = selectedProbesets.dequeue();
                        final TranscriptScore probeset = new TranscriptScore(feature.score, feature.transcriptIndex);

                        reporter.reportFeature(0, ratios.get(feature.transcriptIndex), probeset, options);
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
