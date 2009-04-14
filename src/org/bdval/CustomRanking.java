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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * @author Fabien Campagne
 *         Date: Sep 17, 2008
 *         Time: 3:49:48 PM
 */
public class CustomRanking {
    String datasetName;
    String endpointCode;
    /* Describes the strategy followed to create this ranking. */
    String typeOfRanking;
    /* In the order rank 1, rank 2, etc. where rank 1 is expected to have higher performance.*/
    ObjectArrayList<String> modelIds;
}
