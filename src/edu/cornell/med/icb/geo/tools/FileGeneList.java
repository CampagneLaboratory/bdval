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

import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.RowProcessor;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * @author Fabien Campagne Date: Mar 1, 2006 Time: 12:08:31 PM
 */
public class FileGeneList extends GeneList {
    private String filename;
    private Set<String> primaryIDSet;
    private Set<String> genbankIDsSet;
    private Set<String> refseqIDsSet;
    /**
     * Number of columns in the header.  If it is 4, use probes (4th column).
     */
    private int columnNumber;

    /**
     * Get probe ids in this gene list.
     * @return A set of string with the probe ids found in the gene list file.
     */
    public Set<String> getProbeIDsSet() {
        return probeIDsSet;
    }

    private Set<String> probeIDsSet;
    private Set<String> matchingGeneIds;
    private Set<String> cachedProbesetIds;
    private Vector<GEOPlatform> platforms;
    private final String geneFeaturesDir;

    public FileGeneList(final String[] tokens) throws IOException {
        this(tokens, "./");
    }

    public FileGeneList(final String[] tokens, final String geneFeaturesDir) throws IOException {
        super(tokens[0]);
        this.filename = tokens[1];
        this.geneFeaturesDir = geneFeaturesDir;
        readFilename();
    }

    public Vector<GEOPlatform> getPlatforms() {
        return platforms;
    }

    @Override
    public void setPlatform(final GEOPlatform platform) {
        this.platforms = new Vector<GEOPlatform>();
        platforms.add(platform);
    }

    @Override
    public void setPlatforms(final Vector<GEOPlatform> platforms) {
        this.platforms = platforms;
    }

    @Override
    public boolean isProbesetInList(final String probesetId) {
        assert cachedProbesetIds != null : "set of probesets matching the table must be available."
                + " Call calculateProbeSetSelection first.";
        //  if (cachedProbesetIds == null) {
        //      cachedProbesetIds = calculateProbeSetSelection(convert(probeIDsSet));
        //  }
        for (final String cachedProbeId : cachedProbesetIds) {
            if (cachedProbeId.equals(probesetId)) {
                return true;
            }
        }
        return false;
    }

    private ObjectSet<CharSequence> convert(final Set<String> probeIDsSet) {
        final ObjectSet<CharSequence> result = new ObjectOpenHashSet<CharSequence>();
        for (final String each : probeIDsSet) {
            result.add(each);
        }
        return result;
    }

    private void readFilename() throws IOException {
        primaryIDSet = new ObjectOpenHashSet<String>();
        genbankIDsSet = new ObjectOpenHashSet<String>();
        refseqIDsSet = new ObjectOpenHashSet<String>();
        probeIDsSet = new ObjectOpenHashSet<String>();
        matchingGeneIds = new ObjectOpenHashSet<String>();

        BufferedReader probesetsInAListReader = null;
        try {
            probesetsInAListReader =
                    new BufferedReader(new FileReader(geneFeaturesDir + filename));
            String line;
            int lineCount = 0;
            while ((line = probesetsInAListReader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (lineCount == 1) {
                    columnNumber = line.split("[\t]").length;
                }
                final String[] tokens = line.split("[\t]");
                if (tokens.length < 1) {
                    throw new IllegalArgumentException("Reading data for one gene list from"
                            + geneFeaturesDir + filename
                            + ". Line must have at least 1 field. Line was: " + line);
                }
                primaryIDSet.add(tokens[0]);
                if (tokens.length >= 2) {
                    genbankIDsSet.add(tokens[1].intern());
                }

                if (tokens.length >= 3) {
                    refseqIDsSet.add(tokens[2].intern());
                }
                if (tokens.length >= 4) {
                    probeIDsSet.add(tokens[3].intern());
                }
                lineCount++;
            }
        } finally {
            IOUtils.closeQuietly(probesetsInAListReader);
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    @Override
    public Set<String> calculateProbeSetSelection(final Table result, final int idColumnIndex) {
        if (platforms == null) {
            throw new IllegalArgumentException(
                    "Platform information must be provided. (platform field cannot be null).");
        }
        final Set<String> probesetIds = new HashSet<String>();

        // Each value in the ID column is a probeset id.
        final RowProcessor collect = new RowProcessor(new int[]{idColumnIndex}) {
            @Override
            public void processRow(final Table table, final Table.RowIterator ri)
                    throws TypeMismatchException, InvalidColumnException {
                final String probesetValue = (String) result.getValue(idColumnIndex, ri);
                boolean add = false;

                if (columnNumber >= 4) {
                    add = matchesProbesetId(probesetValue, add);
                } else {
                    add = matchesPlatform(probesetValue, add);
                }
                if (add) {
                    probesetIds.add(probesetValue);
                }
            }
        };
        try {
            result.processRows(collect);
        } catch (TypeMismatchException e) {
            assert false : "This exception must not be thrown, but was: " + e;
        } catch (InvalidColumnException e) {
            assert false : "This exception must not be thrown, but was: " + e;
        }
        cachedProbesetIds = probesetIds;
        return probesetIds;
    }

    private boolean matchesProbesetId(final CharSequence probesetValue, boolean add) {
        for (final String probeId : probeIDsSet) { // for each probe in the gene list file...
            if (probeId.length() >= 1 && probesetValue.equals(probeId)) {
                matchingGeneIds.add(probesetValue.toString());
                add = true;
            }
        }
        return add;
    }

    private boolean matchesPlatform(final CharSequence probesetValue, boolean add) {
        if (add) {
            return true;
        }
        for (final GEOPlatform platform : platforms) {  // check for match on each platform:
            final String[] genbankIDs = platform.getGenbankList(probesetValue.toString());
            // GEO GB_LIST contains RefSeq and Genbank IDs indifferentially.

            for (final String genbankID : genbankIDs) {
                if (genbankID.length() >= 1 && (genbankIDsSet.contains(genbankID) ||
                        refseqIDsSet.contains(genbankID))) { // we found at least one Genbank ID in the genelist:
                    // keep this probeset
                    matchingGeneIds.add(genbankID);
                    add = true;
                }
            }
        }
        return add;
    }

    @Override
    public Set<String> calculateProbeSetSelection(final ObjectSet<CharSequence> tableProbesetIds) {
        cachedProbesetIds = new ObjectOpenHashSet<String>();
        for (final CharSequence id : tableProbesetIds) {
            final String stringId = id.toString().intern();
            boolean add = false;
            add = matchesProbesetId(id, add);
            add = matchesPlatform(id, add);
            if (add) {
                cachedProbesetIds.add(stringId);
            }
        }
        return cachedProbesetIds;
    }

    public Set<String> getPrimaryIDs() {
        return this.primaryIDSet;
    }

    @Override
    public String toString() {
        return filename;
    }
}
