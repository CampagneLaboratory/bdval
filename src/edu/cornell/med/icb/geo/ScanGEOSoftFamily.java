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

import com.martiansoftware.jsap.JSAPException;
import edu.cornell.med.icb.cli.UseModality;

/**
 * Scans a GEO soft family file. The main method helps inspect the content of a
 * file. The class can be subclassed to define different type of
 * analysis/treatment with the data in the file.
 *
 * @author Fabien Campagne Date: Aug 16, 2007 Time: 11:39:23 AM
 */
public class ScanGEOSoftFamily {
    public static void main(final String[] args) throws JSAPException, IllegalAccessException, InstantiationException {
        final ScanGEOSoftFamily similarityTool = new ScanGEOSoftFamily();
        similarityTool.process(args);
    }

    private void process(final String[] args) throws JSAPException, IllegalAccessException, InstantiationException {
        final UseModality<GeoScanOptions> useModality = new GeoScanMode();

        useModality.registerMode("list", GeoScanListMode.class);
        useModality.registerMode("scan", GeoScanSelectedSamplesMode.class);

        final GeoScanOptions options = new GeoScanOptions();
        useModality.process(args, options);
        if (options.output != null) {
            options.output.flush();
        }
        System.exit(0);
    }

}
