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

import edu.cornell.med.icb.tissueinfo.similarity.GeneTranscriptRelationships;
import edu.cornell.med.icb.identifier.IndexedIdentifier;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.lang.MutableString;

/**
 * @author Fabien Campagne Date: Aug 20, 2007 Time: 5:53:54 PM
 */
public class TranscriptProbesetRelationship extends GeneTranscriptRelationships {
    public TranscriptProbesetRelationship(final IndexedIdentifier transcriptIndices) {
        super(transcriptIndices);
    }

    public TranscriptProbesetRelationship() {
        super();
    }

    /**
     * Indicates that a probeset measures the expression level of a transcript.
     *
     * @param transcriptId
     * @param probesetIndex
     */
    @Override
    public void addRelationship(final MutableString transcriptId, final int probesetIndex) {
        super.addRelationship(transcriptId, probesetIndex);
    }

    /**
     * Get the set of probeset indices for that  transcript. Returns
     * probeset indices that measure the given transcript.
     *
     * @param transcriptId Identifier for the transcript of interest
     * @return Set of probeset indices
     */
    @Override
    public IntSet getTranscriptSet(final MutableString transcriptId) {
        return getTranscriptSet(getGeneIndex(transcriptId));
    }

    /**
     * Returns the index of the transcript measured by this probeset.
     *
     * @param probesetIndex
     * @return index of the transcript measured by this probeset.
     */
    public int probeset2TranscriptIndex(final int probesetIndex) {
        return transcript2Gene(probesetIndex);
    }

    public int getTranscriptNumber() {
        return geneId2Index.size();
    }

    public IndexedIdentifier getTranscripts() {
        return geneId2Index;
    }

}
