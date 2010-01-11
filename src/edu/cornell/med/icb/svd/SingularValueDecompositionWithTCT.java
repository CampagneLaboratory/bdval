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

import matrix.DoubleMatrix;
import matrix.decomp.SVD;
import matrix.decomp.jni.ArpackException;
import matrix.decomp.jni.ArpackSVD;
import matrix.dense.DenseMatrix;
import matrix.util.MatrixUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Calculates the Singular value decomposition of a matrix using the Text Clustering Toolkit (TCT) package.
 */
public final class SingularValueDecompositionWithTCT implements SingularValueDecomposition {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(SingularValueDecompositionWithTCT.class);

    private SVD result;

    /**
     * Construct a new {@link edu.cornell.med.icb.svd.SingularValueDecomposition}.
     */
    SingularValueDecompositionWithTCT() {
        super();
    }

    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix m
     */
    public void svd(final double[][] matrix) {
        final int m = matrix.length;
        final int n = matrix[0].length;

        this.svd(matrix, Math.min(m, n));
    }

    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix m
     * @param k Number of singular values to compute.
     */
    public void svd(final double[][] matrix, final int k) {
        try {
            result = new ArpackSVD().factorSelected(MatrixUtils.buildTranspose(new DenseMatrix(matrix)), k);
        } catch (ArpackException e) {
            throw new SVDRuntimeException(e);
        }
    }

    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix m
     * @param numU Number of U vectors that must be estimated.
     * @param numV Number of V elements that must be estimated.
     */
    public void svd(final double[][] matrix, final int numU, final int numV) {
        // in this case we assume that the user knows what they want and won't pass something too large
        this.svd(matrix, Math.max(numU, numV));
    }

    /**
     * Returns the number of singular values computed.
     *
     * @return number of singular values (i.e. the rank of the decomposition).
     */
    public int rank() {
        return result.getRank();
    }

    /**
     * Returns the singular values.
     *
     * @return array of singular values.
     */
    public double[] getSingularValues() {
        return result.getS();
    }

    public double[][] getU() {
        final DoubleMatrix matrix = result.getU();
        final int m = matrix.rows();
        final int n = matrix.columns();
        final double[][] u = new double[n][m];
        for (int row = 0; row < m; row++) {
            for (int column = 0; column < n; column++) {
                u[column][row] = matrix.get(row, column);
            }
        }
        return u;
    }

    public double[][] getV() {
        final DoubleMatrix matrix = result.getV();
        final int m = matrix.rows();
        final int n = matrix.columns();
        final double[][] v = new double[n][m];
        for (int row = 0; row < m; row++) {
            for (int column = 0; column < n; column++) {
                v[column][row] = matrix.get(row, column);
            }
        }
        return v;
    }
}
