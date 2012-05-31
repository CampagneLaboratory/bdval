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

import java.io.IOException;
import java.util.Set;
import java.util.Vector;

/**
 * @author Fabien Campagne Date: Mar 1, 2006 Time: 11:45:42 AM
 */
public abstract class GeneList {
    public static final int TYPE_FULL = 0;
    public static final int TYPE_RANDOM = 1;
    public static final int TYPE_FILE = 2;
    private final String type;

    public static int parseType(final String type) {
        if ("full".equals(type)) {
            return TYPE_FULL;
        } else if ("random".equals(type)) {
            return TYPE_RANDOM;
        } else {
            return TYPE_FILE;
        }
    }

    /**
     * Calculate the set of probeset identifiers that match the array for this
     * gene list.
     *
     * @param result Full array table.
     * @param idColumnIndex Column with probeset identifiers on the full array.
     * @return The set of probeset identifiers that match the array for this
     *         gene list.
     */
    public abstract Set<String> calculateProbeSetSelection(final Table result, final int idColumnIndex);

    /**
     * Calculate the set of probeset identifiers that match the array for this
     * gene list.
     *
     * @param tableProbesetIds Set of probeset ids on the array.
     * @return The set of probeset identifiers that match the array for this gene list.
     */
    public abstract Set<String> calculateProbeSetSelection(final ObjectSet<CharSequence> tableProbesetIds);

    protected GeneList(final String type) {
        super();
        this.type = type;
    }

    public static GeneList createList(final String[] tokens) throws IOException {
        return createList(tokens, "./");
    }

    public static GeneList createList(final String[] tokens, final String geneFeaturesDir) throws IOException {
        final String stringType = tokens[0];
        final int type = parseType(tokens[0]);
        switch (type) {
            case TYPE_FULL:
                return new FullGeneList(stringType);
            case TYPE_RANDOM:
                return new RandomGeneList(tokens);
            case TYPE_FILE:
                // System.out.println("Creating genes from " + geneFeaturesDir);
                return new FileGeneList(tokens, geneFeaturesDir);
            default:
                throw new InternalError("All cases must be accounted for.");
        }
    }

    public void setPlatform(final GEOPlatform platform) {
    }

    public String getType() {
        return type;
    }

    /**
     * Does this gene list contain the probeset?
     *
     * @param probesetId Probeset identifier.
     * @return True if this gene list contains this probeset identifier.
     */
    public abstract boolean isProbesetInList(String probesetId);

    public void setPlatforms(final Vector<GEOPlatform> platform) {
    }

    @Override
    public String toString() {
        return type;
    }
}
