/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

package org.bdval.signalquality;

import edu.cornell.med.icb.R.script.RDataObjectType;
import edu.cornell.med.icb.R.script.RScript;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RserveException;

import java.io.IOException;

/**
 * @author Kevin Dorff
*         Date: Nov 22, 2008
*         Time: 4:49:04 PM
*/
class ComputeRatioRank {
    private double[] pValues;
    private double[] t1Values;
    private double[] t2Values;
    private double[] stoufferVals;
    private double[] fisherVals;
    private double ratioRankVal;

    public ComputeRatioRank(final double[] pValues, final double[] t1Values, final double[] t2Values) {
        super();
        this.pValues = pValues;
        this.t1Values = t1Values;
        this.t2Values = t2Values;
    }

    public double[] getStoufferVals() {
        return stoufferVals;
    }

    public double[] getFisherVals() {
        return fisherVals;
    }

    public double getRatioRankVal() {
        return ratioRankVal;
    }

    /** The script used when writing the second, quality value file. */
    private static RScript pvalueToQualityScript = null;

    public synchronized ComputeRatioRank invoke() throws IOException, RserveException, REXPMismatchException, REngineException {
        // Calculate signal quality using the given features
        if (pvalueToQualityScript == null) {
            pvalueToQualityScript = RScript.createFromResource("rscripts/pvalue_to_quality.R");
        }

        pvalueToQualityScript.setInput("data_in", pValues);
        pvalueToQualityScript.setOutput("stouffer_vals", RDataObjectType.DoubleArray);
        pvalueToQualityScript.setOutput("fisher_vals", RDataObjectType.DoubleArray);

        pvalueToQualityScript.setInput("x", t1Values);
        pvalueToQualityScript.setInput("y", t2Values);
        pvalueToQualityScript.setOutput("ratio_rank_val", RDataObjectType.Double);

        pvalueToQualityScript.execute();

        stoufferVals = pvalueToQualityScript.getOutputDoubleArray("stouffer_vals");
        fisherVals = pvalueToQualityScript.getOutputDoubleArray("fisher_vals");
        ratioRankVal = pvalueToQualityScript.getOutputDouble("ratio_rank_val");
        return this;
    }
}
