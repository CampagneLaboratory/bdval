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

/**
 * Implementations will decide how much to parse, and where to put the data.
 *
 * @author Fabien Campagne Date: Aug 15, 2007 Time: 5:51:34 PM
 */
public abstract class SampleDataCallback<T> {
    protected final GEOPlatformIndexed platform;
    protected final T parsedData;

    protected SampleDataCallback(final GEOPlatformIndexed platform, final T parsedData) {
        super();
        this.platform = platform;
        this.parsedData = parsedData;
    }

    /**
     * Extract the index of the probe for this line of sample data.
     *
     * @param line One line of sample data.
     * @return Index of the probe on this platform.
     */
    public int getProbeIndex(final MutableString line) {
        int indexSpaceCharacter = line.indexOf(' ');
        if (indexSpaceCharacter == -1) {
            indexSpaceCharacter = line.indexOf('\t');
        }
        final MutableString probeId = line.substring(0, indexSpaceCharacter).trim();
        return platform.getProbeIds().getInt(probeId);
    }

    /**
     * Parse one line of the GEO sample table data.
     *
     * @param line One line of sample data.
     */
    public abstract void parse(MutableString line);

    public T getParsedData() {
        return parsedData;
    }

    /**
     * The name of data columns for this sample. For instance "ID_REF VALUE".
     * Informs the callback about the type of columns and the order of these
     * columns in the sample data.
     */
    protected MutableString columnNames;
    protected String[] columnNamesArray;
    protected int probeIdColumnIndex = -1; // ID_REF column
    protected int signalColumnIndex = -1;  // VALUE column
    protected int presenceAbsenceColumnIndex = -1;   // ABS_CALL
    protected int presenceAbsencePValueColumnIndex = -1;  // SC1_DET_P alternative P-value for detection

    /**
     * Let this callback know what column names to expect for this sample. The
     * callback has the ability to determine which column can be reliably
     * interpreted and read from the sample data lines.
     *
     * @param columnNames One line of column names, separated by space or tab,
     * in the order in which each column appears in sample data lines.
     */
    public void setColumnNames(final MutableString columnNames) {
        this.columnNames = columnNames.copy();
        columnNamesArray = this.columnNames.toString().split("[ \t]+");
    }

    protected boolean canParseSampleData;

    /**
     * Returns True if this callback can parse the type of columns for this
     * sample. False otherwise.
     *
     * @return True or False.
     */
    public abstract boolean canParse();

    /**
     * Get the column index for the specific column id.
     *
     * @param columnId id of the column
     * @return index for this column id in the sample data lines.
     */
    protected int getIndex(final String columnId) {
        for (int i = 0; i < columnNamesArray.length; i++) {
            if (columnNamesArray[i].equals(columnId)) {
                return i;
            }
        }
        return -1;
    }

    public MutableString getColumnNames() {
        return columnNames;
    }

    private final float detectionPValueThreshold = 0.05f;


    protected boolean determinePresenceCallPValue(final String detectionPValue) {
        return Float.parseFloat(detectionPValue) < detectionPValueThreshold;
    }
}
