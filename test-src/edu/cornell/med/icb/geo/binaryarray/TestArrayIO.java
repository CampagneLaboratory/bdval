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

package edu.cornell.med.icb.geo.binaryarray;

import edu.cornell.med.icb.geo.GEOPlatformIndexed;
import it.unimi.dsi.lang.MutableString;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * Test ArrayReader and ArrayWriter.
 *
 * @author Fabien Campagne
 *         Date: Aug 25, 2007
 *         Time: 2:39:08 PM
 */
public class TestArrayIO extends TestCase {

    public void testWriteReadBack() throws IOException, ClassNotFoundException {
        final GEOPlatformIndexed platform = new GEOPlatformIndexed();
        for (int i = 1; i < 99; i++) {
            platform.registerProbeId("probeId" + i, "external" + i);
        }

        final String basename = "test-results/test-array-writer";
        final ArrayWriter writer = new ArrayWriter(basename, platform);
        try {
            final float[] signal = new float[platform.getProbeIds().size()];
            int counter = 0;
            for (int sampleIndex = 0; sampleIndex < 10; sampleIndex++) {

                for (int i = 0; i < signal.length; i++) {
                    signal[i] = counter++;

                    // fill in signal array with increasing integer values.  0 for element zero of signal array in sample 0,
                    // 1 for next element, and so on incrementing by one until all signal values and samples are written.
                }
                final MutableString sampleIdString = new MutableString();
                sampleIdString.append(Integer.toString(sampleIndex));
                writer.appendSample(signal, sampleIdString);
            }
        } finally {
            writer.close();
        }
        // check that values read match those that were written. First value must be zero, next value in signal increases

        final ArrayReader reader = new ArrayReader(new MutableString(basename));
        try {
            int countExpected = 0;
            final float[] signalRead = reader.allocateSignalArray();
            for (int sampleIndex = 0; sampleIndex < reader.getSampleIdList().size(); sampleIndex++) {
                reader.readNextSample(signalRead);
                for (final float valueRead : signalRead) {
                    assertEquals((float) countExpected++, valueRead);
                }
            }
        } finally {
            reader.close();
        }
    }
}
