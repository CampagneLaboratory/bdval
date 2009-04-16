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

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import edu.cornell.med.icb.identifier.IndexedIdentifier;

/**
 * @author Fabien Campagne Date: Aug 15, 2007 Time: 4:36:10 PM
 */
public class GEOPlatformIndexed implements Serializable, PropertyMappable {
    private static final long serialVersionUID = -8911477023162911861L;
    private static final int UNDEFINED_VALUE = -1;

    final IndexedIdentifier probeIds2ProbeIndex = new IndexedIdentifier();
    final IndexedIdentifier externalIds2TranscriptIndex = new IndexedIdentifier();
    final Int2IntMap probeIndex2ExternalIDIndex = new Int2IntOpenHashMap();
    MutableString externalIdType;
    MutableString name;
    private final Int2ObjectMap<MutableString> externalIndex2Id;
    private final Int2ObjectMap<MutableString> probeIndex2probeId;

    /** Key name to use for the probeIds2ProbeIndex property. (the trailing "." is intentional)*/
    private static final String PROBE_IDS2_PROBE_INDEX_KEY = "probeIds2ProbeIndex.";
    /** Key name to use for the externalIds2TranscriptIndex property. (the trailing "." is intentional. */
    private static final String EXTERNAL_IDS2_TRANSCRIPT_INDEX_KEY = "externalIds2TranscriptIndex.";
    /** Key name to use for the probeIndex2ExternalIDIndex property. */
    private static final String PROBE_INDEX2_EXTERNAL_IDINDEX_KEY = "probeIndex2ExternalIDIndex";
    /** Key name to use for the externalIdType property. */
    private static final String EXTERNAL_ID_TYPE_KEY = "externalIdType";
    /** Key name to use for the name property. */
    private static final String NAME_KEY = "name";
    /** Key name to use for the externalIndex2Id property. */
    private static final String EXTERNAL_INDEX2_ID_KEY = "externalIndex2Id";
    /** Key name to use for the probeIndex2probeId property. */
    private static final String PROBE_INDEX2PROBE_ID_KEY = "probeIndex2probeId";

    /**
     * Create a new empty GEOPlatformIndexed.
     */
    public GEOPlatformIndexed() {
        super();
        probeIds2ProbeIndex.defaultReturnValue(UNDEFINED_VALUE);
        externalIndex2Id = new Int2ObjectRBTreeMap<MutableString>();
        probeIndex2probeId = new Int2ObjectRBTreeMap<MutableString>();
    }

    /**
     * Create a new GEOPlatformIndexed that is initialized using the data in
     * the supplied map of properties.
     * @param propertyMap A map of properties indexed by string key names that can be then
     * used to reconstruct the object state.
     * @see #fromPropertyMap(java.util.Map)
     */
    public GEOPlatformIndexed(final Map<String, Properties> propertyMap) {
        this();
        fromPropertyMap(propertyMap);
    }

    /**
     * Name of the platform (i.e., GPL96).
     * @return The name of the platform
     */
    public MutableString getName() {
        return name;
    }

    /**
     * Name of the platform (i.e., GPL96).
     * @param name The name of the platform
     */
    public void setName(final MutableString name) {
        this.name = name;
    }

    public MutableString getExternalIdType() {
        return externalIdType;
    }

    public void setExternalIdType(final MutableString externalIdType) {
        this.externalIdType = externalIdType;
    }

    public void registerProbeId(final String probeId, final String externalIdentifier) {
        registerProbeId(new MutableString(probeId).compact(),
                new MutableString(externalIdentifier).compact());
    }

    public void registerProbeId(final MutableString probeId,
                                final MutableString externalIdentifier) {
        final int probeIndex = probeIds2ProbeIndex.registerIdentifier(probeId);
        probeIndex2probeId.put(probeIndex, probeId);
        final int externalTranscriptIndex =
                externalIds2TranscriptIndex.registerIdentifier(externalIdentifier);
        externalIndex2Id.put(externalTranscriptIndex, externalIdentifier);
        probeIndex2ExternalIDIndex.put(probeIndex, externalTranscriptIndex);
    }

    public MutableString getExternalId(final int externalIndex) {
        return externalIndex2Id.get(externalIndex);
    }

    /**
     * Returns the mapping from probe ids to probe indices.
     */
    public IndexedIdentifier getProbeIds() {
        return probeIds2ProbeIndex;
    }

    /**
     * Returns the mapping from external ids (Genbank/GI) to external Gi/Genbank entry index.
     */
    public IndexedIdentifier getExternalIds() {
        return probeIds2ProbeIndex;
    }

    /**
     * Return the number of probe identifiers on this platform.
     *
     * @return number of probe identifiers on this platform.
     */
    public int getNumProbeIds() {
        return probeIds2ProbeIndex.size();
    }

    public int getExternalIndexForProbeId(final MutableString probeId) {
        final int probeIndex = probeIds2ProbeIndex.get(probeId);
        return probeIndex2ExternalIDIndex.get(probeIndex);
    }

    /**
     * Returns the probe identifier.
     *
     * @param probeIndex Index of the probeset.
     * @return the probeset identifier for this probe index.
     */
    public MutableString getProbesetIdentifier(final int probeIndex) {
        return probeIndex2probeId.get(probeIndex);
    }

    /**
     * Store the current state of the object to a map of properties.
     * @return A map of properties indexed by string key names that can be then
     * used to reconstruct the object state at a later time.
     */
    public Map<String, Properties> toPropertyMap() {
        final Map<String, Properties> propertyMap = new HashMap<String, Properties>();

        // for "complex" objects, we get the property map for the object, and prefix the
        // keys with the name of the member variable - this way, we know how a set of
        // properties gets mapped back to the appropriate object

        // IndexedIdentifier probeIds2ProbeIndex
        final Map<String, Properties> probeIds2ProbeIndexPropertyMap = probeIds2ProbeIndex.toPropertyMap();
        for (final Map.Entry<String, Properties> entry : probeIds2ProbeIndexPropertyMap.entrySet()) {
            propertyMap.put(PROBE_IDS2_PROBE_INDEX_KEY + entry.getKey(), entry.getValue());
        }

        // IndexedIdentifier externalIds2TranscriptIndex
        final Map<String, Properties> externalIds2TranscriptIndexPropertyMap = externalIds2TranscriptIndex.toPropertyMap();
        for (final Map.Entry<String, Properties> entry : externalIds2TranscriptIndexPropertyMap.entrySet()) {
            propertyMap.put(EXTERNAL_IDS2_TRANSCRIPT_INDEX_KEY + entry.getKey(), entry.getValue());
        }

        // for collection or map objects, the elements are stored as a set of properties
        // and keyed to the name of the member variable

        // Int2IntMap probeIndex2ExternalIDIndex
        final Properties probeIndex2ExternalIDIndexProperties = new Properties();
        for (final Map.Entry<Integer, Integer> entry : probeIndex2ExternalIDIndex.entrySet()) {
            probeIndex2ExternalIDIndexProperties.put(entry.getKey().toString(), entry.getValue().toString());
        }
        propertyMap.put(PROBE_INDEX2_EXTERNAL_IDINDEX_KEY, probeIndex2ExternalIDIndexProperties);

        // for "simple" objects, the elements are stored as a single property
        // keyed to the name of the member variable.  The side effect of this is that
        // the property key for simple objects appears twice.  We introduce a bit of
        // redundancy for the sake of keeping properties "generic" to avoid special case handling

        // MutableString externalIdType
        final Properties externalIdTypeProperties = new Properties();
        if (externalIdType != null) {
            externalIdTypeProperties.put(EXTERNAL_ID_TYPE_KEY, externalIdType.toString());
        }
        propertyMap.put(EXTERNAL_ID_TYPE_KEY, externalIdTypeProperties);

        // MutableString name
        final Properties nameProperties = new Properties();
        if (name != null) {
            nameProperties.put(NAME_KEY, name.toString());
        }
        propertyMap.put(NAME_KEY, nameProperties);

        // Int2ObjectMap<MutableString> externalIndex2Id
        final Properties externalIndex2IdProperties = new Properties();
        for (final Map.Entry<Integer, MutableString> entry : externalIndex2Id.entrySet()) {
            externalIndex2IdProperties.put(entry.getKey().toString(), entry.getValue().toString());
        }
        propertyMap.put(EXTERNAL_INDEX2_ID_KEY, externalIndex2IdProperties);

        // Int2ObjectMap<MutableString> probeIndex2probeId
        final Properties probeIndex2probeIdProperties = new Properties();
        for (final Map.Entry<Integer, MutableString> entry : probeIndex2probeId.entrySet()) {
            probeIndex2probeIdProperties.put(entry.getKey().toString(), entry.getValue().toString());
        }
        propertyMap.put(PROBE_INDEX2PROBE_ID_KEY, probeIndex2probeIdProperties);

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

        // IndexedIdentifier probeIds2ProbeIndex
        final Map<String, Properties> probeIds2ProbeIndexPropertyMap = new HashMap<String, Properties>();
        for (final Map.Entry<String, Properties> entry : propertyMap.entrySet()) {
            final String key = entry.getKey();
            if (key.startsWith(PROBE_IDS2_PROBE_INDEX_KEY)) {
                probeIds2ProbeIndexPropertyMap.put(StringUtils.removeStart(key, PROBE_IDS2_PROBE_INDEX_KEY), entry.getValue());
            }
        }
        probeIds2ProbeIndex.fromPropertyMap(probeIds2ProbeIndexPropertyMap);

        // IndexedIdentifier externalIds2TranscriptIndex
        final Map<String, Properties> externalIds2TranscriptIndexPropertyMap = new HashMap<String, Properties>();
        for (final Map.Entry<String, Properties> entry : propertyMap.entrySet()) {
            final String key = entry.getKey();
            if (key.startsWith(EXTERNAL_IDS2_TRANSCRIPT_INDEX_KEY)) {
                externalIds2TranscriptIndexPropertyMap.put(StringUtils.removeStart(key, EXTERNAL_IDS2_TRANSCRIPT_INDEX_KEY), entry.getValue());
            }
        }
        externalIds2TranscriptIndex.fromPropertyMap(externalIds2TranscriptIndexPropertyMap);

        // Int2IntMap probeIndex2ExternalIDIndex
        probeIndex2ExternalIDIndex.clear();   // reset any previous state
        final Properties probeIndex2ExternalIDIndexProperties = propertyMap.get(PROBE_INDEX2_EXTERNAL_IDINDEX_KEY);
        if (probeIndex2ExternalIDIndexProperties != null) {
            for (final Map.Entry<Object, Object> entry : probeIndex2ExternalIDIndexProperties.entrySet()) {
                probeIndex2ExternalIDIndex.put(NumberUtils.toInt(entry.getKey().toString()), NumberUtils.toInt(entry.getValue().toString()));
            }
        }

        // MutableString externalIdType
        final Properties externalIdTypeProperties = propertyMap.get(EXTERNAL_ID_TYPE_KEY);
        if (externalIdTypeProperties != null) {
            externalIdType = new MutableString(StringUtils.defaultString(externalIdTypeProperties.getProperty(EXTERNAL_ID_TYPE_KEY)));
        } else {
            externalIdType = null;
        }

        // MutableString name
        final Properties nameProperties = propertyMap.get(NAME_KEY);
        if (nameProperties != null) {
            name = new MutableString(StringUtils.defaultString(nameProperties.getProperty(NAME_KEY)));
        } else {
            name = null;
        }

        // Int2ObjectMap<MutableString> externalIndex2Id
        externalIndex2Id.clear();             // reset any previous state
        final Properties externalIndex2IdProperties = propertyMap.get(EXTERNAL_INDEX2_ID_KEY);
        if (externalIndex2IdProperties != null) {
            for (final Map.Entry<Object, Object> entry : externalIndex2IdProperties.entrySet()) {
                externalIndex2Id.put(NumberUtils.toInt(entry.getKey().toString()), new MutableString(entry.getValue().toString()));
            }
        }

        // Int2ObjectMap<MutableString> probeIndex2probeId
        probeIndex2probeId.clear();           // reset any previous state
        final Properties probeIndex2probeIdProperties = propertyMap.get(PROBE_INDEX2PROBE_ID_KEY);
        if (probeIndex2probeIdProperties != null) {
            for (final Map.Entry<Object, Object> entry : probeIndex2probeIdProperties.entrySet()) {
                probeIndex2probeId.put(NumberUtils.toInt(entry.getKey().toString()), new MutableString(entry.getValue().toString()));
            }
        }
    }
}
