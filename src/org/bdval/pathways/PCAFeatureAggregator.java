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

package org.bdval.pathways;

import edu.cornell.med.icb.learning.MinMaxScalingRowProcessor;
import edu.cornell.med.icb.pca.PrincipalComponentAnalysisWithR;
import edu.cornell.med.icb.pca.RotationReaderWriter;
import edu.cornell.med.icb.svd.SVDFactory;
import edu.cornell.med.icb.svd.SingularValueDecompositionWithR;
import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.Table;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.lang.MutableString;
import org.apache.log4j.Logger;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabien Campagne
 *         Date: Apr 29, 2008
 *         Time: 3:43:07 PM
 */
public class PCAFeatureAggregator extends PathwayFeatureAggregator {
    private static final Logger LOG = Logger.getLogger(PCAFeatureAggregator.class);
    MinMaxScalingRowProcessor scaler;
    RotationReaderWriter rotationIO;
    private final String pathwayComponentsDirectory;
    private int splitId;

    public PCAFeatureAggregator(final String pathwayComponentsDirectory) {
        super();
        this.pathwayComponentsDirectory = pathwayComponentsDirectory;

    }

    /**
     * Aggregate a slice of the input table.
     *
     * @param source
     * @param datasetEndpointName
     * @param aggregated
     * @param sampleIdList
     * @param usedProbesetIndices
     * @param pi
     * @param probeIndices
     * @param numProbeIndices
     * @param slice
     * @param colIds
     * @param probeIds
     * @param splitType
     * @param splitId
     */
    @Override
    protected void aggregate(final Table source, final String datasetEndpointName, final ArrayTable aggregated,
                             final List<CharSequence> sampleIdList, final IntSet usedProbesetIndices,
                             final PathwayInfo pi, final IntList probeIndices, final int numProbeIndices,
                             final double[][] slice, final List<CharSequence> colIds,
                             final MutableString[] probeIds, final String splitType, final int splitId) {
        final PrincipalComponentAnalysisWithR calc = new PrincipalComponentAnalysisWithR();
        final List<CharSequence> rowIds;
        final Object2DoubleOpenHashMap<MutableString> meanMap = new Object2DoubleOpenHashMap<MutableString>();
        final Object2DoubleOpenHashMap<MutableString> rangeMap = new Object2DoubleOpenHashMap<MutableString>();

        scaler = new MinMaxScalingRowProcessor() {
            @Override
            protected MutableString getFeatureIdentifier(final int featureIndex) {
                return probeIds[featureIndex];
            }

        };
        this.splitId = splitId;
        rowIds = new ArrayList<CharSequence>();

        aggregateFeatureNotSynchronized(source, datasetEndpointName, aggregated, sampleIdList, usedProbesetIndices, pi, probeIndices, numProbeIndices, slice, colIds, probeIds, calc, meanMap, rangeMap);


    }

    private void aggregateFeatureNotSynchronized(final Table source, final String datasetEndpointName, final ArrayTable aggregated, final List<CharSequence> sampleIdList, final IntSet usedProbesetIndices, final PathwayInfo pi, final IntList probeIndices, final int numProbeIndices, final double[][] slice, final List<CharSequence> colIds, final MutableString[] probeIds, final PrincipalComponentAnalysisWithR calc, Object2DoubleOpenHashMap<MutableString> meanMap, Object2DoubleOpenHashMap<MutableString> rangeMap) {
        final List<CharSequence> rowIds;
        if (!hasCachedRotation(datasetEndpointName, pi.pathwayId)) {
            //scale and compute PCA.
            scaler.setTrainingMode(meanMap, rangeMap);
            scaler.processMatrix(slice, numProbeIndices);

            // not cached, evaluate from the data
            calc.setCollectRotationRowNames(true);
            calc.setTolerance(0.1); // ignore components that explain less than 10% of the variance.
            calc.pca(slice, colIds, sampleIdList);
            rowIds = calc.getRotationRowNames();

            final double[][] rotation = calc.getRotation();
            if (rotation != null) {
                saveRotation(datasetEndpointName, pi.pathwayId, rowIds, rotation);
                saveMap(datasetEndpointName, pi.pathwayId, rowIds, meanMap, "mean");
                saveMap(datasetEndpointName, pi.pathwayId, rowIds, rangeMap, "range");
            } else {
                LOG.error("An error occurred aggregating features for pathway " + pi.pathwayId + ". Details may be available in log files. Probesets will be left unchanged for this pathway.");
                for (final int probeIndex : probeIndices) {
                    // leave probesets unchanged.
                    usedProbesetIndices.remove(probeIndex);
                }
                // consider next pathway:
                return;

            }
        } else {
            // cached, reuse the rotation matrix previously obtained from data (e.g., on a feature selection dataset)
            rowIds = new ArrayList<CharSequence>();
            meanMap = loadMap(datasetEndpointName, pi.pathwayId, "mean");
            rangeMap = loadMap(datasetEndpointName, pi.pathwayId, "range");
            assert meanMap != null && rangeMap != null : "mean and range maps cannot be found in pathway-components";
            inspectMatrix(slice);
            scaler.setTestSetMode(meanMap, rangeMap);
            scaler.processMatrix(slice, numProbeIndices);
            inspectMatrix(slice);
            // scaleMatrix(slice, slice.length, slice[0].length, meanMap, rangeMap, scaler);
            calc.setRotation(getCachedRotation(datasetEndpointName, pi.pathwayId, rowIds));
            int index = 0;
            for (final CharSequence recoveredRowId : rowIds) {
                assert probeIds[index].equals(recoveredRowId) :
                        " recovered probeset " + recoveredRowId + " at index " + index +
                                " must match probeid for slice: " + probeIds[index];
                index++;
            }

        }
        final double[][] projection = calc.rotate(slice);
        inspectMatrix(projection);
        for (int colIndex = 0; colIndex < projection.length; colIndex++) {

            final int reducedColumnIndex = colIndex + 1;
            final String newColumnId = pi.pathwayId.toString() + "_svd" + reducedColumnIndex;
            //        System.out.println("adding column " + newColumnId);
            final int newColumnIndex = aggregated.addColumn(newColumnId, double.class);
            aggregated.reserve(newColumnIndex, source.getRowNumber());
            final Table.RowIterator ri = aggregated.firstRow();
            for (int rowIndex = 0; rowIndex < projection[colIndex].length; rowIndex++) {

                aggregated.setValue(newColumnIndex, ri, projection[colIndex][rowIndex]);
                assert !ri.end() : String.format("reached final row of aggregated table j=%d, U[i].length=%d," +
                        " aggregated.getRowNumber()=%d ",
                        rowIndex, projection[colIndex].length, aggregated.getRowNumber());

                ri.next();
            }
        }
    }

    @Override
    public Logger getLog() {
        return LOG;
    }

    private void inspectMatrix(final double[][] projection) {
//        System.out.println("projection; "+projection);
    }

    private Object2DoubleOpenHashMap<MutableString> loadMap(final String datasetEndpointName,
                                                            final MutableString pathwayId, final String type) {
        this.setupRotationIO(datasetEndpointName, splitId);
        return this.rotationIO.loadMap(datasetEndpointName, pathwayId, type);
    }

    private void saveMap(final String datasetEndpointName, final MutableString pathwayId,
                         final List<CharSequence> rowIds, final Object2DoubleOpenHashMap<MutableString> map,
                         final String type) {
        this.setupRotationIO(datasetEndpointName, splitId);
        this.rotationIO.saveMap(datasetEndpointName, pathwayId, rowIds, map, type);
    }

    private boolean hasCachedRotation(final String datasetEndpointName, final MutableString pathwayId) {
        if (!setupRotationIO(datasetEndpointName, splitId)) {
            return false;
        } else {

            return rotationIO.isTableCached(datasetEndpointName, pathwayId);
        }
    }

    /**
     * Combine the rotations from all the splits into an aggregate rotation. We can average rotations by using the method
     * described by Curtis WD, Janin AL and Zikan K: We sum all the rotation matrices, calculate the SVD of the sum and
     * take X . Y as the average rotations.  See  http://ieeexplore.ieee.org/iel2/3045/8641/00380755.pdf?tp=&isnumber=&arnumber=380755
     * for details.
     * More precisely, do the following to obtain the average:
     * <p/>
     * delta=0.4; theta=pi/2;a=cos(theta+delta); b=sin(theta+delta);Q1=matrix(c(a,b,-b,a),nrow=2);
     * delta=-0.3; theta=pi/2;a=cos(theta+delta); b=sin(theta+delta);Q2=matrix(c(a,b,-b,a),nrow=2);
     * delta=-0.4; theta=pi/2;a=cos(theta+delta); b=sin(theta+delta);Q3=matrix(c(a,b,-b,a),nrow=2);
     * delta=0.3; theta=pi/2;a=cos(theta+delta); b=sin(theta+delta);Q4=matrix(c(a,b,-b,a),nrow=2);
     * Qsum=Q1+Q2+Q3+Q4
     * Qsum
     * s=svd(Qsum)
     * Qback=s$u %*% diag(s$d) %*% t(s$v)
     * Qaverage=s$u %*% t(s$v)
     * Qaverage
     * atan2(Qaverage[2,1],Qaverage[1,1])
     * Verify that the product of the average rotation by its transpose is the identity matrix:
     * Qaverage%*%t(Qaverage)
     * [,1] [,2]
     * [1,]    1    0
     * [2,]    0    1
     *
     * @param datasetEndpointName
     * @param pathwayId
     */
    private void combineRotations(
            final String datasetEndpointName,
            final MutableString pathwayId, final List<CharSequence> rowIds) {
        // splitId ==0
        assert splitId == 0 : "combineRotation cannot be called when processing a "
                + "single split. It is only useful to combine splits for the entire training set";
        if (setupRotationIO(datasetEndpointName, splitId)) {
            final List<CharSequence> tempRowIds = new ObjectArrayList<CharSequence>();

            final Map<String, Set<String>> rotations =
                    rotationIO.getRotationFiles(datasetEndpointName, pathwayId);
            double[][] sum = null;
            for (final String compoundFile : rotations.keySet()) {
                final Set<String> rotationSubFiles = rotations.get(compoundFile);
                if (rotationSubFiles.size() == 0) {
                    continue;
                }
                final RotationReaderWriter subRotationIO;
                try {
                    final int fileSplitId =
                            RotationReaderWriter.splitIdFromCompoundFilename(compoundFile);
                    if (fileSplitId == -1) {
                        LOG.error("Could not determine split id from compound file named "
                                + compoundFile);
                        continue;
                    }
                    subRotationIO =
                            new RotationReaderWriter(
                                    new File(pathwayComponentsDirectory),
                                    datasetEndpointName, fileSplitId);
                } catch (IOException e) {
                    LOG.error("Error opening compound file " + compoundFile);
                    continue;
                }
                for (final String rotationFilename : rotationSubFiles) {
                    final double[][] matrix = subRotationIO.getRotationMatrix(
                            rotationFilename, tempRowIds);
                    if (rowIds.size() == 0) {
                        // transfer once only. RowIds are the same for all
                        // the matrices to be averaged.
                        rowIds.addAll(tempRowIds);
                    }
                    if (sum == null) {
                        sum = matrix;
                    } else {
                        sum(sum, matrix);
                    }
                }
            }
            final SingularValueDecompositionWithR svd =
                    (SingularValueDecompositionWithR)
                            SVDFactory.getImplementation(SVDFactory.ImplementationType.R);
            assert svd != null : " SVD implementation could not be obtained.";
            if (sum != null) {
                svd.svd(sum);
                final double[][] U = svd.getU();
                final double[][] V = svd.getV();
                final double[][] Vt = transpose(V);
                final double[][] combined = product(U, Vt);
                rotationIO.saveRotationMatrix(datasetEndpointName, pathwayId, rowIds, combined);
            } else {
                LOG.error("Cannot average rotations for endpoint " + datasetEndpointName + " pathway " + pathwayId);
            }
        }
    }

    private double[][] product(final double[][] A, final double[][] B) {
        return PrincipalComponentAnalysisWithR.product(A, B);
    }

    private double[][] transpose(final double[][] matrix) {
        final int numCols = matrix.length;
        final int numRows = matrix[0].length;
        final double[][] transpose = new double[numRows][numCols];
        for (int c = 0; c < matrix.length; c++) {
            for (int r = 0; r < matrix[c].length; r++) {
                transpose[r][c] = matrix[c][r];
            }
        }
        return transpose;
    }

    /**
     * Add a matrix to destination matrix.
     *
     * @param destination Sum will be stored in place.
     * @param matrix      Matrix to add to the destination.
     */
    private void sum(final double[][] destination, final double[][] matrix) {
        assert destination.length == matrix.length : " matrices must have the same number of columns";

        for (int c = 0; c < destination.length; c++) {
            assert destination[c].length == matrix[c].length : " matrices must have the same number of rows";
            for (int r = 0; r < destination[c].length; r++) {
                destination[c][r] += matrix[c][r];
            }
        }
    }


    private double[][] getCachedRotation(final String datasetEndpointName,
                                         final MutableString pathwayId,
                                         final List<CharSequence> rowIds) {
        if (!setupRotationIO(datasetEndpointName, splitId)) {
            return new double[0][];
        } else {

            return rotationIO.getRotationMatrix(datasetEndpointName, pathwayId, rowIds);
        }
    }


    private void saveRotation(final CharSequence datasetEndpointName,
                              final MutableString pathwayId, final List<CharSequence> rowIds,
                              final double[][] rotation) {
        if (!setupRotationIO(datasetEndpointName, splitId)) {
            return;
        }
        rotationIO.saveRotationMatrix(datasetEndpointName, pathwayId, rowIds, rotation);

    }

    /**
     * Returns false if the rotation IO could be setup.
     */

    private boolean setupRotationIO(final CharSequence datasetEndpointName, final int splitId) {
        if (rotationIO == null) {
            try {
                rotationIO = new RotationReaderWriter(
                        new File(pathwayComponentsDirectory), datasetEndpointName, splitId);
            } catch (EOFException e) {
                LOG.error("Could not setup rotation matrix IO support. Found incomplete file.", e);
                return false;
            } catch (IOException e) {
                LOG.error("Could not setup rotation matrix IO support.", e);
                return false;
            }
        }
        return true;
    }
}
