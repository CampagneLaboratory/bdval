/*
 * Copyright (C) 2009 Institute for Computational Biomedicine,
 *                    Weill Medical College of Cornell University
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.bdval.util;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;

/**
 * Describe class here.
 *
 * @author Kevin Dorff
 */
public class TestShortHash {

    @Test
    public void testShortHash() {
        assertEquals("AIQAO", ShortHash.shortHash("A"));
        assertEquals("HSWGJ", ShortHash.shortHash("asdfasdf"));
        assertEquals("UGEWV", ShortHash.shortHash("Checks if a String is not empty (\"\"), not null and not whitespace only."));
        assertEquals("QTGXF", ShortHash.shortHash("--this=that --fudge=cicle --cat=dog"));
    }
}
