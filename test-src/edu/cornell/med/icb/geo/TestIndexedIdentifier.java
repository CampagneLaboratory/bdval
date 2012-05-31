/*
 * Copyright (C) 2008-2010 Institute for Computational Biomedicine,
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

import edu.cornell.med.icb.identifier.IndexedIdentifier;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.lang.StringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Validates the functionality of the {@link edu.cornell.med.icb.identifier.IndexedIdentifier}
 * class.
 */
public class TestIndexedIdentifier {
    /**
     * Validates that an empty {@link edu.cornell.med.icb.identifier.IndexedIdentifier} object
     * is transformed to properties properly.
     */
    @Test
    public void emptyObject() {
        final IndexedIdentifier indexedIdentifier = new IndexedIdentifier();
        assertTrue("Initial state should be empty", indexedIdentifier.isEmpty());
        assertEquals("Initial state should have no elements", 0, indexedIdentifier.size());
        assertEquals("Default value", -1, indexedIdentifier.defaultReturnValue());

        final Map<String, Properties> propertyMap = indexedIdentifier.toPropertyMap();
        assertNotNull("Property map should never be null", propertyMap);
        assertFalse("Property map should never be empty", propertyMap.isEmpty());
        assertEquals("Property map should have 2 keys", 2, propertyMap.size());

        final Properties id2IndexProperties = propertyMap.get("id2Index");
        assertNotNull("id2Index map should never be null", id2IndexProperties);
        assertTrue("id2Index map should be empty", id2IndexProperties.isEmpty());
        assertEquals("id2Index map should have no elements", 0, id2IndexProperties.size());

        final Properties runningIndexProperties = propertyMap.get("runningIndex");
        assertNotNull("runningIndex map should never be null", runningIndexProperties);
        assertFalse("runningIndex map should not be empty", runningIndexProperties.isEmpty());
        assertEquals("runningIndex map should have one element", 1, runningIndexProperties.size());

        final String runningIndexProperty = runningIndexProperties.getProperty("runningIndex");
        assertNotNull("runningIndex must not be null", runningIndexProperty);
        assertTrue("runningIndex must not be blank", StringUtils.isNotBlank(runningIndexProperty));
        assertEquals("runningIndex should be zero", 0, Integer.parseInt(runningIndexProperty));
    }

    /**
     * Validates that a populated {@link edu.cornell.med.icb.identifier.IndexedIdentifier} object
     * is transformed to properties properly.
     */
    @Test
    public void toPropertyMap() {
        final IndexedIdentifier indexedIdentifier = new IndexedIdentifier();
        indexedIdentifier.registerIdentifier(new MutableString("one"));
        indexedIdentifier.registerIdentifier(new MutableString("two"));
        indexedIdentifier.registerIdentifier(new MutableString("three"));

        assertFalse("State should not be empty", indexedIdentifier.isEmpty());
        assertEquals("State should have 3 elements", 3, indexedIdentifier.size());
        assertEquals("Default value", -1, indexedIdentifier.defaultReturnValue());

        final Map<String, Properties> propertyMap = indexedIdentifier.toPropertyMap();
        assertNotNull("Property map should never be null", propertyMap);
        assertFalse("Property map should never be empty", propertyMap.isEmpty());
        assertEquals("Property map should have 2 keys", 2, propertyMap.size());

        final Properties id2IndexProperties = propertyMap.get("id2Index");
        assertNotNull("id2Index map should never be null", id2IndexProperties);
        assertFalse("id2Index map should not be empty", id2IndexProperties.isEmpty());
        assertEquals("id2Index map should have three elements", 3, id2IndexProperties.size());

        assertEquals("Element one", "0", id2IndexProperties.getProperty("one"));
        assertEquals("Element two", "1", id2IndexProperties.getProperty("two"));
        assertEquals("Element three", "2", id2IndexProperties.getProperty("three"));

        final Properties runningIndexProperties = propertyMap.get("runningIndex");
        assertNotNull("runningIndex map should never be null", runningIndexProperties);
        assertFalse("runningIndex map should not be empty", runningIndexProperties.isEmpty());
        assertEquals("runningIndex map should have one element", 1, runningIndexProperties.size());

        final String runningIndexProperty = runningIndexProperties.getProperty("runningIndex");
        assertNotNull("runningIndex must not be null", runningIndexProperty);
        assertTrue("runningIndex must not be blank", StringUtils.isNotBlank(runningIndexProperty));
        assertEquals("runningIndex should be three", 3, Integer.parseInt(runningIndexProperty));
    }

    /**
     * Validates that a set of properties populates an
     * {@link edu.cornell.med.icb.identifier.IndexedIdentifier} object properly.
     */
    @Test
    public void fromPropertyMap() {
        final Map<String, Properties> propertyMap = new HashMap<String, Properties>();
        final Properties id2IndexProperties = new Properties();
        id2IndexProperties.put("one", "0");
        id2IndexProperties.put("two", "1");
        id2IndexProperties.put("three", "2");
        propertyMap.put("id2Index", id2IndexProperties);

        final Properties runningIndexProperties = new Properties();
        runningIndexProperties.put("runningIndex", "3");
        propertyMap.put("runningIndex", runningIndexProperties);

        final IndexedIdentifier indexedIdentifier = new IndexedIdentifier(propertyMap);
        assertFalse("Initial state should not be empty", indexedIdentifier.isEmpty());
        assertEquals("Initial state should have 3 elements", 3, indexedIdentifier.size());
        assertEquals("Default value", -1, indexedIdentifier.defaultReturnValue());

        assertEquals("Element one", 0, indexedIdentifier.getInt(new MutableString("one")));
        assertEquals("Element two", 1, indexedIdentifier.getInt(new MutableString("two")));
        assertEquals("Element three", 2, indexedIdentifier.getInt(new MutableString("three")));

        // now add another element
        indexedIdentifier.registerIdentifier(new MutableString("four"));
        assertEquals("State should now have 4 elements", 4, indexedIdentifier.size());
        assertEquals("Element four", 3, indexedIdentifier.getInt(new MutableString("four")));
    }
}
