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
 * @author Fabien Campagne Date: Aug 16, 2007 Time: 3:22:20 PM
 */
public class SAGECountFormatAdapter implements FormatAdapter {
    public SampleDataCallback getCallback(final GEOPlatformIndexed platform) {
        return new ParseSAGECountsSampleDataCallback(platform);
    }

    public void analyzeSampleData(final GEOPlatformIndexed platform,
                                  final SampleDataCallback callback,
                                  final MutableString sampleIdentifier) {
        final SAGECountsSampleData data = (SAGECountsSampleData) callback.getParsedData();
        System.out.println(" number of signal lines: " + data.count.length);
    }

    public void preSeries(final GEOPlatformIndexed platform) {
    }

    public void postSeries(final GEOPlatformIndexed platform,
                           final ObjectList<MutableString> sampleIdSelection) {
    }

    public void setOptions(final GeoScanOptions options) {

    }
}
