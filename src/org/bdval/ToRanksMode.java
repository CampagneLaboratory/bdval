package org.bdval;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.TypeMismatchException;
import edu.mssm.crover.tables.writers.InsightfulMinerTableWriter;

import java.util.Collections;
import java.util.Comparator;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
/*
 * Copyright (C) 2009 Institute for Computational Biomedicine,
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

/**
 * Converts a value dataset to a rank dataset. Rank datasets substitute the value of a feature signal in a sample by
 * the rank of the feature signal among all other features in the same sample.
 *
 * @author Fabien Campagne
 *         Date: Apr 21, 2009
 *         Time: 4:10:07 PM
 */
public class ToRanksMode extends DAVMode {
    @Override
    public void defineOptions(JSAP jsap) throws JSAPException {

    }

    class SignalValue {
        double value;
        int rowIndex;
        int rank;
    }

    final Comparator<SignalValue> signalValueComparator = new Comparator<SignalValue>() {
        public int compare(final SignalValue o1, final SignalValue o2) {
            return (int) Math.round(o1.value - o2.value);
        }
    };

    @Override
    public void process(DAVOptions options) {
        super.process(options);

        ArrayTable table = (ArrayTable) options.inputTable;
        ArrayTable destinationTable = new ArrayTable();
        destinationTable.defineColumnsFrom(table);

        try {
            String ids[] = table.getStrings("ID_REF");
            for (String id : ids) {
                destinationTable.appendObject(0, id);
            }
        } catch (InvalidColumnException e) {
            e.printStackTrace();
        }
        int numColumns = table.getColumnNumber();
        for (int colIndex = 1; colIndex < numColumns; colIndex++) {
            String colIdf = table.getIdentifier(colIndex);
            try {
                double[] values = table.getDoubles(colIdf);
                // convert values to ranks:
                ObjectList<SignalValue> sorted = new ObjectArrayList<SignalValue>();
                int index = 0;
                for (double value : values) {
                    SignalValue v = new SignalValue();
                    v.value = value;
                    v.rowIndex = index++;
                    sorted.add(v);
                }

                Collections.sort(sorted, signalValueComparator);
                // assign ranks:
                double lastValue = Double.NaN;
                int lastRank = 0;
                for (SignalValue signal : sorted) {
                    if (signal.value != lastValue) {
                        signal.rank = lastRank + 1;
                    } else signal.rank = lastRank;

                    lastRank = signal.rank;
                    lastValue = signal.value;
                }

                // put the ranks back into the table:
                for (SignalValue signal : sorted) {
                    values[signal.rowIndex] = signal.rank;
                }
                for (double value : values) {
                    destinationTable.appendDoubleValue(colIndex, value);
                }
            } catch (InvalidColumnException e) {
                // Cannot happen
            } catch (TypeMismatchException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Writing ranked dataset to output file.");
        InsightfulMinerTableWriter.writeData(destinationTable, options.output);
        options.output.flush();
    }
}
