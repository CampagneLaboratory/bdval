/*
 * Copyright (C) 2009-2010 Institute for Computational Biomedicine,
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
        this.coxP = c;
        this.hazardRatio = h;
        this.lowCI = lo;
        this.upCI = up;
        this.logRankP = logr;
    }
}
