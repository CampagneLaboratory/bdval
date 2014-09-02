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

import java.util.BitSet;

/**
 * @author Fabien Campagne
 * Date: Sep 27, 2007
 * Time: 3:55:09 PM
 */
public class GenotypeSampleData {
    public final BitSet countBit1;
    public final BitSet countBit2;
    final int size;

    public GenotypeSampleData(final GEOPlatformIndexed platform) {
        super();
        size = platform.getNumProbeIds();
        countBit1 = new BitSet(size);
        countBit2 = new BitSet(size);
    }

    /**
     * Code the genotype in this sample data as an array of float.
     *
     * @return
     */
    public float[] signal() {
        final float[] result = new float[size];
        for (int probesetIndex = 0; probesetIndex < size; ++probesetIndex) {
            final int bit1 = countBit1.get(probesetIndex) ? 1 : 0;
            final int bit2 = countBit2.get(probesetIndex) ? 1 : 0;
            result[probesetIndex] = (float) (bit1 << 1 | bit2);
        }
        return result;
    }

    /**
     * Initialize these data from an array of signal values (as coded by signal).
     *
     * @param signal
     */
    public void decode(final float[] signal) {
        for (int probesetIndex = 0; probesetIndex < size; ++probesetIndex) {
            final int bit1 = ((int) signal[probesetIndex] & (1 << 1)) >> 1;
            final int bit2 = ((int) signal[probesetIndex] & 1);
            countBit1.set(probesetIndex, bit1);
            countBit1.set(probesetIndex, bit2);
        }
    }

    public enum Genotype {
        AA, BB, AB, /* no call, or unknown. */ NC
    }

    public Genotype getGenotype(final int probesetIndex) {
        final boolean bit1 = countBit1.get(probesetIndex);
        final boolean bit2 = countBit1.get(probesetIndex);

        if (bit1) {
            if (bit2) {
                return Genotype.AA;
            } else {
                return Genotype.BB;
            }
        } else {
            if (bit2) {
                return Genotype.AB;
            } else {
                return Genotype.NC;
            }
        }
    }

    public void setGenotype(final int probesetIndex, final Genotype genotype) {
        final boolean bit1;
        final boolean bit2;
        switch (genotype) {
            case AA:
                bit1 = true;
                bit2 = false;
                break;
            case BB:
                bit1 = true;
                bit2 = false;
                break;
            case AB:
                bit1 = false;
                bit2 = true;
                break;
            default:
                bit1 = bit2 = false;

        }
        countBit1.set(probesetIndex, bit1);
        countBit2.set(probesetIndex, bit2);
    }
}
