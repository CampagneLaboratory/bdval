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

package edu.cornell.med.icb.geo.binaryarray;

import edu.cornell.med.icb.identifier.IndexedIdentifier;
import it.unimi.dsi.fastutil.floats.FloatIterator;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.LineIterator;
import it.unimi.dsi.lang.MutableString;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads signals from an array, stored in binary format.
 *
 * @author Fabien Campagne Date: Aug 25, 2007 Time: 11:29:02 AM
 */
public class ArrayReader {
    private final DataInput dataInput;
    private final IndexedIdentifier probesetIds;
    private IndexedIdentifier sampleIds;
    private final InputStream stream;
    private ObjectList<MutableString> sampleIdList;
    public static final String ARRAY_DATA_BIN_SUFFIX = "-array-data.bin";
    public static final String ARRAY_SAMPLES_TXT_SUFFIX = "-array-samples.txt";
    public static final String ARRAYS_PROBESETIDS_IO_SUFFIX = "-arrays-probesetids.io";
    private final FloatIterator signalIterator;

    /**
     * Get sample ids for this array, in the order in which they were written.
     *
     * @return sample ids for this array.
     */
    public ObjectList<MutableString> getSampleIdList() {
        return sampleIdList;
    }

    /**
     * Return a map from sample ids to sample index (for access through this reader).
     */
    public IndexedIdentifier getSampleIds() {
        return sampleIds;
    }

    /**
     * Construct a reader to read binary array data.
     *
     * @param basename The basename of the binary array data.
     * @throws FileNotFoundException If some binary array files cannot be found relative to the provided basename.
     */
    public ArrayReader(final MutableString basename) throws IOException, ClassNotFoundException {
        super();
        final String inputFilename = basename.toString() + ARRAY_DATA_BIN_SUFFIX;
        final String sampleListFilename = basename.toString() + ARRAY_SAMPLES_TXT_SUFFIX;
        final String probesetIdsFilename = basename + ARRAYS_PROBESETIDS_IO_SUFFIX;

        readSampleIds(sampleListFilename);
        stream = new FastBufferedInputStream(new FileInputStream(inputFilename));
        dataInput = new DataInputStream(stream);
        signalIterator = BinIO.asFloatIterator(dataInput);
        probesetIds = (IndexedIdentifier) BinIO.loadObject(probesetIdsFilename);

    }

    protected ObjectList<MutableString> readSampleIds(final String sampleIdFilename) {
        sampleIdList = new it.unimi.dsi.fastutil.objects.ObjectArrayList<MutableString>();
        sampleIds = new IndexedIdentifier();
        try {
            final LineIterator it = new LineIterator(new FastBufferedReader(new FileReader(sampleIdFilename)));
            while (it.hasNext()) {
                final MutableString sampleIdSelected = it.next();
                if (sampleIdSelected.startsWith("#")) {
                    continue;
                }
                final MutableString id = sampleIdSelected.copy().compact();
                sampleIdList.add(id);
                sampleIds.registerIdentifier(id);
            }
            return sampleIdList;
        } catch (FileNotFoundException e) {
            System.err.println("Cannot open sample id filename " + sampleIdFilename);
            System.exit(1);
        }
        sampleIds = null;
        return null;
    }

    /**
     * Allocate an array of the appropriate size to store all signal values for a sample of the array.
     *
     * @return An array of the appropriate size to store all signal values for a sample of the array.
     */
    public float[] allocateSignalArray() {
        return new float[probesetIds.size()];
    }

    /**
     * Read signal values for the next available sample in the binary file.
     *
     * @param signal
     */
    public void readNextSample(final float[] signal) throws IOException {
        int count = 0;
        while (signalIterator.hasNext() && count < signal.length) {
            final float signalValue = signalIterator.nextFloat();
            signal[count++] = signalValue;
        }
        //  BinIO.loadFloats(dataInput, signal);
    }

    /**
     * Release resources used by this reader.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        stream.close();
    }
}
