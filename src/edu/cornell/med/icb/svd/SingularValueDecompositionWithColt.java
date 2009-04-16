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

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.QRDecomposition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Perform SVD with the Colt implementation.
 *
 * @author Fabien Campagne Date: Dec 13, 2007 Time: 2:53:08 PM
 */
public final class SingularValueDecompositionWithColt implements SingularValueDecomposition {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(SingularValueDecompositionWithColt.class);

    private cern.colt.matrix.linalg.SingularValueDecomposition result;
    /**
     * Indicates that the incoming matrix was transposed because of rectangular restrictions
     * imposed by colt.
     */
    private boolean transposed;

    /**
     * Used to perform matrix operations when required.
     */
    private static final Algebra ALGEBRA = new Algebra();

    /**
     * Construct a new {@link edu.cornell.med.icb.svd.SingularValueDecomposition}.
     */
    SingularValueDecompositionWithColt() {
        super();
    }

    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix m
     */
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
        DoubleMatrix2D matrix2d = new DenseDoubleMatrix2D(matrix);

        // transpose when the matrix is not rectangular
        if (matrix2d.rows() < matrix2d.columns()) {
            matrix2d = ALGEBRA.transpose(matrix2d);
            transposed = true;
        } else {
            transposed = false;
        }

        result = new cern.colt.matrix.linalg.SingularValueDecomposition(matrix2d);
    }

    public int rank() {
        return result.rank();
    }

    public double[] getSingularValues() {
        return result.getSingularValues();
    }

    public double[][] getU() {
        DoubleMatrix2D u = result.getU();
        if (LOG.isTraceEnabled()) {
            LOG.trace(ALGEBRA.toVerboseString(u));
        }
        if (transposed) {
            u = ALGEBRA.transpose(new QRDecomposition(u).getQ());
        }
        return u.toArray();
    }

    public double[][] getV() {
        DoubleMatrix2D v = result.getV();
        if (LOG.isTraceEnabled()) {
            LOG.trace(ALGEBRA.toVerboseString(v));
        }
        if (transposed) {
            v = new cern.colt.matrix.linalg.SingularValueDecomposition(v).getU();
        }
        return v.toArray();
    }
}
