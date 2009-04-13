/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.biomarkers.pathways;

import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.Table;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the pathway feature aggregation mechanism used by DAVMode.
 *
 * @author Fabien Campagne
 *         Date: Apr 12, 2008
 *         Time: 12:46:31 PM
 */
public abstract class PathwayFeatureAggregator {
    /**
     * Aggregate features for pathways.
     *
     * @param pathways  Set of Pathway descriptions. If null, source table is returned unchanged.
     * @param source    Input table (columns are probesets, rows are samples).
     * @param splitType Type of split
     * @param splitId   Split id
     * @return
     */
    public Table aggregateFeaturesForPathways(final ObjectSet<PathwayInfo> pathways, final Table source,
                                              final String datasetEndpointName, final String splitType, final int splitId) {

        if (getLog().isDebugEnabled()) {
            getLog().debug("Aggregating features for dataset " + datasetEndpointName + " in thread "
                    + Thread.currentThread().getName());
        }

        if (pathways == null) {
            return source;
        } else {
            final ProgressLogger logger = new ProgressLogger(getLog());
            logger.expectedUpdates = pathways.size();

            final ArrayTable aggregated = new ArrayTable();
            aggregated.setChunk(source.getRowNumber());
            aggregated.setInitialSize(source.getRowNumber());
            final int idRefColumnIndex = aggregated.addColumn("ID_REF", String.class);
            final ArrayTable.ColumnDescription sampleIdsCD = source.getColumnValues(idRefColumnIndex);

            final int rowNumber = source.getRowNumber();
            final String[] sampleIds = sampleIdsCD.getStrings();
            final List<CharSequence> sampleIdList = new ArrayList<CharSequence>();
            for (int i = 0; i < rowNumber; i++) {


                final String sampleId = sampleIds[i];
                aggregated.appendObject(idRefColumnIndex, sampleId);
                sampleIdList.add(sampleId);
                //  System.out.println("sampleId[i]: " + sampleIds[i]);
            }
            logger.start("Aggregating pathway features...");
            // indices already aggregated:
            final IntSet usedProbesetIndices = new IntOpenHashSet();
            for (final PathwayInfo pi : pathways) {
                final IntList probeIndices = pi.probesetIndices;
                // get the slice of source corresponding to the probesets in the pathway pi:
                final int numProbeIndices = probeIndices.size();
                if (numProbeIndices <= 1) {
                    // skip pathways with one probeset or less.
                    continue;
                }
                final double[][] slice = new double[/* columns */ numProbeIndices][/* rows */ rowNumber];   // probesets are columns, rows are samples
                final List<CharSequence> colIds = new ArrayList<CharSequence>(numProbeIndices);
                int columnIndex = 0;
                final MutableString[] probeIds = new MutableString[probeIndices.size()];

                for (final int probeIndex : probeIndices) {
                    // System.out.println("probeIndex: " + probeIndex);
                    final int columnIndexForProbeset = probeIndex + 1;
                    final double[] sourceColumnArray = source.getColumnValues(columnIndexForProbeset).getDoubles(); // add +1 for ID_REF
                    System.arraycopy(sourceColumnArray, 0, slice[columnIndex], 0, rowNumber);
                    probeIds[columnIndex] = new MutableString(source.getIdentifier(columnIndexForProbeset)).compact();

                    columnIndex++;
                    usedProbesetIndices.add(probeIndex);
                    colIds.add(source.getIdentifier(columnIndexForProbeset));
                }
                aggregate(source, datasetEndpointName, aggregated, sampleIdList, usedProbesetIndices,
                        pi, probeIndices, numProbeIndices, slice, colIds, probeIds, splitType, splitId);

                logger.update();
            }
            //System.out.println("aggregated.toString: "+aggregated.toString(aggregated, false));

            final int numPathwayFeatures = aggregated.getColumnNumber() - 1;
            copyOtherColumns(source, aggregated, usedProbesetIndices);
            final int otherProbesetNumber = aggregated.getColumnNumber() - numPathwayFeatures;
            logger.stop(String.format("Done aggregating features for dataset %s" +
                    ". Aggregated %d probeset features for %d pathways into %d. Left %d features unchanged.",
                    datasetEndpointName,
                    usedProbesetIndices.size(), pathways.size(), numPathwayFeatures,
                    otherProbesetNumber));
            //     System.out.println("aggregated.toString: " + aggregated.toString(aggregated, false));
            return aggregated;
        }
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
    protected abstract void aggregate(Table source, String datasetEndpointName, ArrayTable aggregated,
                                      List<CharSequence> sampleIdList, IntSet usedProbesetIndices,
                                      PathwayInfo pi, IntList probeIndices, int numProbeIndices,
                                      double[][] slice, List<CharSequence> colIds,
                                      final MutableString[] probeIds, String splitType, int splitId);

    /**
     * Copy all other probesets verbatim to aggregated table..
     *
     * @param source
     * @param aggregated
     * @param usedProbesetIndices
     */
    private void copyOtherColumns(final Table source, final ArrayTable aggregated,
                                  final IntSet usedProbesetIndices) {
        for (int columnIndex = 1; columnIndex < source.getColumnNumber(); columnIndex++) {
            final int probesetIndex = columnIndex - 1;
            if (!usedProbesetIndices.contains(probesetIndex)) {
                final int newColIndex =
                        aggregated.addColumn(source.getIdentifier(columnIndex), double.class);

                final ArrayTable.ColumnDescription values = source.getColumnValues(columnIndex);
                final double[] dValues = values.getDoubles();
                aggregated.reserve(newColIndex, source.getRowNumber());
                final Table.RowIterator ri = aggregated.firstRow();
                for (int i = 0; i < source.getRowNumber(); i++) {

                    aggregated.setValue(newColIndex, ri, dValues[i]);
                    ri.next();
                }
            }
        }
    }

    public abstract Logger getLog();
}
