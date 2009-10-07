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

package org.bdval.util;

import static junit.framework.Assert.assertEquals;
import org.apache.commons.lang.ArrayUtils;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * Validates that the {@link org.bdval.util.ShortHash} functions return correct and consistent
 * values.
 *
 * @author Kevin Dorff
 */
public class TestShortHash {

    @Test
    public void testShortHash() {
        assertNull(ShortHash.shortHash((String) null));
        assertNull(ShortHash.shortHash(""));
        assertNull(ShortHash.shortHash(" "));
        assertNull(ShortHash.shortHash("             "));
        assertNull(ShortHash.shortHash(ArrayUtils.EMPTY_STRING_ARRAY));

        assertEquals("PHMJM", ShortHash.shortHash("A"));
        assertEquals("PHMJM", ShortHash.shortHash(new String[] {"A"}));

        assertEquals("OZKTS", ShortHash.shortHash("asdfasdf"));
        assertEquals("OZKTS", ShortHash.shortHash(new String[] {"asdfasdf"}));

        assertEquals("FIWCB", ShortHash.shortHash("foo bar"));
        assertEquals("FIWCB", ShortHash.shortHash(new String[] {"foo", "bar"}));

        final String stringWithQuotes =
                "Checks if a String is not empty (\"\"), not null and not whitespace only.";
        assertEquals("GDEQM", ShortHash.shortHash(stringWithQuotes));
        assertEquals("GDEQM", ShortHash.shortHash(stringWithQuotes.split(" ")));

        final String stringWithArgs = "--this=that --fudge=cicle --cat=dog";
        assertEquals("ADQVZ", ShortHash.shortHash(stringWithArgs));
        assertEquals("ADQVZ", ShortHash.shortHash(stringWithArgs.split(" ")));
    }
}
