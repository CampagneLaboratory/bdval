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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.IOException;

/**
 * Validates the functionality of the {@link org.bdval.ConsensusBDVModel}
 * class.
 */
public class TestConsensusBDVModel {
    /**
     * Validates that models in the "old" binary format will load properly.
     * @throws ClassNotFoundException if the model cannot be loaded properly
     * @throws java.io.IOException if the model cannot be loaded properly
     */
    // @Test
    // TODO: The old binary format is no longer supported
    public void loadBinaryModel() throws ClassNotFoundException, IOException {
        final BDVModel model = new ConsensusBDVModel("test-data/models/binary/libSVM_Cologne_EFS_MO-baseline-global-svm-weights-final-model-LDPSS", "libSVM_Cologne_EFS_MO-baseline-global-svm-weights-final-model-LDPSS");
        model.load(new DAVOptions());

        assertEquals("Number of features is incorrect", 10, model.getNumberOfFeatures());
        assertTrue("Model should be a consensus model", model.isConsensusModel());
    }

    /**
     * Validates that models in the "new" property based format will load properly.
     * @throws ClassNotFoundException if the model cannot be loaded properly
     * @throws IOException if the model cannot be loaded properly
     */
    @Test
    public void loadPropertyModel() throws ClassNotFoundException, IOException {
        final BDVModel model = new ConsensusBDVModel("test-data/models/properties/libSVM_Cologne_EFS_MO-baseline-global-svm-weights-final-model-LDPSS", "libSVM_Cologne_EFS_MO-baseline-global-svm-weights-final-model-LDPSS");
        model.load(new DAVOptions());

        assertEquals("Number of features is incorrect", 10, model.getNumberOfFeatures());
        assertTrue("Model should be a consensus model", model.isConsensusModel());
    }
}
