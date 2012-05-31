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

package edu.cornell.med.icb.geo.tools;

/**
 * A platform with no information except the probeset ids on the chip.
 *
 * @author Fabien Campagne Date: Oct 23, 2007 Time: 4:29:12 PM
 */
public class DummyPlatform extends GEOPlatform {
    @Override
    public String[] getGenbankList(final String probesetID) {
        final String[] result = new String[1];
        result[0] = probesetID;
        return result;
    }

    @Override
    public String getGenbankId(final String probesetID) {
        return probesetID;
    }

    @Override
    public void read(final String filename) {

    }
}
