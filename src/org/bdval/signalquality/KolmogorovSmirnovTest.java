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

package org.bdval.signalquality;

import edu.cornell.med.icb.R.script.RDataObjectType;
import edu.cornell.med.icb.R.script.RScript;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the Kolmogorov-Smirnov Test using the RServe.
 * @author Kevin Dorff
 */
public class KolmogorovSmirnovTest {

    /**
     * The logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(KolmogorovSmirnovTest.class);

    private KolmogorovSmirnovTest() {
    }

    /**
     * Calculates Kolmogorov-Smirnov for the given doubles[]'s xA and xB.
     * @param x the first double[]
     * @param y the first double[]
     * @return the the result values
     */
    public static KolmogorovSmirnovTestResult calculate(final double[] x, final double[] y) {
        try {
            final RScript rscript = RScript.createFromResource("rscripts/KS_test.R");
            rscript.setInput("x", x);
            rscript.setInput("y", y);
            rscript.setOutput("p_value", RDataObjectType.Double);
            rscript.setOutput("test_statistic", RDataObjectType.Double);
            rscript.execute();
            return new KolmogorovSmirnovTestResult(
                    rscript.getOutputDouble("p_value"),
                    rscript.getOutputDouble("test_statistic"));
        } catch (Exception e) {
            LOG.warn(String.format(
                    "Cannot calculate KolmogorovSmirnovTest for x=%s, y=%s",
                    ArrayUtils.toString(x), ArrayUtils.toString(y)), e);
            return null;
        }
    }
}
