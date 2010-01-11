/*
 * Copyright (C) 2006-2010 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.geo.tools;

import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.RowProcessor;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.HashSet;
import java.util.Set;

/**
 * @author campagne Date: Mar 1, 2006 Time: 12:04:50 PM
 */
public class FullGeneList extends GeneList {
    public FullGeneList(final String type) {
        super(type);
    }

    @Override
    public boolean isProbesetInList(final String probesetId) {
        return true; // each probeset id is in the full gene list.
    }

    @Override
    public Set<String> calculateProbeSetSelection(final Table result, final int idColumnIndex) {
        final Set<String> probesetIds = new HashSet<String>();

        // Each value in the ID column is a probeset id.
        final RowProcessor collect = new RowProcessor(new int[]{idColumnIndex}) {
            @Override
            public void processRow(final Table table, final Table.RowIterator ri)
                    throws TypeMismatchException, InvalidColumnException {
                probesetIds.add(result.getValue(idColumnIndex, ri).toString());
            }
        };
        try {
            result.processRows(collect);
        } catch (TypeMismatchException e) {
            assert false : "This exception must not be thrown, but was: " + e;
        } catch (InvalidColumnException e) {
            assert false : "This exception must not be thrown, but was: " + e;
        }
        return probesetIds;
    }

    @Override
    public Set<String> calculateProbeSetSelection(final ObjectSet<CharSequence> tableProbesetIds) {
        final ObjectSet<String> result=new ObjectOpenHashSet<String>();
        for (final CharSequence id: tableProbesetIds) {
            result.add(id.toString());
        }
        return result;
    }
}
