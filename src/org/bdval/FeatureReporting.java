/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.bdval;

import edu.cornell.med.icb.tissueinfo.similarity.TranscriptScore;
import it.unimi.dsi.lang.MutableString;

/**
 * Provides convenience functions to report identity of features.
 *
 * @author Fabien Campagne
 *         Date: Feb 23, 2008
 *         Time: 11:54:35 AM
 */
public class FeatureReporting {
    private final boolean writeGeneListFormat;

    public FeatureReporting(final boolean writeGeneListFormat) {
        super();
        this.writeGeneListFormat = writeGeneListFormat;
    }

    public void reportFeature(final int iteration, final double[] weights,
                              final TranscriptScore probeset, final DAVOptions options) {
        final double weight = weights[probeset.transcriptIndex];
        reportFeature(iteration, weight, probeset, options);
    }

    public void reportFeature(final int iteration, final double weight,
                              final TranscriptScore probeset, final DAVOptions options) {
        final MutableString probesetId = options.getProbesetIdentifier(probeset.transcriptIndex);
        if (writeGeneListFormat) {
            //"#Ensembl Gene ID\tEMBL ID\tRefSeq DNA ID\tProbe ID";
            options.output.print('\t');
            options.output.print('\t');
            options.output.print('\t');
            options.output.print(probesetId);
            options.output.print('\n');
        } else {
            options.output.print(iteration);
            options.output.print('\t');
            options.output.print(probeset.transcriptIndex);
            options.output.print('\t');
            options.output.print(weight);
            options.output.print('\t');

            options.output.print(probesetId);
            options.output.print('\n');
        }
        options.output.flush();
    }
}
