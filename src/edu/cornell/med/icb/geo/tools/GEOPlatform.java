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

import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import edu.mssm.crover.tables.readers.GeoPlatformFileReader;
import edu.mssm.crover.tables.readers.SyntaxErrorException;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang.ArrayUtils;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

/**
 * Represent a GEO platform. Provide support to find GenbankIds from probeset
 * IDs.
 *
 * @author Fabien Campagne Date: Mar 1, 2006 Time: 2:34:12 PM
 */
public class GEOPlatform {
    private final Object2ObjectMap probesetId2GenbankList;
    private int count;

    public GEOPlatform() {
        super();
        this.probesetId2GenbankList = new Object2ObjectOpenHashMap<String, String>();
    }

    /**
     * Read the platform information from a GEO platform file.
     *
     * @param filename
     * @throws SyntaxErrorException
     * @throws IOException
     */
    public void read(final String filename) throws SyntaxErrorException, IOException {
        if (filename == null) {
            return;
        }
        final GeoPlatformFileReader reader = new GeoPlatformFileReader();
        final Reader lowLevelReader;
        if (filename.endsWith(".gz")) {
            lowLevelReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)));
        } else {
            lowLevelReader = new FileReader(filename);
        }

        final Table platformFileContent = reader.read(lowLevelReader);
        final Table.RowIterator ri = platformFileContent.firstRow();

        int probeSetIdColumnIndex = -1;
        int genbankAcColumnIndex = -1;
        int genbankListColumnIndex = -1;
        int genbankColumnIndex = -1;
        final String probeIdColumnName = "ID";
        final String genbankAcColumnName = "GB_ACC";
        final String genbankListColumnName = "GB_LIST";

        try {
            probeSetIdColumnIndex = platformFileContent.getColumnIndex(probeIdColumnName);
        } catch (InvalidColumnException e) {
            assert false;
        }
        try {
            genbankAcColumnIndex = platformFileContent.getColumnIndex(genbankAcColumnName);
        } catch (InvalidColumnException e) { // NOPMD
            // OK, see below.
        }
        try {
            genbankListColumnIndex = platformFileContent.getColumnIndex(genbankListColumnName);
        } catch (InvalidColumnException e) { // NOPMD
            // OK, see below.
        }


        genbankColumnIndex = (genbankListColumnIndex != -1 ? genbankListColumnIndex : genbankAcColumnIndex);
        if (probeSetIdColumnIndex == -1 || genbankColumnIndex == -1) {
            throw new SyntaxErrorException(0, "One of the following column names could not be found in the platformFileContent description file: "
                    + probeIdColumnName + ", (at least one of : " + genbankAcColumnName + ", " + genbankListColumnName + " ).");
        }

        int count = 0;

        while (!ri.end()) {
            try {
                final String probesetId = (String) platformFileContent.getValue(probeSetIdColumnIndex, ri);
                final String genbankAccession = (String) platformFileContent.getValue(genbankColumnIndex, ri);
                probesetId2GenbankList.put(probesetId, genbankAccession);
                count++;
            } catch (TypeMismatchException e) {
                throw new InternalError("Column type does not match" + e.getMessage());
            } catch (InvalidColumnException e) {
                throw new InternalError("Should never happen");

            }
            ri.next();
        }
        this.count = count;
    }

    /**
     * Return the number of PrimaryIDSet on this platform.
     *
     * @return the number of PrimaryIDSet on this platform.
     */
    public int getProbesetCount() {
        return count;
    }

    /**
     * Returns the first GenBank ID found for this probeset.
     *
     * @param probesetID
     * @return One Genbank ID.
     */
    public String getGenbankId(final String probesetID) {
        return getGenbankList(probesetID)[0];
    }

    /**
     * Return all Genbank IDs associated to this probeset.
     *
     * @param probesetID
     * @return Genbank IDs associated to this probeset.
     */
    public String[] getGenbankList(final String probesetID) {
        final String gbList = (String) probesetId2GenbankList.get(probesetID);
        if (gbList == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        } else {
            return gbList.split("[ ]");
        }
    }
}
