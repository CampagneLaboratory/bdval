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
 * @author Fabien Campagne
 * Date: Sep 27, 2007
 * Time: 3:54:08 PM
 */
public class AffySnpChipCountsSampleDataCallback extends SampleDataCallback<GenotypeSampleData> {
    public AffySnpChipCountsSampleDataCallback(final GEOPlatformIndexed platform) {
        super(platform, new GenotypeSampleData(platform));
    }

    private final Pattern spaceSplitter = Pattern.compile("[ \t]+");

    @Override
    public void parse(final MutableString line) {
        final int probeIndex = getProbeIndex(line);
        final String[] tokens = spaceSplitter.split(line.toString(), 0);
        final String signalValue = tokens[1];
        if (probeIndex != -1) {
            parsedData.setGenotype(probeIndex, GenotypeSampleData.Genotype.valueOf(signalValue));
        }
    }

    @Override
    public boolean canParse() {
        canParseSampleData = false;
        // Required field are ID_REF,VALUE,DETECTION P-VALUE

        probeIdColumnIndex = getIndex("ID_REF");
        signalColumnIndex = getIndex("VALUE");
        presenceAbsencePValueColumnIndex = getIndex("DETECTION");

        if (probeIdColumnIndex != -1 && signalColumnIndex != -1 && presenceAbsencePValueColumnIndex != -1) {
            return true;
        } else {
            return false;
        }
    }
}
