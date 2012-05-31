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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test our R-based KolmogorovSmirnovTest.
 * @author Kevin Dorff
 */
public class TestKolmogorovSmirnovTest {
    /**
     * Run the KS test test.
     */
    @Test
    public void testKSTest() {
        final double[] x = new double[] {0.1, 0.2, 0.3, 0.4, 0.5};
        final double[] y = new double[] {0.6, 0.7, 0.8, 0.9, 1.0};
        final KolmogorovSmirnovTestResult result = KolmogorovSmirnovTest.calculate(x, y);
        assertEquals(0.007937, result.getPValue(), 0.000001);
        assertEquals(1.0d, result.getTestStatistic(), 0.000001);
    }
}
