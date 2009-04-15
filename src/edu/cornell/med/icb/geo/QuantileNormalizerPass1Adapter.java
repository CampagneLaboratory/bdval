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

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/*
 * A signal adapter that parses Affymetrix GP96 platform soft files and calculates the mean
 * of signal values across samples at each quantile of signal.
 *
 * @author Fabien Campagne
 * Date: Aug 20, 2007
 * Time: 1:35:09 PM
 *
 */
public class QuantileNormalizerPass1Adapter implements FormatAdapter {
    private final Comparator<? super ProbeSignal> comparator = new Comparator<ProbeSignal>() {

        public int compare(final ProbeSignal o1, final ProbeSignal o2) {
            return -Float.compare(o1.signal, o2.signal);
        }
    };
    private int numSampleScanned;

    class ProbeSignal {
        int probeIndex;
        float signal;
        int rank;
    }

    public SampleDataCallback getCallback(final GEOPlatformIndexed platform) {
        if (this.signals == null) {
            this.signals = new ProbeSignal[platform.getNumProbeIds()];
            for (int i = 0; i < signals.length; i++) {
                this.signals[i] = new ProbeSignal();
            }
            accumulator = new Int2FloatOpenHashMap();
            accumulator.defaultReturnValue(0);
            numSampleScanned = 0;
        }

        return new ParseAffymetrixSampleDataCallback(platform);
    }

    ProbeSignal[] signals;
    Int2FloatMap accumulator = new Int2FloatOpenHashMap();

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
                float accumulation = accumulator.get(rank);
                accumulation += ps.signal;
                accumulator.put(rank, accumulation);
                ++rank;
            }
            numSampleScanned++;

        } else {
        }

    }

    public void preSeries(final GEOPlatformIndexed platform) {

    }

    public void postSeries(final GEOPlatformIndexed platform, final ObjectList<MutableString> sampleIdSelection) {

        try {
            for (final int rank : accumulator.keySet()) {
                float value = accumulator.get(rank);
                value /= numSampleScanned;
                accumulator.put(rank, value);
                //      System.out.println("rank: " + rank + " mean signal: " + value);
            }
            BinIO.storeObject(accumulator, "QuantileNormalizerPass1Accumulator.io");

        } catch (IOException e) {
            System.err.println("Cannot write accumulator file ");
            e.printStackTrace();
            System.exit(10);
        }
    }

    GeoScanOptions options;

    public void setOptions(final GeoScanOptions options) {
        this.options = options;
    }
}
