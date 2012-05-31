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

import edu.cornell.med.icb.geo.binaryarray.ArrayWriter;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.lang.MutableString;

import java.io.IOException;

/**
 * @author Fabien Campagne
 * Date: Sep 27, 2007
 * Time: 3:53:13 PM
 */
public class AffySnpChipFormatAdapter implements FormatAdapter {
    public SampleDataCallback getCallback(final GEOPlatformIndexed platform) {
            return new AffySnpChipCountsSampleDataCallback(platform);
    }

    public void analyzeSampleData(final GEOPlatformIndexed platform,
                                  final SampleDataCallback callback,
                                  final MutableString sampleIdentifier) {
     if (callback.canParse()) {
            final GenotypeSampleData data = (GenotypeSampleData) callback.getParsedData();

            try {
                arrayWriter.appendSample(data.signal(), sampleIdentifier);

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

      public void postSeries(final GEOPlatformIndexed platform,
                             final ObjectList<MutableString> sampleIdSelection) {
          try {
              arrayWriter.close();
          } catch (IOException e) {
              System.out.println("Error writing list of samples used for normalization.");
              e.printStackTrace();
              System.exit(10);
          }
      }

    public void setOptions(final GeoScanOptions options) {
       //not used.
    }
}
