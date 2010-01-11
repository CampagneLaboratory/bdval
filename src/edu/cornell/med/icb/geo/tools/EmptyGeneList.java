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

import edu.mssm.crover.tables.Table;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.HashSet;
import java.util.Set;

/**
 * This gene list is always empty.
 *
 * @author Fabien Campagne Date: Mar 3, 2006 Time: 11:33:03 AM
 */
public class EmptyGeneList extends GeneList {
    protected EmptyGeneList() {
        super("empty");
    }

    @Override
    public Set<String> calculateProbeSetSelection(final Table result, final int idColumnIndex) {
        return new HashSet<String>();
    }

    @Override
    public Set<String> calculateProbeSetSelection(final ObjectSet<CharSequence> tableProbesetIds) {
        return new HashSet<String>();
    }

    @Override
    public boolean isProbesetInList(final String probesetId) {
        return false;
    }
}
