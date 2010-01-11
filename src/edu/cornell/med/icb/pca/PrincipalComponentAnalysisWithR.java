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

package edu.cornell.med.icb.pca;

import edu.cornell.med.icb.R.RConnectionPool;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Fabien Campagne Date: April 15 2008.
 */
public class PrincipalComponentAnalysisWithR {
    private static final Log LOG = LogFactory.getLog(PrincipalComponentAnalysisWithR.class);
    private boolean doScaling;


    public void setTolerance(final double tolerance) {
        this.tolerance = tolerance;
    }

    private double tolerance = 1E-6;
    private double[][] rotation;
    private List<CharSequence> rotationRowNames = new ArrayList<CharSequence>();
    private boolean collectRotationRowNames;
    private List<CharSequence> rowIds;

    public void setCollectRotationRowNames(final boolean collectRotationRowNames) {
        this.collectRotationRowNames = collectRotationRowNames;
    }

    /**
     * Construct a new PrincipalComponentAnalysisWithR.
     */
    public PrincipalComponentAnalysisWithR() {
        super();
    }

    public double[][] getRotation() {
        return rotation;
    }

    public List<CharSequence> getRotationRowNames() {
        return rotationRowNames;
    }

    /**
     * Perform a principal component analysis on a matrix.
     *
     * @param matrix Matrix to analyse. matrix.length must be the number of columns of the matrix,
     *               and matrix[x].length=numCol for any x.
     * @param colIds Ids of the columns.
     */
    public void pca(final double[][] matrix,
                    final List<CharSequence> colIds
    ) {
        this.pca(matrix, colIds, makeRowIds(matrix[0].length));
    }

    private List<CharSequence> makeRowIds(final int length) {
        final List<CharSequence> result = new ArrayList<CharSequence>();
        for (int rowIndex = 0; rowIndex < length; rowIndex++) {
            final MutableString rowName = new MutableString("row_");
            rowName.append(Integer.toString(rowIndex));
            result.add(rowName);
        }
        return result;
    }

    /**
     * Perform a principal component analysis on a matrix.
     *
     * @param matrix Matrix to analyse. matrix.length must be the number of columns of the matrix,
     *               and matrix[x].length=numCol for any x.
     * @param rowIds Ids of the rows.
     * @param colIds Ids of the columns.
     */
    public void pca(final double[][] matrix, final List<CharSequence> colIds,
                    final List<CharSequence> rowIds) {
        this.rowIds = rowIds;
        // Clear any previous results
        rotation = null;
        // CALL R ROC
        final RConnectionPool connectionPool = RConnectionPool.getInstance();

        RConnection connection = null;
        try {
            final int numCols = matrix.length;      // number of columns of input matrix
            final int numRows = matrix[0].length;   // number of rows of input matrix

            //    System.out.println(String.format("numCols: %d numRows: %d", numCols, numRows));
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
            // assemble the matrix to perform PCA on:

            final String makeMatrix = //"print(\"Making matrix\");\n" +
                    "mat<-matrix(c(" + rowNames.toString() + "), nrow=" + Integer.toString(numRows) + ", ncol=" +
                            Integer.toString(numCols) +
                            ", dimnames=list(  c(" + names(rowIds) + "), c(" + names(colIds) + ") ) );\n"
                            //    + "print(\"matrix built.\"); \n"
                            + deleteRows + "\n" +
                            //     + "print(\"row variables deleted\");\n" +
                            "\n";

            //   System.out.println("makeMatrix: " + makeMatrix);
            final REXP exp = connection.eval(makeMatrix);

            final String script = "result<-prcomp(mat, scale=" + doScalingAsString() +
                    ", center=" + doScalingAsString() + ", tol=" + Double.toString(tolerance) + ");" +
                    //  "print(result$rotation)\n" +
                    "result$rotation";
            //   System.out.println("script: " + script);

            final REXP rotationEval = connection.eval(script);

            //  rotation = rotationEval.asDoubleMatrix();

            // final REXP expressionU = connection.eval("result$u");
            final double[] elements = rotationEval.asDoubles();
            final int reducedNumCols = elements.length / numCols;
            //     System.out.println("reducedCols: " + reducedNumCols);
            //     System.out.println("numRows: " + numCols);
            final int reducedNumRows = numCols;
            rotation = new double[reducedNumCols][reducedNumRows];
            int k = 0;
            for (int c = 0; c < reducedNumCols; c++) {
                rotation[c] = new double[reducedNumRows];
                for (int r = 0; r < reducedNumRows; r++) {

                    rotation[c][r] = elements[k];
                    k++;
                }

                //       System.out.println("rotation["+c+"]: ");
                //     TextIO.storeDoubles(rotation[c], System.out);
            }

            if (collectRotationRowNames) {
                rotationRowNames = new ArrayList<CharSequence>();
                final String scriptNames = //"print((dimnames(result$rotation))[[1]]);\n" +
                        "(dimnames(result$rotation))[[1]];";
                final REXP rotationNamesEval = connection.eval(scriptNames);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Rotation names: " + rotationNamesEval.toDebugString());
                }
                if (rotationNamesEval.isList()) {
                    for (final Object o : rotationNamesEval.asList().values()) {
                        final REXPString expr = (REXPString) o;
                        final String name = expr.asString();
                        //System.out.println("nameasList: " + name);
                        rotationRowNames.add(name);
                    }
                } else {
                    rotationRowNames.addAll(Arrays.asList(rotationNamesEval.asStrings()));
                }
            }
        } catch (RserveException e) {

            rotation = null;
            connection = checkConnection(connectionPool, connection);
            System.out.println(e.getRequestErrorDescription());
            LOG.warn("Cannot PCA. Make sure Rserve (R server) is configured and running.", e);
            LOG.error(e.getRequestErrorDescription());
            LOG.error(e.getMessage());
            if (connection != null) {
                LOG.error("Error with " + connection.getHost() + ":" + connection.getPort()
                        + " in thread " + Thread.currentThread().getName());
            }
        } catch (REngineException e) {

            rotation = null;
            connection = checkConnection(connectionPool, connection);
            LOG.warn("Cannot PCA. Make sure Rserve (R server) is configured and running.", e);
            if (connection != null) {
                LOG.error("Error with " + connection.getHost() + ":" + connection.getPort()
                        + " in thread " + Thread.currentThread().getName());
            }
        } catch (REXPMismatchException e) {

            rotation = null;
            connection = checkConnection(connectionPool, connection);
            LOG.warn("Cannot PCA. Make sure Rserve (R server) is configured and running.", e);
            if (connection != null) {
                LOG.error("Error with " + connection.getHost() + ":" + connection.getPort()
                        + " in thread " + Thread.currentThread().getName());
            }
        } finally {
            if (connection != null) {
                // in local tests closing helps reliability however there may still be memory issues
                connection.close();
                connectionPool.returnConnection(connection);
            }
        }
    }

    private RConnection checkConnection(final RConnectionPool connectionPool, final RConnection connection) {
        try {
            return connectionPool.reEstablishConnection(connection);
        } catch (RserveException e) {
            LOG.error(e.getRequestErrorDescription());
        }
        return null;
    }

    private String doScalingAsString() {
        return (doScaling) ? "TRUE" : "FALSE";
    }

    private MutableString names(final List<CharSequence> rowIds) {
        final MutableString result = new MutableString();
        for (int i = 0; i < rowIds.size(); i++) {
            result.append('"');
            result.append(rowIds.get(i));
            result.append('"');
            if (i != rowIds.size() - 1) {
                result.append(',');
            }
        }
        return result;
    }

    public void setRotation(final double[][] rotation) {
        this.rotation = rotation;
    }

    /**
     * Returns the product slice * rotation matrix. When presented with similar data to that used to determine
     * the PCA, this method will project the columns of slice onto the  principal components for each row of slice.
     *
     * @param slice
     * @return A matrix of dimension numRows(slice) x numCol(rotation) where each row of slice has been projected onto a principal component.
     */
    public double[][] rotate(final double[][] slice) {
        final double[][] A = slice;
        final double[][] B = rotation;
        if (rotation == null) {

            return null;
        }
        final double[][] result = product(A, B);
        return result;
    }

    /**
     * Calculate the product of two matrices.
     * @param A Matrix A
     * @param B Matrix B
     * @return A * B
     */
    public static double[][] product(final double[][] A, final double[][] B) {
        final int numResultRows = A[0].length;        // numRows of slice matrix
        final int numResultCols = B.length;    // numCol for rotation matrix
        final int n = A.length;                   // numCol of slice matrix
        assert n == B[0].length : "number of columns of slice must match the number of rows of the rotation matrix.";

        final double[][] result = new double[/* cols */ numResultCols][/* rows */ numResultRows];
        for (int i = 0; i < numResultRows; i++) {    // result column index
            for (int j = 0; j < numResultCols; j++) {   // result row index
                for (int r = 0; r < n; r++) {

                    final double s = A[r][i];
                    final double rot = (j >= B.length ? 0 : B[j][r]);

                    result[j][i] += s * rot;
                }
            }
            //  System.out.println("rotationRowName: "+rowIds.get(i));
        }
        return result;
    }

    public void setDoScaling(final boolean b) {
        doScaling = b;
    }
}
