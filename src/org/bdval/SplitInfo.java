/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.bdval;

/**
 * @author Fabien Campagne
 * Date: Apr 2, 2008
 * Time: 5:00:20 PM
 */
public class SplitInfo {
    final int splitId;
    final int repeatId;
    final int foldId;
    final String splitType;
    final String sampleId;
    final double label;
    final int sampleIndex;

    public SplitInfo(final int splitId, final int repeatId, final int foldId, final String splitType, final String sampleId, final double label, final int sampleIndex) {
        super();
        this.splitId = splitId;
        this.repeatId = repeatId;
        this.foldId = foldId;
        this.splitType = splitType;
        this.sampleId = sampleId;
        this.label = label;
        this.sampleIndex = sampleIndex;
    }
}
