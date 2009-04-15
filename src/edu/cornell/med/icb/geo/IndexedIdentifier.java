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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.lang.math.NumberUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Maintains a mapping between identifiers and indices.
 *
 * @author Fabien Campagne Date: Aug 15, 2007 Time: 4:46:03 PM
 */
public class IndexedIdentifier extends Object2IntOpenHashMap<MutableString>
        implements PropertyMappable {
    /**
     * Used for serialization.
     */
    private static final long serialVersionUID = -1829319805452451069L;
    private static final int UNDEFINED_VALUE = -1;

    /**
     * A running counter used to set along with registering an identifier.
     * NOTE: This is not thread-safe - the runningIndex should probably be an
     * {@link java.util.concurrent.atomic.AtomicInteger} instead.
     */
    private int runningIndex;

    /** Key name to use for the id2Index property. */
    private static final String ID2_INDEX_KEY = "id2Index";

    /** Key to use for the runningIndex property. */
    private static final String RUNNING_INDEX_KEY = "runningIndex";

    /**
     * Create a new empty IndexedIdentifier.
     */
    public IndexedIdentifier() {
        super();
        defaultReturnValue(UNDEFINED_VALUE);
    }

    /**
     * Create a new IndexedIdentifier that is initialized using the data in
     * the supplied map of properties.
     * @param propertyMap A map of properties indexed by string key names that can be then
     * used to reconstruct the object state.
     * @see #fromPropertyMap(java.util.Map)
     */
    public IndexedIdentifier(final Map<String, Properties> propertyMap) {
        this();
        fromPropertyMap(propertyMap);
    }

    public int registerIdentifier(final MutableString id) {
        int index = getInt(id);
        if (index == UNDEFINED_VALUE) {
            index = runningIndex++;
            put(id, index);
        }
        return index;
    }

    /**
     * Creates and returns a copy of this object.
     * @return a clone of this instance.
     */
    @Override
    public Object clone() {
        final IndexedIdentifier clone = (IndexedIdentifier) super.clone();
        clone.runningIndex = runningIndex;
        return clone;
    }

    /**
     * Store the current state of the object to a map of properties.
     * @return A map of properties indexed by string key names that can be then
     * used to reconstruct the object state at a later time.
     */
    public Map<String, Properties> toPropertyMap() {
        final Map<String, Properties> propertyMap = new HashMap<String, Properties>();

        // store the values contained in the Id to Index map
        final Properties id2IndexProperties = new Properties();
        for (final Map.Entry<MutableString, Integer> entry : entrySet()) {
            id2IndexProperties.setProperty(entry.getKey().toString(), entry.getValue().toString());
        }
        propertyMap.put(ID2_INDEX_KEY, id2IndexProperties);

        // store the runningIndex
        final Properties runningIndexProperties = new Properties();
        runningIndexProperties.put(RUNNING_INDEX_KEY, Integer.toString(runningIndex));
        propertyMap.put(RUNNING_INDEX_KEY, runningIndexProperties);

        return propertyMap;
    }

    /**
     * Set the current state of the object from a map of properties.
     * @param propertyMap A map of properties indexed by string key names that can be then
     * used to reconstruct the object state.
     */
    public void fromPropertyMap(final Map<String, Properties> propertyMap) {
        if (propertyMap == null) {
            throw new IllegalArgumentException("Property map cannot be null");
        }

        clear();   // reset any previous state

        // get the values contained in the Id to Index map
        final Properties id2IndexProperties = propertyMap.get(ID2_INDEX_KEY);
        if (id2IndexProperties != null) {
            for (final Map.Entry<Object, Object> entry : id2IndexProperties.entrySet()) {
                put(new MutableString(entry.getKey().toString()),
                        NumberUtils.toInt(entry.getValue().toString()));
            }
        }

        // get the running index
        final Properties runningIndexProperties = propertyMap.get(RUNNING_INDEX_KEY);
        if (runningIndexProperties != null) {
            runningIndex = NumberUtils.toInt(runningIndexProperties.getProperty(RUNNING_INDEX_KEY));
        } else {
            runningIndex = 0;
        }
    }
}
