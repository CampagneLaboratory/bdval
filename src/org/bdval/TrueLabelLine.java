/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

package org.bdval;

/**
 * Hold a line of information from the true labels file.
 *
 * @author Kevin Dorff
 */
public class TrueLabelLine {
    String sampleID1;
    String sampleID2;
    String classLabel;
    String datasetCode;
    String endpointCode;

    TrueLabelLine(
            final String sampleID1, final String sampleID2, final String classLabel,
            final String datasetCode, final String endpointCode) {
        super();
        this.sampleID1 = sampleID1;
        if (datasetCode.equals("NB")) {
            this.sampleID2 = fixNbSample2(sampleID2);
        } else {
            this.sampleID2 = sampleID2;
        }
        this.classLabel = classLabel;
        this.datasetCode = datasetCode;
        this.endpointCode = endpointCode;
    }

    public String trueLabelMapKey() {
        // sampleID2 needs to be modified if we are in NB
        return String.format("%s-%s-%s", sampleID1, sampleID2, endpointCode);
    }

    public static String fixNbSample2(final String sample2) {
        final String newSample;
        if (sample2.startsWith("Processed-")) {
            newSample = sample2.substring(10, 37);
        } else {
            newSample = sample2.substring(0, 27);
        }
        return newSample;
    }

    public static String fixAnySample(final String sample) {
        if (sample.startsWith("Processed-")) {
            return sample.substring(10, 33); // This was 37 in the above... weird
        } else {
            return sample;
        }
    }
}
