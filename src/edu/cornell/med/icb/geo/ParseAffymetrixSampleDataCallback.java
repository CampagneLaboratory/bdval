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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Pattern;

/**
 * Parse Affymetrix signal and present/absent calls. Present absent calls are determined as follows: 1. If the column
 * ABS_CALL is present, assume 'P' means present and everything else absent. 2. if one of the columns DET_P,  P_VALUE,
 * or  SC1_DET_P is present, assume the value is a p-value. If the p-value is less than 0.05, assume the probe is
 * present. 3. If none of this worked, look at the signal, assume the probe is present when signal>50.
 *
 * @author Fabien Campagne Date: Aug 15, 2007 Time: 6:12:51 PM
 */
class ParseAffymetrixSampleDataCallback extends SampleDataCallback<AffymetrixSampleData> {
    private final float signalThresholdForDetection = 50;
    private static final Log LOG =
            LogFactory.getLog(ParseAffymetrixSampleDataCallback.class);

    public ParseAffymetrixSampleDataCallback(final GEOPlatformIndexed platform) {
        super(platform, new AffymetrixSampleData(platform));
    }

    final Pattern spaceSplitter = Pattern.compile("[\t]|[ ]+");

    @Override
    public void parse(final MutableString line) {
        final int probeIndex = getProbeIndex(line);
        final String[] tokens = spaceSplitter.split(line.toString(), 0);
        if (tokens.length <= signalColumnIndex || tokens[signalColumnIndex].length() == 0) {
            // stop if no signal because NC/ No Call instead.
            return;
        }
        float signalValue = 0;
        try {
            signalValue = Float.parseFloat(tokens[signalColumnIndex]);

            boolean present = false;
            if (presenceAbsenceColumnIndex < tokens.length && presenceAbsencePValueColumnIndex < tokens.length) {
                present = presenceAbsenceColumnIndex != -1 ? determinePresenceCall(tokens[presenceAbsenceColumnIndex]) :
                        presenceAbsencePValueColumnIndex != -1 ? determinePresenceCallPValue(
                                tokens[presenceAbsencePValueColumnIndex]) :
                                signalValue > signalThresholdForDetection;
            } else {
                present = signalValue > signalThresholdForDetection;
            }

            if (probeIndex != -1) {
                parsedData.signal[probeIndex] = signalValue;
                parsedData.presentAbsentCalls.set(probeIndex, present);
            }
        } catch (NumberFormatException e) {
            LOG.debug("Cannot parse signal value " + tokens[signalColumnIndex]);
        }
    }


    @Override
    public boolean canParse() {

        canParseSampleData = false;
        probeIdColumnIndex = getIndex("ID_REF");
        signalColumnIndex = getIndex("VALUE");
        presenceAbsenceColumnIndex = getIndex("ABS_CALL");
        presenceAbsencePValueColumnIndex = getIndex("DET_P");
        if (presenceAbsencePValueColumnIndex == -1) {
            presenceAbsencePValueColumnIndex = getIndex("P_VALUE");
        }
        if (presenceAbsencePValueColumnIndex == -1) {
            presenceAbsencePValueColumnIndex = getIndex("SC1_DET_P");
        }

        if (probeIdColumnIndex != -1 && signalColumnIndex != -1) {
            return true;
        } else {
            return false;
        }
    }


    private boolean determinePresenceCall(final String token) {
        if (token.length() == 0) {
            return false;
        }
        switch (token.charAt(0)) {
            case 'P':
                return true;
            case 'A':
                return false;
            default:
                return false;
        }
    }
}
