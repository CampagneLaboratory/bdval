/*
 * Copyright (C) 2006-2009 Institute for Computational Biomedicine,
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author campagne Date: Mar 3, 2006 Time: 11:47:32 AM
 */
public class FixedGeneList extends GeneList {
    private final HashSet<String> probesetIds;

    public FixedGeneList(final String[] fixedProbesetIds) {
        super("fixed");
        this.probesetIds = new HashSet<String>();
        probesetIds.addAll(Arrays.asList(fixedProbesetIds));
    }

    @Override
    public Set<String> calculateProbeSetSelection(final Table result, final int idColumnIndex) {
        return probesetIds;
    }

    @Override
    public Set<String> calculateProbeSetSelection(final ObjectSet<CharSequence> tableProbesetIds) {
      return probesetIds;
    }

    @Override
    public boolean isProbesetInList(final String probesetId) {
        return probesetIds.contains(probesetId);
    }

    public int getNumberOfProbesets() {
        return probesetIds.size();
    }
}
