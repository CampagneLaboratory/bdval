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
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * @author Fabien Campagne Date: Dec 13, 2007 Time: 12:17:52 PM
 */
public class SingularValueDecompositionWithR implements SingularValueDecomposition {
    private static final Log LOG = LogFactory.getLog(SingularValueDecompositionWithR.class);

    double[][] U;
    double[][] V;
    double[] S;

    /**
     * Construct a new {@link edu.cornell.med.icb.svd.SingularValueDecomposition}.
     */
    SingularValueDecompositionWithR() {
        super();
    }

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
        // Clear any previous results
        U = null;
        V = null;
        S = null;

        // CALL R ROC
        final RConnectionPool connectionPool = RConnectionPool.getInstance();
        RConnection connection = null;
        try {
            final int m = matrix.length;      // number of columns of input matrix
            final int n = matrix[0].length;   // number of rows of input matrix

            connection = connectionPool.borrowConnection();

            final MutableString rowNames = new MutableString();
            final MutableString deleteRows = new MutableString();

            for (int i = 0; i < matrix.length; i++) {
                final String rowName = "row" + i;
                connection.assign(rowName, matrix[i]);
                deleteRows.append("rm(").append(rowName).append(");\n");
                if (rowNames.length() > 0) {
                    rowNames.append(',');
                }
                rowNames.append(rowName);
            }
            // assemble the matrix to perform SVD on:

            final String makeMatrix = "mat<-matrix(c(" + rowNames.toString() + ")," + Integer.toString(n) + "," +
                    Integer.toString(m) + ");"
                    + "print(\"matrix built.\") \n"
                    + deleteRows + "\n"
                    + "print(\"row variables deleted\")";
            final REXP exp = connection.eval(
                    makeMatrix);

            final String script = "result<-svd(mat," + Integer.toString(numU) + "," + Integer.toString(numV) + ");"
                    + "print(\"SVD computation finished\")\n" +
                    "result$d";
            // System.out.println("makeMatrix: " + makeMatrix);
            final REXP expression = connection.eval(script);

            // System.out.println("result: ");
            // TextIO.storeDoubles(expression.asDoubleMatrix()[0], System.out);

            S = expression.asDoubles();

            if (numV != 0) {
                final REXP expressionV = connection.eval("result$v");
                final double[] elements = expressionV.asDoubles();
                V = new double[numV][m];
                int k = 0;
                for (int i = 0; i < numV; i++) {
                    for (int j = 0; j < m; j++) {
                        V[i][j] = elements[k];
                        k++;
                    }
                }
            }

            if (numU != 0) {
                final REXP expressionU = connection.eval("result$u");
                final double[] elements = expressionU.asDoubles();
                U = new double[numU][n];
                int k = 0;
                for (int i = 0; i < numU; i++) {
                    for (int j = 0; j < n; j++) {
                        U[i][j] = elements[k];
                        k++;
                    }
                    // System.out.println("U["+i+"]: ");
                    // TextIO.storeDoubles(U[i], System.out);
                }

            }
        } catch (RserveException e) {
            LOG.warn("Cannot SVD curve. Make sure Rserve (R server) is configured and running.", e);
            LOG.error(e.getRequestErrorDescription());
            LOG.error(e.getMessage());
        } catch (REXPMismatchException e) {
            LOG.warn("Cannot SVD curve. Make sure Rserve (R server) is configured and running.", e);
        } catch (REngineException e) {
            LOG.warn("Cannot SVD curve. Make sure Rserve (R server) is configured and running.", e);
        } finally {
            if (connection != null) {
                connectionPool.returnConnection(connection);
            }
        }
    }

    public int rank() {
        return S.length;
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
