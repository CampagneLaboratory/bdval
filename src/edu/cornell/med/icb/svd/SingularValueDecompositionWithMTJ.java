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

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.SVD;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Calculates the Singular value decomposition of a matrix using the Matrix
 * Toolkits for Java (MTJ) package.
 */
public final class SingularValueDecompositionWithMTJ implements SingularValueDecomposition {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(SingularValueDecompositionWithMTJ.class);

    private SVD result;

    /**
     * Construct a new {@link edu.cornell.med.icb.svd.SingularValueDecomposition}.
     */
    SingularValueDecompositionWithMTJ() {
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

        this.svd(matrix, n, m);
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

    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix m
     * @param numU Number of U vectors that must be estimated.
     * @param numV Number of V elements that must be estimated.
     */
    public void svd(final double[][] matrix, final int numU, final int numV) {
        try {
            result = SVD.factorize(new DenseMatrix(matrix));
        } catch (NotConvergedException e) {
            throw new SVDRuntimeException(e);
        }
    }

    public int rank() {
        return result.getS().length;
    }

    public double[] getSingularValues() {
        return result.getS();
    }

    public double[][] getU() {
        final Matrix matrix = result.getVt();
        final int m = matrix.numRows();
        final int n = matrix.numColumns();
        final double[][] u = new double[m][n];
        for (int row = 0; row < m; row++) {
            for (int column = 0; column < n; column++) {
                u[row][column] = matrix.get(row, column);
            }
        }
        return u;
    }

    public double[][] getV() {
        final Matrix matrix = result.getU();
        final int m = matrix.numRows();
        final int n = matrix.numColumns();
        final double[][] v = new double[m][n];
        for (int row = 0; row < m; row++) {
            for (int column = 0; column < n; column++) {
                v[row][column] = matrix.get(row, column);
            }
        }
        return v;
    }
}
