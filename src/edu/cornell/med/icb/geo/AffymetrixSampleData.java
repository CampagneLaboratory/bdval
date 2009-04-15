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

import java.util.BitSet;

/**
 * Stores affymetrix data signals and present/absence calls.
 *
 * @author Fabien Campagne Date: Aug 15, 2007 Time: 6:11:04 PM
 */
public class AffymetrixSampleData {
    public final float[] signal;
    public final BitSet presentAbsentCalls;

    public AffymetrixSampleData(final GEOPlatformIndexed platform) {
        super();
        signal = new float[platform.getNumProbeIds()];
        presentAbsentCalls = new BitSet(platform.getNumProbeIds());
    }
}
