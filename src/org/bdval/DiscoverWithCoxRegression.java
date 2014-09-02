/*
 * Copyright (C) 2008-2010 Institute for Computational Biomedicine,
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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import javastat.survival.regression.CoxRegression;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Filter features by cox regression partial p-value (cox proportional hazard model).
 * <p/>
 * --survival &lt;survivial-datafile&gt;
 * This file contains survival data
 * In tab delimited table;
 * column 1 chipID match cids and tmm
 * column 2 time to event
 * column 3 censor with 1 as event 0 as censor
 * column 4 and beyond are all numerical covariates that will be included in the regression model
 */
public class DiscoverWithCoxRegression extends DAVMode {
    /**
     * Used to log debug and error messages.
     */
    private static final Log LOG = LogFactory.getLog(DiscoverWithCoxRegression.class);

    private int maxProbesToReport;
    private FeatureReporting reporter;
    private double alpha;

    double[] censor;
    double[] time;
    double[][] covariate; // such as age

    /**
     * The number of covariates.
     */
    int nCov;

    /**
     * Maps sample ID the index of each sample in the entire training set.
     */
    final Object2IntMap<String> sampleIndexMap = new Object2IntOpenHashMap<String>();

    private double coxTest(final double[] featureValues, final String[] labelIds) {
        // filter the survival information to keep only information corresponding to the current
        // split. (a more elegant approach would be to filter survival data when the survival
        // file is loaded. DAVMode.splitPlanContainsSampleId(sampleId) can be used to determine
        // if a sample Id should be read.)

        final int numberOfSamples = featureValues.length;
        final double[] censorThisSplit = new double[numberOfSamples];
        final double[] timeThisSplit = new double[numberOfSamples];
        final double[][] covariateThisSplit = new double[nCov][numberOfSamples];

        int j = 0;
        for (int sample = 0; sample < numberOfSamples; sample++) {
            final int i = sampleIndexMap.get(labelIds[sample]); // in split plan
            timeThisSplit[j] = time[i];
            censorThisSplit[j] = censor[i];
            for (int k = 0; k < nCov; k++) {
                covariateThisSplit[k][j] = covariate[k][i];
            }
            j++;
        }

        if (numberOfSamples != j) {
            LOG.fatal("Different array size in cox test featureValues size " + numberOfSamples
                    + " cov size " + j);
            System.exit(10);
        }

        final double[][] featureWithCovariate = new double[nCov + 1][numberOfSamples]; // = {featureValues, covariateThisSplit[0]};
        System.arraycopy(featureValues, 0, featureWithCovariate[0], 0, numberOfSamples);      //the [0] is feature, the following is covariates
        for (int k = 0; k < nCov; k++) {
            System.arraycopy(covariateThisSplit[k], 0, featureWithCovariate[k + 1], 0, numberOfSamples);
        }

        try {
            final CoxRegression testclass1 = new CoxRegression(alpha,
                    timeThisSplit, censorThisSplit, featureWithCovariate);
            final double[] pValue = testclass1.pValue;
            return pValue[0];
        } catch (RuntimeException e) {
            LOG.warn(e.getMessage());
            return 1;
        }
    }

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        super.interpretArguments(jsap, result, options);

        alpha = result.getDouble("alpha");
        final boolean writeGeneListFormat = result.getBoolean("output-gene-list");
        reporter = new FeatureReporting(writeGeneListFormat);
        for (final GEOPlatform platform : options.platforms) {
            maxProbesToReport += platform.getProbesetCount();
        }

        if (result.contains("report-max-probes")) {
            maxProbesToReport = result.getInt("report-max-probes");
            LOG.info("Output will be restricted to the " + maxProbesToReport
                    + " probesets with smaller Cox-Regression result.");
        }

        final String survivalFileName = result.getString("survival");
        try {
            readSurvival(survivalFileName);
        } catch (IOException e) {
            LOG.fatal("Cannot read input file \"" + options.input + "\"", e);
            System.exit(10);
        }
    }

    public void readSurvival(final String survivalFileName) throws IOException {
        // TODO: Xutao, a better way to parse tab delimited files:

//              TSVReader tabDelimitedParser=new TSVReader(new FileReader(survivalFileName));
//                tabDelimitedParser.setCommentPrefix("#");
//              while ( tabDelimitedParser.hasNext())    {
//                  tabDelimitedParser.next();
//                 String sampleId= tabDelimitedParser.getString();
//                 double covariate1= tabDelimitedParser.getDouble();
//              }

        final ObjectList<String> sampleIDList = new ObjectArrayList<String>();
        final DoubleList timeList = new DoubleArrayList();
        final DoubleList censorList = new DoubleArrayList();
        final ObjectList<DoubleList> covariateList = new ObjectArrayList<DoubleList>();

        LOG.info("Reading survival files");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(survivalFileName));
            String line = reader.readLine(); // get rid of header
            String[] linearray = line.toUpperCase().split("\t");

            // number of covariates equals total columns minus ID, time, and censor
            nCov = linearray.length - 3;

            for (int i = 0; i < nCov; i++) {
                final DoubleList tmp = new DoubleArrayList(); //empty array
                covariateList.add(tmp);
            }
            LOG.info("number of parsed covariates " + nCov);
            int i = 0;
            while ((line = reader.readLine()) != null) {
                linearray = line.split("\t");
                sampleIDList.add(linearray[0]);            //chipId
                timeList.add(Double.parseDouble(linearray[1])); //time to event
                censorList.add(Double.parseDouble(linearray[2])); //censor
                for (int j = 0; j < nCov; j++) {
                    covariateList.get(j).add(Double.parseDouble(linearray[j + 3]));
                }
                sampleIndexMap.put(linearray[0], i);
                i++;
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        // turn lists to arrays
        final int size = sampleIDList.size();
        time = timeList.toArray(new double[size]);
        censor = censorList.toArray(new double[size]);
        covariate = new double[nCov][size];
        for (int j = 0; j < nCov; j++) {
            covariate[j] = covariateList.get(j).toArray(new double[size]);
        }
        LOG.info("Reading survival Files done, sample size is   "
                + time.length + "sampleIndexMap size " + sampleIndexMap.size());
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
                .setHelp("The significance level for the univariate cox regression-test p-value. "
                        + "Default 0.05/5% confidence level.");
        jsap.registerParameter(numFeatureParam);

        final Parameter survivalFilenameOption = new FlaggedOption("survival")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("survival")
                .setHelp("Survival filename. This file contains survival data "
                        + "in tab delimited table; column 1: chipID has to match cids and "
                        + "tmm, column 2: time to event, column 3 censor with 1 as event 0 "
                        + "as censor, column 4 and beyond are all numerical covariates that "
                        + "will be included in the regression model");
        jsap.registerParameter(survivalFilenameOption);

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
        double minPvalue = 1.0;
        int minPvalueProbleIndex = 0;
        for (final ClassificationTask task : options.classificationTasks) {
            for (final GeneList geneList : options.geneLists) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Discover markers with Cox-regression-Test " + task);
                    }
                    options.normalizeFeatures = false;
                    options.scaleFeatures = false;
                    final Table processedTable = processTable(geneList, options.inputTable,
                            options, MicroarrayTrainEvaluate.calculateLabelValueGroups(task));

                    final ArrayTable.ColumnDescription labelColumn =
                            processedTable.getColumnValues(0);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(ArrayUtils.toString(labelColumn.getStrings()));
                    }

                    assert labelColumn.type == String.class : "label must have type String";

                    final ScoredTranscriptBoundedSizeQueue selectedProbesets =
                            new ScoredTranscriptBoundedSizeQueue(maxProbesToReport);

                    for (int featureIndex = 1; featureIndex < processedTable.getColumnNumber();
                         featureIndex++) {

                        final int probesetIndex = featureIndex - 1;
                        final ArrayTable.ColumnDescription cd =
                                processedTable.getColumnValues(featureIndex);
                        assert cd.type == double.class : "features must have type double";
                        final double[] values = cd.getDoubles();

                        final double pValue = coxTest(values, labelColumn.getStrings());

                        if (pValue < minPvalue) {
                            minPvalue = pValue;
                            minPvalueProbleIndex = probesetIndex;
                        }
                        if (pValue <= alpha) {
                            // The queue keeps items with larger score. Transform the pValue accordingly.
                            selectedProbesets.enqueue(new TranscriptScore(1 - pValue, probesetIndex));
                        }
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Selected " + selectedProbesets.size()
                                + " probesets at significance level = " + alpha);
                    }
                    if (selectedProbesets.isEmpty()) {
                        LOG.warn("no probeset pass test - adding the probeset with min PValue");
                        selectedProbesets.enqueue(new TranscriptScore(1 - minPvalue,
                                minPvalueProbleIndex));
                    }

                    while (!selectedProbesets.isEmpty()) {
                        final TranscriptScore feature = selectedProbesets.dequeue();
                        final TranscriptScore probeset =
                                new TranscriptScore(feature.score, feature.transcriptIndex);

                        reporter.reportFeature(0, -(feature.score - 1), probeset, options);
                    }
                } catch (Exception e) {
                    LOG.fatal("Caught exception during Cox-Regression", e);
                    System.exit(10);
                }
            }
        }
    }
}
