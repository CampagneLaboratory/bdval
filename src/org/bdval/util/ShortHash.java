/*
 * Copyright (C) 2008-2010 Institute for Computational Biomedicine,
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

package org.bdval.util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.zip.CRC32;

/**
 * Utility class to create a short hash of a {@link String} or String[].
 * @author Kevin Dorff
 */
public class ShortHash {

    /** Logging. */
    private static final Logger LOG = Logger.getLogger(ShortHash.class);

    /** No constructor for utility class. */
    private ShortHash() {
        super();
    }

    /**
     * The characters to use in the hash.
     */
    private static final char[] HASH_CHARS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    /**
     * Five int-sized masks for the 5 parts of the hash.
     */
    private static final int[] MASKS = {
        Integer.parseInt("01111110000000000000000000000000", 2),
        Integer.parseInt("00000001111110000000000000000000", 2),
        Integer.parseInt("00000000000001111110000000000000", 2),
        Integer.parseInt("00000000000000000001111110000000", 2),
        Integer.parseInt("00000000000000000000000001111111", 2),
    };

    /**
     * Five int-sized shifts for the 5 parts of the hash
     * after the value has been masked.
     */
    private static final int[] MASK_SHIFTS = {
            25, 19, 13, 7, 0
        //Integer.parseInt("00000001111111111111111111111111", 2),
        //Integer.parseInt("00000000000001111111111111111111", 2),
        //Integer.parseInt("00000000000000000001111111111111", 2),
        //Integer.parseInt("00000000000000000000000001111111", 2),
        //Integer.parseInt("00000000000000000000000000000000", 2)
    };

    /**
     * Return a short hash (String of 5 chars, A-Z) of the contents of toHash.
     * @param toHash the content to hash
     * @return the short hash
     */
    public static String shortHash(final String[] toHash) {
        final StringBuilder argsSb = new StringBuilder();
        int count = 0;
        for (final String arg : toHash) {
            if (count++ > 0) {
                argsSb.append(' ');
            }
            argsSb.append(arg);
        }
        return shortHash(argsSb.toString());
    }

    /**
     * Return a short hash (String of 5 chars, A-Z) of the contents of toHash.
     * @param toHash the content to hash
     * @return the short hash
     */
    public static String shortHash(final String toHash) {
        if (StringUtils.isBlank(toHash)) {
            return null;
        }

        // Get the CRC32 checksum of the string (CRC will clash less often than the Adler checksum for short strings)
        final CRC32 crc32 = new CRC32();
        crc32.update(toHash.getBytes());
        // Map it from a long to an int with mod
        final int checksum = (int) (crc32.getValue() % Integer.MAX_VALUE);

        final StringBuilder output = new StringBuilder();
        for (int i = 0; i < MASKS.length; i++) {
            // Mask the value, shift it to the right, and mod it to the output-able characters
            final int partial = ((checksum & MASKS[i]) >> MASK_SHIFTS[i]) % HASH_CHARS.length;
            final char asChar = HASH_CHARS[partial];
            output.append(asChar);
        }
        LOG.debug(String.format("hash=%s for string=%s", output.toString(), toHash));
        return output.toString();
    }
}
