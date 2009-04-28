/*
 * Copyright (C) 2009 Institute for Computational Biomedicine,
 *                    Weill Medical College of Cornell University
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

import java.util.Map;
import java.util.Properties;

/**
 * Classes that implement this interface are capable of storing and loading there
 * state to and from property objects.
 */
public interface PropertyMappable {
    /**
     * Store the current state of the object to a map of properties.
     * @return A map of properties indexed by string key names that can be then
     * used to reconstruct the object state at a later time.
     */
    Map<String, Properties> toPropertyMap();

    /**
     * Set the current state of the object from a map of properties.
     * @param propertyMap A map of properties indexed by string key names that can be then
     * used to reconstruct the object state.
     */
    void fromPropertyMap(Map<String, Properties> propertyMap);
}
