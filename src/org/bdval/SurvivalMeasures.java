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

import edu.cornell.med.icb.R.script.RDataObjectType;
import edu.cornell.med.icb.R.script.RScript;
import edu.cornell.med.icb.learning.CrossValidation;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import javastat.survival.regression.CoxRegression;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RserveException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SurvivalMeasures {
    public static void main(final String[] args) throws IOException {  //test survivalMeasures
        final DoubleList decisions = new DoubleArrayList ();
        decisions.add(1); decisions.add(0.2); decisions.add(-0.3); decisions.add(-2.3);
        final DoubleList trueLabel = new DoubleArrayList ();
        trueLabel.add(1); trueLabel.add(2); trueLabel.add(3);trueLabel.add(4);
        final String[] sampleID = {"111", "222", "333", "444"};
        final SurvivalMeasures s = new SurvivalMeasures("C:\\CLINICAL\\norm-data\\survivalTest.txt", decisions, trueLabel, sampleID );
        final SurvivalMeasures b = new SurvivalMeasures();
        final double[] time={0.6,8.0,214.5,169.5,175.7,56.1,9.8,24.7,88.6,3.1,7.8,16.9,3.5,92.3,15.8,1.5,4.2,17.4,108.6,5.6,8.2,0.7,15.7,40.8,0.2,1.5,61.2,19.7,158.8,32.3,22.6,33.7,60.5,5.7,8.5,36.7,26.9,138.3,12.0,59.6,13.5,3.3,10.4,10.0,64.7,17.2,5.9,102.9,12.9,2.0,2.0,127.3,125.2,16.6,27.5,2.8,104.4,10.9,106.6,14.4,0.7,70.8,19.2,6.3,18.2,7.8,6.2,17.9,51.4,6.3,3.1,9.5,9.2,56.7,83.1,113.5,19.2,58.9,15.3,39.4,12.5,69.1,1.5,210.9,6.9,0.3,6.9,18.0,143.6,120.5,109.7,19.0,21.9,12.3,13.5,8.8,131.9,17.4,63.6,0.1,57.8,9.2,38.2,2.4,2.3,0.8,39.3,47.8,9.0,37.8,66.3,6.2,1.1,2.8,12.6,0.7,3.8,0.5,30.6,2.2,8.9,13.4,6.6,1.5,20.0,16.2,17.0,10.3,13.0,145.4,99.3,21.1,0.1,7.4,42.1,124.0,4.9,45.1,2.4,10.7,4.0,54.6,44.2,65.5,75.4,11.3,12.9,3.8,10.6,63.4,37.0,6.9,10.6,60.5,64.3,2.4,4.2,11.6,12.4,7.7,4.1,2.9,11.9,21.7,84.4,8.4,1.1,7.6,4.9,6.6,10.1,0.4,110.9,85.9,62.1,8.8,9.3,24.1,86.3,4.5,24.1,10.3,0.1,13.0,73.6,6.9,5.6,53.9,8.3,6.9,56.2,44.8,42.4,85.8,87.4,0.7,60.5,20.1,8.4,35.4,9.2,26.0,38.5,10.9,23.4};
        final double[]censor={1.0,1.0,0.0,0.0,0.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,0.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,0.0,0.0,1.0,1.0,1.0,0.0,1.0,0.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,0.0,0.0,0.0,1.0,0.0,1.0,0.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,0.0,0.0,0.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,0.0,1.0,0.0,1.0,0.0,1.0,1.0,1.0,0.0,0.0,1.0,0.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,0.0,0.0,0.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,0.0,1.0,1.0,0.0,0.0,0.0,1.0,0.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0};
        final double[] group={-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,1.0,1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,1.0,-1.0,-1.0,-1.0,1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,1.0,-1.0,-1.0,1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0,-1.0,1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,1.0,-1.0};
        try {
            b.logRankByRscript(time, censor, group);
        } catch (RserveException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (REXPMismatchException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (REngineException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.out.println(s.logRankP);

        final double[] pval = s.coxP;
        final double logpval = s.logRankP;
        final double[] coef = s.hazardRatio;
        System.out.println("log rank p " + logpval);
        for(int i=0; i<pval.length; i++){
            System.out.println("pval:\t"+i+"\t"+pval[i]);
            System.out.println("coef:\t"+i+"\t"+Math.pow(2.71828,coef[i]));
        }
        System.out.println("R2 "+s.R2);

    }
    private static final Log LOG = LogFactory.getLog(SurvivalMeasures.class);
    private final DiscoverWithCoxRegression cox = new DiscoverWithCoxRegression();
    public String survivalFilename;
    public double [] coxP;
    public double R2;
    public double [] hazardRatio;
    public double [] lowCI;
    public double [] upCI;

    public double logRankP;

    IntList binaryDecisions = new IntArrayList(); //coverting input decisions to binary decisions
    //the filtered data by comparing survival and predictions table
    private final DoubleList decisionList =new DoubleArrayList();
    private final DoubleList binaryDecisionList =new DoubleArrayList();
    private final DoubleList censorList =new DoubleArrayList();
    private final DoubleList timeList=new DoubleArrayList() ;
    private final List <DoubleList> covariateList=new ArrayList <DoubleList>();
    private int nCov;

    public SurvivalMeasures(){}
    public SurvivalMeasures(final String survivalFilename, final DoubleList decisions,
                            final DoubleList trueLabels, final String [] sampleID) throws IOException {
        binaryDecisions = CrossValidation.convertBinaryLabels(decisions); //group -1, 1 for logRank
        cox.readSurvival(survivalFilename);
        nCov=cox.nCov;
        filterSamples(sampleID, decisions);
        calculateStat();
    }


    /**
     *
     * @param sampleID contains all samples in the predict samples
     * @param decisions
     */
    private void filterSamples(final String[] sampleID, final DoubleList decisions) {
        // filter the survival samples with the predict samples

        for (int k = 0; k < nCov; k++) {
            final DoubleList tmp = new DoubleArrayList ();
            covariateList.add(tmp);
        }
        int j = 0;
//        for(Map.Entry <String, Integer> e: cox.sampleIndexMap.entrySet()){
//            LOG.info("sampleIndexmap "+e.getKey()+" value "+e.getValue());
//        }
        for (int s = 0; s < sampleID.length; s++) {
            //    LOG.info("sampleID["+s+"]= "+sampleID[s]);
            if(!cox.sampleIndexMap.containsKey(sampleID[s])) {
                continue;
            }
            final int i = cox.sampleIndexMap.get(sampleID[s]); //map contains all samples read from survival
            decisionList.add(decisions.getDouble(s)); //get decisions scroe for sampleID[s]
            binaryDecisionList.add(binaryDecisions.getInt(s));
            timeList.add(cox.time[i]);
            censorList.add(cox.censor[i]);
            for (int k = 0; k < nCov; k++) {
                covariateList.get(k).add( cox.covariate[k][i]);
            }
            j++;
        }

        if (sampleID.length > timeList.size()) {
            LOG.warn("Not all test samples found survival data " +cox.time.length+"\t"+ sampleID.length + " cov size " + timeList.size());
        }
        covariateList.add(decisionList); //This last dimension is the dimension score
    }

    public void calculateStat() {
        final double alpha = 0.05;
        double[] time = null;
        double[] censor = null;
        double[] group = null;
        try {
            final int numSamples = timeList.size();
            time = timeList.toArray(new double[numSamples]);
            censor = censorList.toArray(new double[numSamples]);
            group = binaryDecisionList.toArray(new double[numSamples]);
            final double[] decisionScore = decisionList.toArray(new double [numSamples]);
            final double [][]covariateWithScore=new double [nCov+1][numSamples];

            for (int k = 0; k < nCov+1; k++) {
                final double [] arr= covariateList.get(k).toArray(new double [numSamples]);
                System.arraycopy(arr, 0, covariateWithScore[k], 0, numSamples); //turn 2D list into  2Darray
            }

            coxRegressionByJava(alpha, time, censor, covariateWithScore);
            //     coxRegressionByRScript(time, censor, covariateWithScore);

            logRankByRscript(time, censor, group);

        } catch (Exception e) {
            LOG.warn(String.format(
                    "Cannot calculate stats for time=%s, censor=%s,group=%s, size=%d, size=%d, size=%d",
                    ArrayUtils.toString(time), ArrayUtils.toString(censor), ArrayUtils.toString(group), time.length, censor.length, group.length ), e);
        }
    }
    public void logRankByJava() {
        //todo:
    }

    public void logRankByRscript(final double[] time, final double[] censor, final double [] group) throws RserveException, REXPMismatchException, REngineException {
        RScript rscript = null;
        try {
            rscript = RScript.createFromResource("rscripts/logRank_test.R");
        } catch(IOException e) {
            LOG.warn(e.getMessage());
        }

        rscript.setInput("time", time);
        rscript.setInput("censor", censor);
        rscript.setInput("group", group);
        rscript.setOutput("p_value", RDataObjectType.Double);
        rscript.execute();
        logRankP=1.0;
        logRankP=rscript.getOutputDouble("p_value");
    }
    private void coxRegressionByRScript( final double[] time, final double[] censor, final double[][] covariateWithScore) throws RserveException, REXPMismatchException, REngineException {
        RScript rscript=null;
        try{
            rscript = RScript.createFromResource("rscripts/cox_regression.R");
        }catch(IOException e){LOG.warn(e.getMessage());}
//        System.out.println("time=" + ArrayUtils.toString(time));
//        System.out.println("censor=" + ArrayUtils.toString(censor));
//        System.out.println("cov1=" + ArrayUtils.toString(covariateWithScore[0]));
//        System.out.println("cov2=" + ArrayUtils.toString(covariateWithScore[1]));
        rscript.setInput("time", time);
        rscript.setInput("censor", censor);
        rscript.setInput("cov1", covariateWithScore[0]);
        rscript.setInput("cov2", covariateWithScore[1]);
        rscript.setOutput("p_value", RDataObjectType.DoubleArray);
        rscript.setOutput("coef", RDataObjectType.DoubleArray);
        rscript.setOutput("R2", RDataObjectType.Double);
        rscript.execute();
        coxP = rscript.getOutputDoubleArray("p_value");
        hazardRatio = rscript.getOutputDoubleArray("coef");
        R2 = rscript.getOutputDouble("R2");
        System.out.println("R2  "+R2);
    }

    private void coxRegressionByJava(final double alpha, final double[] time, final double[] censor, final double[][] covariateWithScore) {
        try {
            LOG.info("In coxRegressionByJava: the sample size is:"+ time.length);
            final CoxRegression coxReg = new CoxRegression(alpha,
                    time,
                    censor,
                    covariateWithScore);
            lowCI = new double[nCov+1];
            upCI = new double[nCov+1];
            for (int k = 0; k < lowCI.length; k++){
                lowCI[k] = coxReg.confidenceInterval[k][0];
                upCI[k] = coxReg.confidenceInterval[k][1];
            }
            coxP = coxReg.pValue;
            hazardRatio= coxReg.coefficients;
        } catch (RuntimeException e) {
            LOG.warn(e.getMessage());
        }
    }
}
