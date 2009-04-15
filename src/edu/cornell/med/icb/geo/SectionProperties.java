/*
 * Copyright (C) 2007-2009 Institute for Computational Biomedicine,
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

import it.unimi.dsi.lang.MutableString;
import org.apache.commons.collections.map.MultiValueMap;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Fabien Campagne Date: Aug 15, 2007 Time: 4:02:33 PM
 */
class SectionProperties extends MultiValueMap {
    public MutableString getUniqueValue(final MutableString key) {
        if (size(key) != 1) {
            return null;
        }
        final Collection values = getCollection(key);
        if (values == null) {
            return null;
        }
        if (values.size() == 1) {
            final Iterator iterator = values.iterator();

            return (MutableString) iterator.next();
        } else {
            return null;
        }
    }

    public MutableString getUniqueValue(final String key) {
        return getUniqueValue(new MutableString(key).compact());
    }

    public int size(final String key) {
        return size(new MutableString(key).compact());
    }

    public Collection getCollection(final String key) {
        return getCollection(new MutableString(key).compact());
    }
}
