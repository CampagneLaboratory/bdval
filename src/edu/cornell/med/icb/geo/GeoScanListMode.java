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

import it.unimi.dsi.lang.MutableString;

import java.io.IOException;

/**
 * @author Fabien Campagne Date: Aug 16, 2007 Time: 2:25:57 PM
 */
public class GeoScanListMode extends GeoScanMode {
    @Override
    public void process(final GeoScanOptions options) {
        try {
            final GeoSoftFamilyParser parser =
                    new GeoSoftFamilyParser(options.softFilename);
            if (parser.skipToDatabaseSection()) {
                final MutableString databaseName = parser.getSectionAttribute();
                System.out.println("Database: " + databaseName);
            } else {
                System.out.println("No database section found.");
            }
            GEOPlatformIndexed platform = null;
            if (parser.skipToPlatformSection()) {
                final MutableString platformName = parser.getSectionAttribute();
                System.out.println("Platform: " + platformName);
                platform = parser.parsePlatform();
                platform.setName(platformName);
                if (platform != null) {
                    System.out.println("Platform has "
                            + platform.getNumProbeIds() + " probes.");
                } else {
                    System.out.println("Could not parse platform information.");
                }
            } else {
                System.out.println("No database section found.");
            }
            while (parser.skipToSampleSection()) {
                final MutableString sampleName = parser.getSectionAttribute();
                System.out.print("Sample " + sampleName + " ");
                final SampleDataCallback callback =
                        options.formatAdapter.getCallback(platform);
                parser.parseSampleData(platform, callback);
                options.formatAdapter
                        .analyzeSampleData(platform, callback, sampleName);
            }
        } catch (IOException e) {
            System.err.println("An error occurred reading soft file " + options
                    .softFilename);
            e.printStackTrace();
            System.exit(10);

        }
    }
}
