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
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Write signal values of an array in a binary file format.
 *
 * @author Fabien Campagne Date: Aug 25, 2007 Time: 11:12:39 AM
 */
public class ArrayWriter {
    DataOutput dataOutput;
    private final String sampleListFilename;
    private FastBufferedOutputStream out;

    public ArrayWriter(final String basename, final GEOPlatformIndexed platform) throws IOException {
        super();
        final String outputFilename = basename + ArrayReader.ARRAY_DATA_BIN_SUFFIX;
        sampleListFilename = basename + ArrayReader.ARRAY_SAMPLES_TXT_SUFFIX;
        final String probesetIdsFilename = basename + ArrayReader.ARRAYS_PROBESETIDS_IO_SUFFIX;

        out = new FastBufferedOutputStream(new FileOutputStream(outputFilename));
        dataOutput = new DataOutputStream(out);
        BinIO.storeObject(platform.getProbeIds(), probesetIdsFilename);
        sampleIdSelection = new ObjectArrayList<MutableString>();
    }

    public void appendSample(final float[] signal, final MutableString sampleId) throws IOException {
        BinIO.storeFloats(signal, dataOutput);
        sampleIdSelection.add(sampleId);

    }

    final ObjectList<MutableString> sampleIdSelection;

    /**
     * Close this writer, release resources used for writing.
     *
     * @throws java.io.IOException If an error occurs closing IO resources.
     */
    public void close() throws IOException {
        try {
            out.flush();
            out.close();
            dataOutput = null;
            final PrintWriter pw = new PrintWriter(new FileWriter(sampleListFilename));
            for (final MutableString sampleId : sampleIdSelection) {
                sampleId.write(pw);
                pw.write("\n");
            }
            pw.close();
        } catch (IOException e) {
            System.out.println("Error writing list of samples.");
            e.printStackTrace();
            System.exit(10);
        }
    }
}
