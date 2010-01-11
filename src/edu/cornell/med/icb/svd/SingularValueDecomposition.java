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

/**
 * An interface to calculate the Singular value decomposition of a matrix.
 *
 * @author Fabien Campagne Date: Dec 13, 2007 Time: 12:15:29 PM
 */
public interface SingularValueDecomposition {
    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix The matrix m where the form of the matrix is
     * expected to be double[m][n].
     * m is the number of columns of input matrix
     * and n is the number of rows of input matrix
     */
    void svd(double[][] matrix);

    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix m
     * @param k Number of singular values to compute.
     */
    void svd(double[][] matrix, int k);

    /**
     * Computes the Singular Value Decomposition of matrix m.
     *
     * @param matrix m
     * @param numU Number of U vectors that must be estimated.
     * @param numV Number of V elements that must be estimated.
     */
    void svd(double[][] matrix, int numU, int numV);

    /**
     * Returns the number of singular values computed.
     * @return number of singular values (i.e. the rank of the decomposition).
     */
    int rank();

    /**
     * Returns the singular values.
     * @return array of singular values.
     */
    double[] getSingularValues();

    double[][] getU();

    double[][] getV();
}

