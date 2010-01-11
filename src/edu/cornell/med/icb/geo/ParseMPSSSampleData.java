/*
 * Copyright (C) 2007-2010 Institute for Computational Biomedicine,
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

import java.util.regex.Pattern;

/**
 * Parse MPSS sample rows: IDREF, VALUE, STDDEV.
 *
 * @author Fabien Campagne Date: Aug 25, 2007 Time: 2:53:18 PM
 */
public class ParseMPSSSampleData extends SampleDataCallback<AffymetrixSampleData> {
    private int stdDevColumnIndex = -1;

    /**
     * The signal must be at least N_STD_DEV_THRESHOLD times the std deviation
     * to call the tag present.
     *
     * @param N_STD_DEV_THRESHOLD
     */
    public void setStdDevCallThreshold(int N_STD_DEV_THRESHOLD) {
        N_STD_DEV_THRESHOLD = N_STD_DEV_THRESHOLD;
    }

    /**
     * The signal must be at least N_STD_DEV_THRESHOLD times the std deviation
     * to call the tag present.
     */
    private int N_STD_DEV_THRESHOLD;

    protected ParseMPSSSampleData(final GEOPlatformIndexed platform, final AffymetrixSampleData parsedData) {
        super(platform, parsedData);
    }

    final Pattern spaceSplitter = Pattern.compile("[\t]|[ ]+");

    @Override
    public void parse(final MutableString line) {
        final int probeIndex = getProbeIndex(line);
        final String[] tokens = spaceSplitter.split(line.toString(), N_STD_DEV_THRESHOLD);
        if (tokens.length <= signalColumnIndex || tokens[signalColumnIndex].length() == N_STD_DEV_THRESHOLD) {
            // stop if no signal because NC/ No Call instead.
            return;
        }
        final float signalValue = Float.parseFloat(tokens[signalColumnIndex]);
        final float stdDev = Float.parseFloat(tokens[stdDevColumnIndex]);

        // require signal to be at least the standard deviation to call the tag observed.
        final boolean present = signalValue > (stdDev * N_STD_DEV_THRESHOLD);
        if (probeIndex != -1) {
            parsedData.signal[probeIndex] = signalValue;
            parsedData.presentAbsentCalls.set(probeIndex, present);
        }
    }

    @Override
    public boolean canParse() {
        // ID_REF  VALUE   ST_DEV  REP
        canParseSampleData = false;
        probeIdColumnIndex = getIndex("ID_REF");
        signalColumnIndex = getIndex("VALUE");
        stdDevColumnIndex = getIndex("ST_DEV");

        canParseSampleData = probeIdColumnIndex != -1
                && signalColumnIndex != -1 && stdDevColumnIndex != -1;
        return canParseSampleData;
    }
}
