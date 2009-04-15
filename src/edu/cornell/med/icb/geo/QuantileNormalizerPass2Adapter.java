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
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/*
 * A signal adapter that parses Affymetrix GP96 platform soft files and normalize the signal of each probeset
 * by the mean signal for the quantile (produced by QuantileNormalizerPass1Adapter).
 *
 * @author Fabien Campagne
 * Date: Aug 20, 2007
 * Time: 1:35:09 PM
 *
 */
public class QuantileNormalizerPass2Adapter implements FormatAdapter {
    private final Comparator<? super ProbeSignal> comparator = new Comparator<ProbeSignal>() {
        public int compare(final ProbeSignal o1, final ProbeSignal o2) {
            return Double.compare(o1.signal, o2.signal);
        }
    };

    private ArrayWriter writer;

    public SampleDataCallback getCallback(final GEOPlatformIndexed platform) {
        return new ParseAffymetrixSampleDataCallback(platform);
    }

    ProbeSignal[] signals;
    Int2FloatMap accumulator;

    public void analyzeSampleData(final GEOPlatformIndexed platform,
                                  final SampleDataCallback callback,
                                  final MutableString sampleIdentifier) {
        if (callback.canParse()) {
            final AffymetrixSampleData data = (AffymetrixSampleData) callback.getParsedData();
            int i = 0;
            for (final ProbeSignal ps : signals) {
                ps.signal = data.signal[i];
                ps.probeIndex = i;
                ++i;
            }
            Arrays.sort(signals, comparator);
            int rank = 1;
            for (final ProbeSignal ps : signals) {
                ps.rank = rank;
                final float accumulation = accumulator.get(rank);
                ps.signal = accumulation;
                data.signal[ps.probeIndex] = accumulation;
                ++rank;
            }
            try {
                writer.appendSample(data.signal, sampleIdentifier);

            } catch (IOException e) {
                System.err.println("Cannot write to binary output file. ");
                System.exit(1);
            }
        }

    }


    public void preSeries(final GEOPlatformIndexed platform) {

        this.signals = new ProbeSignal[platform.getNumProbeIds()];
        for (int i = 0; i < signals.length; i++) {
            this.signals[i] = new ProbeSignal();
        }
        final String filename = "QuantileNormalizerPass1Accumulator.io";
        try {

            accumulator = (Int2FloatMap) BinIO.loadObject(filename);
        } catch (IOException e) {
            System.out.println("Cannot load mean signal average for quantiles.. problem reading filename: " + filename);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(10);
        }

        try {
            writer = new ArrayWriter(platform.getName().toString(), platform);

        } catch (FileNotFoundException e) {
            System.err.println("Cannot write to binary output file. ");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Cannot write to binary output file. ");
            System.exit(1);
        }
    }

    public void postSeries(final GEOPlatformIndexed platform, final ObjectList<MutableString> sampleIdSelection) {
        try {
            writer.close();

        } catch (IOException e) {
            System.out.println("Error writing list of samples used for normalization.");
            e.printStackTrace();
            System.exit(10);
        }
    }

    public void setOptions(final GeoScanOptions options) {

    }
}
