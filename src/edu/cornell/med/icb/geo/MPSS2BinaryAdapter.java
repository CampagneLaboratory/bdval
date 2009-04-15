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

package edu.cornell.med.icb.geo;

import edu.cornell.med.icb.geo.binaryarray.ArrayWriter;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;

import java.io.IOException;

/*
 * Writes the array signals into a binary format where each signal value is represented as a float.
 * @author Fabien Campagne
 * Date: Aug 25, 2007
 * Time: 10:32:34 AM
 *
 */
public class MPSS2BinaryAdapter implements FormatAdapter {
    private GeoScanOptions options;

    public SampleDataCallback getCallback(final GEOPlatformIndexed platform) {
        return new ParseMPSSSampleData(platform, new AffymetrixSampleData(platform));
    }

    public void analyzeSampleData(final GEOPlatformIndexed platform,
                                  final SampleDataCallback callback,
                                  final MutableString sampleIdentifier) {
        assert options.tpr != null : "probe to transcript mapping must be provided.";
        if (callback.canParse()) {
            final AffymetrixSampleData data = (AffymetrixSampleData) callback.getParsedData();
            int present = 0;
            // replace signal values that are not observed by 0
            final IntSet transcriptsSeen = new IntRBTreeSet();

            for (int i = 0; i < data.signal.length; ++i) {
                if (!data.presentAbsentCalls.get(i)) {
                    data.signal[i] = 0;
                } else {
                    present++;
                    transcriptsSeen.add(options.tpr.probeset2TranscriptIndex(i));
                }
            }
            System.out.println(String.format("present: %d #transcripts seen: %d",
                    (int) ((((float) data.presentAbsentCalls.cardinality()) / (float) platform.getProbeIds().size()) * 100),
                    transcriptsSeen.size()));
            try {
                arrayWriter.appendSample(data.signal, sampleIdentifier);

            } catch (IOException e) {
                System.err.println("Cannot write to binary output file. ");
                System.exit(1);
            }
        }

    }

    private ArrayWriter arrayWriter;

    public void preSeries(final GEOPlatformIndexed platform) {
        try {
            arrayWriter = new ArrayWriter(platform.getName().toString(), platform);

        } catch (IOException e) {
            System.err.println("Cannot write to binary array representation . ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void postSeries(final GEOPlatformIndexed platform, final ObjectList<MutableString> sampleIdSelection) {
        try {
            arrayWriter.close();
        } catch (IOException e) {
            System.out.println("Error writing list of samples used for normalization.");
            e.printStackTrace();
            System.exit(10);
        }
    }

    public void setOptions(final GeoScanOptions options) {
        this.options = options;
    }
}
