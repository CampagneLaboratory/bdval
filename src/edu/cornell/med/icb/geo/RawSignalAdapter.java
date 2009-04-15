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
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;

/*
 * A signal adapter that parses Affymetrix GP96 platform soft files and  writes the raw signal
 *
 * @author Fabien Campagne
 * Date: Dec 6, 2007
 * Time: 6:03:09 PM
 *
 */
public class RawSignalAdapter implements FormatAdapter {
    private Comparator<? super ProbeSignal> comparator = new Comparator<ProbeSignal>() {
        public int compare(final ProbeSignal o1, final ProbeSignal o2) {
            return Double.compare(o1.signal, o2.signal);
        }
    };
    private ArrayWriter writer;
    private String outputBasename;

    public SampleDataCallback getCallback(final GEOPlatformIndexed platform) {

        return new ParseAffymetrixSampleDataCallback(platform);
    }

    ProbeSignal[] signals;

    public void analyzeSampleData(final GEOPlatformIndexed platform, final SampleDataCallback callback,
                                  final MutableString sampleIdentifier) {
        if (callback.canParse()) {
            final AffymetrixSampleData data = (AffymetrixSampleData) callback.getParsedData();

            try {
                for (int probesetIndex = 0; probesetIndex < data.signal.length; ++probesetIndex) {
                    if (!data.presentAbsentCalls.get(probesetIndex)) {
                        // make signal=0 for absent probesets.
                        /* System.out.println("Ignoring value: " + data.signal[probesetIndex] + " for probeset "
                                                              + platform.getProbesetIdentifier(probesetIndex) +
                                                              " in sample " + sampleIdentifier);*/
                        data.signal[probesetIndex] = 0;

                    }
                }
                writer.appendSample(data.signal, sampleIdentifier);

            } catch (IOException e) {
                System.err.println("Cannot write to binary output file. ");
                System.exit(1);
            }
        }

    }


    public void preSeries(final GEOPlatformIndexed platform) {
        try {
            String basename = platform.getName().toString();
            if (outputBasename != null) {
                basename = outputBasename;
            }
            writer = new ArrayWriter(basename, platform);

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
        assert options.tpr != null : "This implementation requires transcript to probeset relationships.";

        if (options.adapterOptions != null) {
            final String[] opts = options.adapterOptions.split("[\\s]+");
            this.outputBasename = CLI.getOption(opts, "--output", null);
            System.out.println("Will write to output : " + this.outputBasename);
        } else {
            System.out.println("adapter options were null.");
        }
    }
}
