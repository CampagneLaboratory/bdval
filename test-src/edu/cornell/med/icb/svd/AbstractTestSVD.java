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

package edu.cornell.med.icb.svd;

import edu.cornell.med.icb.R.RConnectionPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Base class for SVD tests.
 */
public abstract class AbstractTestSVD {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(AbstractTestSVD.class);

    /**
     * Tolerance for floating point comparisons.
     */
    protected static final double EPSILON = 1e-4;

    /**
     * Simple matrix used for testing.
     */
    protected final double[][] matrix2x3 = {
            {1d, 2d, 3d},
            {4d, 5d, 6d}
    };

    /**
     * Simple matrix used for testing.
     */
    protected final double[][] matrix3x2 = {
           {1d, 4d},
           {2d, 5d},
           {3d, 6d}
    };

    /**
     * {@link edu.cornell.med.icb.svd.SingularValueDecomposition} implementation
     * used during the tests.
     */
    protected SingularValueDecomposition svdImplementation;

    /**
     * Used to indicate that the R was used during the test and needs to be shutdown.
     */
    protected static boolean rConnectionPoolUsed;

    /**
     * Expected S values for the test 2x3 matrix.
     */
    protected final double[] expected2x3S = {9.508032000695724d, 0.7728696356734842d};

    /**
     * Expected U values for the test 2x3 matrix.
     */
    protected final double[][] expected2x3U = {
            {-0.4286671, -0.5663069, -0.7039467},
            {0.8059639, 0.1123824, -0.5811991}
    };

    /**
     * Expected S values for the test 2x3 matrix.
     */
    protected final double[] expected3x2S = {9.508032000695724d, 0.7728696356734842d};

    /**
     * Expected U values for the test 2x3 matrix.
     */
    protected final double[][] expected3x2U = {
            {-0.4286671, 0.8059639},
            {-0.5663069, 0.1123824},
            {-0.7039467, -0.5811991}
    };

    /**
     * Initialize the {@link edu.cornell.med.icb.svd.SingularValueDecomposition} to
     * be used during the tests.
     */
    @Before
    public final void initializeSVD() {
        svdImplementation = SVDFactory.getImplementation(getSVDImplementationType());
    }

    /**
     * Ensure that the RConnection pool gets shutdown.  This would typically happen
     * in the {@link edu.cornell.med.icb.R.RConnectionPool#finalize()} method but we
     * can't be guaranteed that this will get called so we explicitly shut it down.
     */
    @AfterClass
    public static void shutdownPool() {
        if (rConnectionPoolUsed) {
            LOG.debug("Shutting down connection pool");
            final RConnectionPool pool = RConnectionPool.getInstance();
            pool.close();
        }
    }

    /**
     * Gets the {@link edu.cornell.med.icb.svd.SingularValueDecomposition} type
     * used for the tests.
     * @return The {@link edu.cornell.med.icb.svd.SVDFactory.ImplementationType}
     * used for the tests
     */
    public abstract SVDFactory.ImplementationType getSVDImplementationType();

    /**
     * Runs tests using the default method to compute the svd of a 2x3 matrix.
     */
    @Test
    public void test2x3withDefaults() {
        svdImplementation.svd(matrix2x3);
        testRank(svdImplementation, expected2x3S.length);
        testS(svdImplementation, expected2x3S);
        testU(svdImplementation, expected2x3U);
        testV(svdImplementation);
    }

    /**
     * Runs tests using the default method to compute the svd of a 2x3 matrix.
     */
    @Test
    public void test3x2withDefaults() {
        svdImplementation.svd(matrix3x2);
        testRank(svdImplementation, expected3x2S.length);
        testS(svdImplementation, expected3x2S);
// TODO        testU(svdImplementation, expected3x2U);
// TODO        testV(svdImplementation);
    }

    protected void test2x3(final double[][] matrix, final SingularValueDecomposition svd) {
        testRank(svd, expected2x3S.length);
        testS(svd, expected2x3S);

        final double[][] U = testU(svd, expected2x3U);
        final double[][] V = svd.getV();
        assertNull(V);

        final SingularValueDecomposition svd2 = SVDFactory.getImplementation("R");
        svd2.svd(matrix, 1, 1);
        assertNotNull(svd2.getSingularValues());
        assertEquals(2, svd2.getSingularValues().length);
        assertNotNull(svd2.getU());
        assertEquals(1, svd2.getU().length);
        assertEquals(3, svd2.getU()[0].length);
        assertEquals(-0.4286671d, U[0][0], EPSILON);
        assertEquals(-0.5663069d, U[0][1], EPSILON);

        /* Expected V: $v
                   [,1]
        [1,] -0.3863177
        [2,] -0.9223658
        */
        final double[][] V2 = svd2.getV();
        assertNotNull(V2);
        assertEquals(-0.3863177d, V2[0][0], EPSILON);
        assertEquals(-0.9223658d, V2[0][1], EPSILON);
    }

    /**
     * Validate the U vectors.
     * @param svd The svd results
     * @param expectedU The expected U vectors
     * @return
     */
    private double[][] testU(final SingularValueDecomposition svd, final double[][] expectedU) {
        final double[][] u = svd.getU();
        assertNotNull("U vector must not be null", u);
        assertTrue("There should be at least " + expectedU.length + " U vectors, but there are " + u.length,
                u.length >= expectedU.length);
        assertEquals("There should be " + expectedU[0].length + " elements in each U vector",
                expectedU[0].length, u[0].length);

        for (int i = 0; i < expectedU.length; i++) {
            for (int j = 0; j < expectedU[i].length; j++) {
                assertEquals("Entry at (" + i + ", " + j + ") does not match expected",
                        expectedU[i][j], u[i][j], EPSILON);
            }
        }

        return u;
    }

    private void testV(final SingularValueDecomposition svd) {
        final double[][] v = svd.getV();
        assertNotNull("V vector must not be null", v);
        assertEquals(2, v.length);
        assertEquals(2, v[0].length);

        /* Expected V: $v
                   [,1]       [,2]
        [1,] -0.3863177 -0.9223668
        [2,] -0.9223658  0.3863177
        */

        assertEquals(-0.3863177d, v[0][0], EPSILON);
        assertEquals(-0.9223658d, v[0][1], EPSILON);
        assertEquals(-0.9223668d, v[1][0], EPSILON);
        assertEquals(0.3863177d, v[1][1], EPSILON);
    }

    /**
     * Validates the rank computed for the 2x3 matrix example.
     * @param svd The SingularValueDecomposition instance to check.
     * @param expectedSLength Length of the expected "S" computed values.
     */
    private void testRank(final SingularValueDecomposition svd, final int expectedSLength) {
        assertEquals("The rank should be", expectedSLength, svd.rank());
    }

    /**
     * Validates the "S" computed values for the 2x3 matrix example.
     * @param svd The SingularValueDecomposition instance to check.
     * @param expectedS The expected "S" computed values.
     */
    private void testS(final SingularValueDecomposition svd, final double[] expectedS) {
        final double[] s = svd.getSingularValues();
        assertNotNull("There should be an S vector", s);
        assertEquals("There should be " + expectedS.length + " elements in S", expectedS.length, s.length);
        for (int i = 0; i < expectedS.length; i++) {
            assertEquals("S[" + i + "] is not correct", s[i], expectedS[i], EPSILON);
        }
    }

    /*  public void testDragonNNMF() {
    double[][] matrix = new double[2][3];
    matrix[0] = new double[]{1d, 2d, 3d};
    matrix[1] = new double[]{4d, 5d, 6d};


    SingularValueDecomposition svd = SVDFactory.getImplementation("DRAGON_NNMF:100");
    svd.svd(matrix, 2, 0);
    assertNotNull(svd.getSingularValues());
    //TextIO.storeDoubles(svd.getSingularValues(), System.out);
    assertEquals(2, svd.getSingularValues().length);
    assertNotNull(svd.getU());
    for (int c = 0; c < svd.getU().length; c++) {
        System.out.println("U[" + c + "]");
        TextIO.storeDoubles(svd.getU()[c], System.out);
    }
    assertEquals(2, svd.getU().length);
}
       */

/*
public void testLAPACKvsColt() {
double[][] matrix = new double[2][3];         // two columns, three rows
matrix[0] = new double[]{1d, 2d, 3d};
matrix[1] = new double[]{4d, 5d, 6d};

// matrix:
/*
1  4
2  5
3  6
*/  /*
        assertEquals(1, matrix[0][0], EPSILON);
        assertEquals(4, matrix[1][0], EPSILON);
        assertEquals(2, matrix[0][1], EPSILON);
        SingularValueDecomposition svdLAPACK = SVDFactory.getImplementation("LAPACK");
        SingularValueDecomposition svdColt = SVDFactory.getImplementation("COLT");
        svdLAPACK.svd(matrix);
        svdColt.svd(matrix);
        compare(svdLAPACK.getSingularValues(), svdColt.getSingularValues());
        compare(svdLAPACK.getU(), svdColt.getU(), svdColt.rank());
        compare(svdLAPACK.getV(), svdColt.getV(), matrix.length);
    }
        /*
    public void testLAPACKvsR() {
        double[][] matrix = new double[2][3];         // two columns, three rows
        matrix[0] = new double[]{1d, 2d, 3d};
        matrix[1] = new double[]{4d, 5d, 6d};

        // matrix:
        /*
       1  4
       2  5
       3  6
        */

    /*assertEquals(1, matrix[0][0], EPSILON);
      assertEquals(4, matrix[1][0], EPSILON);
      assertEquals(2, matrix[0][1], EPSILON);
      SingularValueDecomposition svdLAPACK = SVDFactory.getImplementation("LAPACK");
      SingularValueDecomposition svdR = SVDFactory.getImplementation("R");
      svdLAPACK.svd(matrix);
      svdR.svd(matrix);
      compare(svdLAPACK.getSingularValues(), svdR.getSingularValues());
      compare(svdLAPACK.getU(), svdR.getU(), 2);

      compare(svdLAPACK.getV(), svdR.getV(), matrix.length);
  }

    */
/*

    protected void compare(final double[][] u, final double[][] u2, final int rank) {
        assertEquals(rank, u2.length);
        // assertEquals(u.length, u2.length);
        for (int i = 0; i < rank; i++) {
            assertEquals(u[i].length, u2[i].length);
            for (int j = 0; j < u[i].length; j++) {
                assertEquals(u[i][j], u2[i][j], EPSILON);
            }
        }
    }
*/
}
