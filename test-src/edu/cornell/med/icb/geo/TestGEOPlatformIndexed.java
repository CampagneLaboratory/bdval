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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

/**
 * Validates the functionality of the {@link edu.cornell.med.icb.geo.IndexedIdentifier}
 * class.
 */
public class TestGEOPlatformIndexed {
    /**
     * Validates that an empty {@link edu.cornell.med.icb.geo.GEOPlatformIndexed} object
     * is transformed to properties properly.
     */
    @Test
    public void emptyObject() {
        final GEOPlatformIndexed geoPlatformIndexed = new GEOPlatformIndexed();
        final Map<String, Properties> propertyMap = geoPlatformIndexed.toPropertyMap();
        assertNotNull("Property map should never be null", propertyMap);
        assertFalse("Property map should never be empty", propertyMap.isEmpty());

        // 2 IndexedIdentifier objects which each have 2 properties + all the rest
        assertEquals("Property map should have 9 keys", 9, propertyMap.size());

        // don't test the IndexedIdentifiers here, just validate that there is at least
        // one entry for each local member variable - let the IndexedIdentifier tests
        // validate the contents

        // IndexedIdentifier probeIds2ProbeIndex
        int probeIds2ProbeIndexCount = 0;
        for (final Map.Entry<String, Properties> entry : propertyMap.entrySet()) {
            if (entry.getKey().startsWith("probeIds2ProbeIndex.")) {
                probeIds2ProbeIndexCount++;
            }
        }
        assertTrue("There should be at least one entry for probeIds2ProbeIndex", probeIds2ProbeIndexCount >= 1);

        // IndexedIdentifier externalIds2TranscriptIndex
        int externalIds2TranscriptIndexCount = 0;
        for (final Map.Entry<String, Properties> entry : propertyMap.entrySet()) {
            if (entry.getKey().startsWith("externalIds2TranscriptIndex.")) {
                externalIds2TranscriptIndexCount++;
            }
        }
        assertTrue("There should be at least one entry for externalIds2TranscriptIndex", externalIds2TranscriptIndexCount >= 1);

        // Int2IntMap probeIndex2ExternalIDIndex
        final Properties probeIndex2ExternalIDIndexProperties = propertyMap.get("probeIndex2ExternalIDIndex");
        assertNotNull("probeIndex2ExternalIDIndex property must not be null", probeIndex2ExternalIDIndexProperties);
        assertTrue("probeIndex2ExternalIDIndex property should be empty", probeIndex2ExternalIDIndexProperties.isEmpty());
        assertEquals("probeIndex2ExternalIDIndex property should have no elements", 0, probeIndex2ExternalIDIndexProperties.size());

        // MutableString externalIdType
        final Properties externalIdTypeProperties = propertyMap.get("externalIdType");
        assertNotNull("externalIdType property must not be null", externalIdTypeProperties);
        assertTrue("externalIdType property should be empty", externalIdTypeProperties.isEmpty());
        assertEquals("externalIdType property should have no elements", 0, externalIdTypeProperties.size());

        // MutableString name
        final Properties nameProperties = propertyMap.get("name");
        assertNotNull("Name property must not be null", nameProperties);
        assertTrue("Name property should be empty", nameProperties.isEmpty());
        assertEquals("Name property should have no elements", 0, nameProperties.size());

        // Int2ObjectMap<MutableString> externalIndex2Id
        final Properties externalIndex2IdProperties = propertyMap.get("externalIndex2Id");
        assertNotNull("externalIndex2Id property must not be null", externalIndex2IdProperties);
        assertTrue("externalIndex2Id property should be empty", externalIndex2IdProperties.isEmpty());
        assertEquals("externalIndex2Id property should have no elements", 0, externalIndex2IdProperties.size());

        // Int2ObjectMap<MutableString> probeIndex2probeId`
        final Properties probeIndex2probeIdProperties = propertyMap.get("probeIndex2probeId");
        assertNotNull("probeIndex2probeId property must not be null", probeIndex2probeIdProperties);
        assertTrue("probeIndex2probeId property should be empty", probeIndex2probeIdProperties.isEmpty());
        assertEquals("probeIndex2probeId property should have no elements", 0, probeIndex2probeIdProperties.size());
    }

    /**
     * Validates that a populated {@link edu.cornell.med.icb.geo.GEOPlatformIndexed} object
     * is transformed to properties properly.
     */
    @Test
    public void toPropertyMap() {
        // TODO
    }

    /**
     * Validates that a set of properties populates an
     * {@link edu.cornell.med.icb.geo.GEOPlatformIndexed} object properly.
     */
    @Test
    public void fromPropertyMap() {
        // TODO
    }
}
