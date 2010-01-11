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
import org.netlib.lapack.DGESDD;
import org.netlib.util.intW;

/**
 * Perform SVD with the LAPACK DGESDD implementation (divide and conquer algorithm).
 *
 * @author Fabien Campagne Date: Dec 13, 2007 Time: 2:53:08 PM
 */
public final class SingularValueDecompositionWithLAPACK implements SingularValueDecomposition {
    private int rank;
    private double[] S;
    private double[][] V;
    private double[][] U;
    private static final Log LOG = LogFactory.getLog(SingularValueDecompositionWithLAPACK.class);

    /**
     * Construct a new {@link edu.cornell.med.icb.svd.SingularValueDecomposition}.
     */
    SingularValueDecompositionWithLAPACK() {
        super();
    }

    public void svd(final double[][] matrix) {
        svd(matrix, matrix.length, matrix[0].length);
    }

    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix m
     * @param k Number of singular values to compute.
     */
    public void svd(final double[][] matrix, final int k) {
        throw new UnsupportedOperationException();
    }


    public void svd(final double[][] matrix, final int numU, final int numV) {
        /*  Arguments to the LAPACK code:
         *  =========
jobz

    CHARACTER*1. Must be 'A', 'S', 'O', or 'N'.

    Specifies options for computing all or part of the matrix U.

    If jobz = 'A', all m columns of U and all n rows of VT are returned in the arrays u and vt;

    if jobz = 'S', the first min(m, n) columns of U and the first min(m, n) rows of VT are returned in the arrays u and vt;

    if jobz = 'O', then

    if m ?  n, the first n columns of U are overwritten in the array a and all rows of VT are returned in the array vt;

    if m < n, all columns of U are returned in the array u and the first m rows of VT are overwritten in the array a;

    if jobz = 'N', no columns of U or rows of VT are computed.
m

    INTEGER. The number of rows of the matrix A (m ? 0).
n

    INTEGER. The number of columns in A (n ? 0).
a, work

    REAL for sgesdd

    DOUBLE PRECISION for dgesdd

    COMPLEX for cgesdd

    DOUBLE COMPLEX for zgesdd.

    Arrays: a(lda,*) is an array containing the m-by-n matrix A.

    The second dimension of a must be at least max(1, n).

    work is a workspace array, its dimension max(1, lwork).
lda

    INTEGER. The first dimension of the array a. Must be at least max(1, m).
ldu, ldvt

    INTEGER. The leading dimensions of the output arrays u and vt, respectively.

    Constraints:

    ldu ? 1; ldvt ? 1.

    If jobz = 'S' or 'A', or jobz = 'O' and m < n,

    then ldu ? m;

    If jobz = 'A' or jobz = 'O' and m < n,

    then ldvt ? n;

    If jobz = 'S', ldvt ? min(m, n).
lwork

    INTEGER.

    The dimension of the array work; lwork ? 1.

    If lwork = -1, then a workspace query is assumed; the routine only calculates the optimal size of the work array, returns this value as the work(1), and no error message related to lwork is issued by xerbla.

    See Application Notes for the suggested value of lwork.
rwork

    REAL for cgesdd

    DOUBLE PRECISION for zgesdd

    Workspace array, DIMENSION at least max(1, 5*min(m,n)) if jobz = 'N'.

    Otherwise, the dimension of rwork must be at least max(1, 5*(min(m,n))2 + 7*min(m,n)). This array is used in complex flavors only.
iwork

    INTEGER. Workspace array, DIMENSION at least max(1, 8 *min(m, n)).

Output Parameters

a

    On exit:

    If jobz = 'O', then if m? n, a is overwritten with the first n columns of U (the left singular vectors, stored columnwise). If m < n, a is overwritten with the first m rows of VT (the right singular vectors, stored rowwise);

    If jobz?'O', the contents of a are destroyed.
s

    REAL for single precision flavors DOUBLE PRECISION for double precision flavors.

    Array, DIMENSION at least max(1, min(m,n)). Contains the singular values of A sorted so that s(i) ? s(i+1).
u, vt

    REAL for sgesdd

    DOUBLE PRECISION for dgesdd

    COMPLEX for cgesdd

    DOUBLE COMPLEX for zgesdd.

    Arrays:

    u(ldu,*); the second dimension of u must be at least max(1, m) if jobz = 'A' or jobz = 'O' and m < n.

    If jobz = 'S', the second dimension of u must be at least max(1, min(m, n)).

    If jobz = 'A'or jobz = 'O' and m < n, u contains the m-by-m orthogonal/unitary matrix U.

    If jobz = 'S', u contains the first min(m, n) columns of U (the left singular vectors, stored columnwise).

    If jobz = 'O' and m?n, or jobz = 'N', u is not referenced.

    vt(ldvt,*); the second dimension of vt must be at least max(1, n).

    If jobz = 'A'or jobz = 'O' and m?n, vt contains the n-by-n orthogonal/unitary matrix VT.

    If jobz = 'S', vt contains the first min(m, n) rows of VT (the right singular vectors, stored rowwise).

    If jobz = 'O' and m < n, or jobz = 'N', vt is not referenced.
work(1)

    On exit, if info = 0, then work(1) returns the required minimal size of lwork.
info

    INTEGER.

    If info = 0, the execution is successful.

    If info = -i, the i-th parameter had an illegal value.

    If info = i, then ?bdsdc did not converge, updating process failed.



        */
        final java.lang.String jobz;
        final int numColumns = matrix.length;
        final int numRows = matrix[0].length;
        final int m = numColumns;
        final int n = numRows;
        final double[][] a = new double[numColumns][numRows];
        // copy matrix into a, because a will be overwritten:
        int i = 0;
        for (final double[] column : matrix) {
            a[i] = new double[column.length];
            System.arraycopy(column, 0, a[i], 0, column.length);
            ++i;
        }
        final int min_m_n = Math.min(m, n);
        final int numSingularValues = Math.max(1, Math.min(m, n));
        final double[] s = new double[numSingularValues];
        final double[][] u = new double[numColumns][numRows];
        final double[][] vt = new double[min_m_n][min_m_n];

        final int minLWork = Math.max(3 * Math.min(m, n) + Math.max(m, n), 5 * Math.min(m, n) - 4);
        int lwork = minLWork * 4; // larger than minLWork to increase performance.
        double[] work = new double[lwork];
        final int[] iwork = new int[Math.max(1, 8 * Math.max(m, n))];
        final intW info = new intW(0);

        jobz = "S"; // columns of U will be written in U.
        lwork = getOptimalLWork(jobz, m, n, a, s, vt, u, work, lwork, iwork, info);
        work = new double[lwork];

        // watch out since the Javadoc for DGESDD incorrectly indicate that argument u should appear before vt.
        // The following call is correct and shows vt immediately before u in the parameter list:

        DGESDD.DGESDD(jobz, m, n, a, s, vt, u, work, lwork, iwork, info);

        if (info.val != 0) {
            LOG.warn("SVD computation has not converged!");

        }
        S = s;
        U = numU != 0 ? u : null;
        V = numV != 0 ? vt : null;

        rank = numSingularValues;
    }

    private int getOptimalLWork(final String jobz, final int m, final int n, final double[][] a, final double[] s,
                                final double[][] vt, final double[][] u, final double[] work, final int lwork,
                                final int[] iwork, final intW info) {
        final double[] queryWork = new double[1];

        DGESDD.DGESDD(jobz, m, n, a, s, vt, u, queryWork, -1, iwork, info);
        return (int) queryWork[0];
    }

    public int rank() {
        return rank;
    }

    public double[] getSingularValues() {
        return S;
    }

    public double[][] getU() {
        return U;
    }

    public double[][] getV() {
        return V;
    }
}
