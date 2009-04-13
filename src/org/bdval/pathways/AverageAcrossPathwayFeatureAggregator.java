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

package org.bdval.pathways;

import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.Table;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.lang.MutableString;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author Fabien Campagne
 *         Date: Apr 29, 2008
 *         Time: 3:46:13 PM
 */
public class AverageAcrossPathwayFeatureAggregator extends PathwayFeatureAggregator {
    private static final Logger LOG = Logger.getLogger(AverageAcrossPathwayFeatureAggregator.class);

    protected void aggregate(final Table source, final String datasetEndpointName, final ArrayTable aggregated,
                             final List<CharSequence> sampleIdList, final IntSet usedProbesetIndices, final PathwayInfo pi,
                             final IntList probeIndices, final int numProbeIndices, final double[][] slice, final List<CharSequence> colIds,
                             final MutableString[] probeIds, final String splitType, final int splitId) {

        // double[][] slice = new double[/* columns */ numProbeIndices][/* rows */ rowNumber];   // probesets are columns, rows are samples

        // create new feature called pathway_average.
        final String newColumnId = pi.pathwayId.toString() + "_average";
        //        System.out.println("adding column " + newColumnId);
        final int newColumnIndex = aggregated.addColumn(newColumnId, double.class);
        aggregated.reserve(newColumnIndex, source.getRowNumber());
        final Table.RowIterator ri = aggregated.firstRow();
        for (int rowIndex = 0; rowIndex < slice[0].length; rowIndex++) {   // for each sample:
            // calculate the average of the probeset signal values:
            double sum = 0;
            final double numColumns = slice.length;
            for (int colIndex = 0; colIndex < slice.length; colIndex++) {
                sum += slice[colIndex][rowIndex];
            }

            final double average = sum / numColumns;
            // and store in the result table as a feature called :
            aggregated.setValue(newColumnIndex, ri, average);
            assert !ri.end() : String.format("reached final row of aggregated table j=%d,"
                    + " aggregated.getRowNumber()=%d ",
                    rowIndex, aggregated.getRowNumber());

            ri.next();
        }
    }

    @Override
    public Logger getLog() {
        return LOG;
    }
}
