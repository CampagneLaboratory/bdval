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

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author campagne Date: Mar 1, 2006 Time: 11:01:08 AM
 */
public class ConditionIdentifiers {

    private final Object2ObjectMap<String, Set<String>> conditionToIdentifiers;

    public ConditionIdentifiers() {
        super();
        conditionToIdentifiers = new Object2ObjectArrayMap<String, Set<String>> ();
    }

    @Override
    public String toString() {
        return "cids= "+conditionToIdentifiers.toString();
    }

    public void addIdentifier(final String condition, final String identifier) {
        final String internedCondition = condition.intern();
        Set<String> idGroup = conditionToIdentifiers.get(internedCondition);
        if (idGroup == null) {
            idGroup = new HashSet<String>();
        }
        idGroup.add(identifier.intern());
        conditionToIdentifiers.put(internedCondition, idGroup);
    }

    public boolean conditionExists(final String conditionName) {
        return conditionToIdentifiers.containsKey(conditionName.intern());
    }

    public Set<String> getLabelGroup(final String conditionName) {
        return conditionToIdentifiers.get(conditionName.intern());
    }

    /**
     * Returns the condition for the specified identifier
     * (example: can be used to retrieve the class from a sample name).
     * @param identifier the identifier to get the condition for.
     * @return the condition or null if the identifier wasn't found
     */
    public String conditionForIdentifier(final String identifier) {
        for (final Map.Entry<String, Set<String>> entry : conditionToIdentifiers.entrySet()) {
            if (entry.getValue().contains(identifier.intern())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Return the number of samples represented in this cids object.
     * @return
     */
    public int size() {
        int size=0;
        for (Set<String> val: conditionToIdentifiers.values()) {
            size+=val.size();
        }
        return size;
    }
}
