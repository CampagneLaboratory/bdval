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

package edu.cornell.med.icb.geo;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.lang.MutableString;

/**
 * Associates identifiers to indices, both ways.
 *
 * @author Fabien Campagne
 *         Date: Mar 2, 2008
 *         Time: 2:47:19 PM
 */
public class DoubleIndexedIdentifier {
    private final IndexedIdentifier ids;
    private final Int2ObjectMap<MutableString> reverseMap;

    public DoubleIndexedIdentifier(final IndexedIdentifier ids) {
        super();
        this.ids = ids;
        reverseMap = new Int2ObjectOpenHashMap<MutableString>();
        for (final MutableString transcriptId : ids.keySet()) {
            reverseMap.put(ids.getInt(transcriptId), transcriptId);
        }
    }

    public int getIndex(final MutableString id) {
        return ids.getInt(id);
    }

    public int getIndex(final String id) {
        return ids.getInt(new MutableString(id));
    }

    public MutableString getId(final int index) {
        return reverseMap.get(index);
    }

    public int size() {
        return reverseMap.size();
    }
}
