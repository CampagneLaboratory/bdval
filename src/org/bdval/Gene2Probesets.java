/*
 * Copyright (C) 2008-2010 Institute for Computational Biomedicine,
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

import edu.cornell.med.icb.identifier.DoubleIndexedIdentifier;
import edu.cornell.med.icb.identifier.IndexedIdentifier;
import edu.cornell.med.icb.tissueinfo.similarity.GeneTranscriptRelationships;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.lang.MutableString;

import java.io.FileNotFoundException;

/**
 * @author Fabien Campagne
 *         Date: Mar 2, 2008
 *         Time: 3:06:10 PM
 */
public class Gene2Probesets {
    final GeneTranscriptRelationships delegate;

    public Gene2Probesets() {
        super();
        this.delegate = new GeneTranscriptRelationships();
    }

    public DoubleIndexedIdentifier load(final String geneTranscriptRelFilename) throws FileNotFoundException {
        final IndexedIdentifier probesetIndices = delegate.load(geneTranscriptRelFilename);
        return new DoubleIndexedIdentifier(probesetIndices);
    }

    public IntSet getProbesetIndices(final MutableString geneId) {
        return delegate.getTranscriptSet(geneId);
    }
}
