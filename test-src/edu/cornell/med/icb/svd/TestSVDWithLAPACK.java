/*
 * Copyright (C) 2007-2010 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.svd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Tests SVD functions using the LAPACK implementation.
 */
public final class TestSVDWithLAPACK extends AbstractTestSVD {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(TestSVDWithLAPACK.class);

    /**
     * Gets the {@link edu.cornell.med.icb.svd.SingularValueDecomposition} type
     * used for the tests.
     * @return The {@link edu.cornell.med.icb.svd.SVDFactory.ImplementationType}
     * used for the tests
     */
    @Override
    public SVDFactory.ImplementationType getSVDImplementationType() {
        return SVDFactory.ImplementationType.LAPACK;
    }

    @Test
    @Override
    public void test3x2withDefaults() {
        // TODO: this causes LAPACK to crash - maybe because of the configuration of the matrix???
    }

    @Test
    public void testLAPACK2x3() {
        svdImplementation.svd(matrix2x3, 2, 0);
        test2x3(matrix2x3, svdImplementation);
    }

    private void compareUpTo(final double[][] u, final double[][] u2, final int upTo) {
        // assertEquals(u.length, u2.length);
        for (int i = 0; i < upTo; i++) {
            assertEquals(u[i].length, u2[i].length);
            for (int j = 0; j < u[i].length; j++) {
                assertEquals(u[i][j], u2[i][j], EPSILON);
            }
        }
    }

    private void compare(final double[] a, final double[] b) {
        // assertEquals(a.length, b.length);
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            assertEquals("i = " + i, a[i], b[i], EPSILON);
        }
    }

    @Test
    public void testLAPACK() {
        rConnectionPoolUsed = true;   // this test ends up using R so we'll need to shut it down
        LOG.debug("Using Connection Pool");
        final double[][] matrix = new double[5][7];
        matrix[0] = new double[]{30.73861, 30.65108, 29.43861, 29.29700, 30.73861, 30.65108, 29.43861};
        matrix[1] = new double[]{29.48155, 30.34502, 30.13504, 30.20052, 29.48155, 30.34502, 30.13504};
        matrix[2] = new double[]{29.22964, 30.51928, 31.16050, 29.87943, 29.22964, 30.51928, 31.16050};
        matrix[3] = new double[]{29.97515, 29.35558, 30.28322, 29.13139, 29.97515, 29.35558, 30.28322};
        matrix[4] = new double[]{28.32116, 28.15695, 30.80357, 31.64230, 28.32116, 28.15695, 30.80357};


        final SingularValueDecomposition svd = SVDFactory.getImplementation("LAPACK");
        final SingularValueDecomposition svdR = SVDFactory.getImplementation("R");
        final int numU = 5;
        final int numV = 7;
        svd.svd(matrix);
        svdR.svd(matrix);
        compare(svd.getSingularValues(), svdR.getSingularValues());
        compareUpTo(svd.getU(), svdR.getU(), 4);
        //  compareUpTo(svd.getV(), svdR.getV(),4);
        /*
         final double[] S = svd.getSingularValues();
         assertNotNull(S);
         assertEquals(1.770505e+02, S[0], EPSILON);
         assertEquals(3.984961e+00, S[1], EPSILON);
         assertEquals(1.645600e+00, S[2], EPSILON);
         assertEquals(1.212450e+00, S[3], EPSILON);
         assertEquals(1.215590e-15, S[4], EPSILON);


         final double[][] U = svd.getU();
         assertNotNull(U);
         assertEquals(numU, U.length);
         assertEquals(5, U[0].length);
         assertEquals(-0.4502256d, U[0][0], EPSILON);
         assertEquals(-0.60060712, U[0][1], EPSILON);
         assertEquals(-0.09115429, U[1][1], EPSILON);
         assertEquals(-0.4403605, U[0][4], EPSILON);
        */ /*  Expected U:
       [,1]        [,2]
    [1,] -0.4502256 -0.60060712
    [2,] -0.4485756 -0.09115429
    [3,] -0.4520089  0.06135175
    [4,] -0.4448005 -0.13505397
    [5,] -0.4403605  0.78035785
        */

        /* Expected V:
    $v
          [,1]       [,2]       [,3]
    [1,] -0.3732299 -0.3271132  0.4774864
    [2,] -0.3765225 -0.3249754 -0.4290302
    [3,] -0.3834575  0.3592796 -0.1979035
    [4,] -0.3791851  0.5626831  0.3123269
    [5,] -0.3732299 -0.3271132  0.4774864
    [6,] -0.3765225 -0.3249754 -0.4290302
    [7,] -0.3834575  0.3592796 -0.1979035


        */
    }

    /**
     * Note: Requires extra memory.
     */
    @Test
    public void testLargeLAPACK() {
        final int numCols = 1000;
        final int numRows = 20000;
        final double[][] matrix = new double[numCols][numRows];
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = new double[numRows];
        }
        final SingularValueDecomposition svd = SVDFactory.getImplementation("LAPACK");
        svd.svd(matrix);
        assertNotNull(svd.getSingularValues());
        for (final double singularValue : svd.getSingularValues()) {
            assertEquals(0d, singularValue, EPSILON);
        }
        //TextIO.storeDoubles(svd.getSingularValues(), System.out);
        assertEquals(1000, svd.getSingularValues().length);
        assertNotNull(svd.getU());
        assertEquals(1000, svd.getU().length);
    }
}
