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

/**
 * Store counts for SAGE tags.
 *
 * @author Fabien Campagne Date: Aug 15, 2007 Time: 6:23:03 PM
 */
public class SAGECountsSampleData {
    public final int[] count;

    public SAGECountsSampleData(final GEOPlatformIndexed platform) {
        super();
        count = new int[platform.getNumProbeIds()];

    }
}
