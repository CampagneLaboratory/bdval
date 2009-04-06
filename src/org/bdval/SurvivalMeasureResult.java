package org.bdval;

/**
 * Created by IntelliJ IDEA.
 * User: xutao
 * Date: Jan 6, 2009
 * Time: 12:47:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class SurvivalMeasureResult {
    public double coxP;
    public double hazardRatio;
    public double lowCI;
    public double upCI;
    public double logRankP;

    public void assignValue(final double c, final double h, final double lo, final double up, final double logr)
    {
        this.coxP=c;
        this.hazardRatio=h;
        this.lowCI = lo;
        this.upCI = up;
        this.logRankP = logr;
    }
}
