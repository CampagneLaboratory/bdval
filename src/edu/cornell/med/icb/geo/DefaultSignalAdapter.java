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

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;

/**
 * A signal adapter that parses Affymetrix GP96 platform soft files and prints what percent of
 * probeIds are present in each sample.
 *
 * @author Fabien Campagne
 * Date: Aug 16, 2007
 * Time: 3:05:19 PM
 */
public class DefaultSignalAdapter implements FormatAdapter {
    public SampleDataCallback getCallback(final GEOPlatformIndexed platform) {
        return new ParseAffymetrixSampleDataCallback(platform);
    }

    boolean quiet;

    public DefaultSignalAdapter(final boolean quiet) {
        super();
        this.quiet = quiet;
    }

    public DefaultSignalAdapter() {
        super();
    }

    public void analyzeSampleData(final GEOPlatformIndexed platform, final SampleDataCallback callback, final MutableString sampleIdentifier) {
        if (callback.canParse()) {
            final AffymetrixSampleData data = (AffymetrixSampleData) callback.getParsedData();
            double percentPresent = data.presentAbsentCalls.cardinality();
            percentPresent /= (double) data.presentAbsentCalls.size();
            percentPresent *= 100;
            if (!quiet) {
                System.out.println("Number of signal lines: " + data.signal.length + " %present: "
                        + ((int) percentPresent));
            }
        } else {
            System.out.println("Cannot parse the data for this sample. Columns were non standard: " + callback.getColumnNames());
        }

    }

    public void preSeries(final GEOPlatformIndexed platform) {

    }

    public void postSeries(final GEOPlatformIndexed platform, final ObjectList<MutableString> sampleIdSelection) {

    }

    public void setOptions(final GeoScanOptions options) {

    }
}
