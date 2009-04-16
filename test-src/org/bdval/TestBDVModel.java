/*
 * Copyright (C) 2009 Institute for Computational Biomedicine,
 *                    Weill Medical College of Cornell University
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

import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import java.io.IOException;

/**
 * Validates the functionality of the {@link org.bdval.BDVModel} class.
 */
public class TestBDVModel {
    /**
     * Validates that models in the "old" binary format will load properly.
     * @throws ClassNotFoundException if the model cannot be loaded properly
     * @throws IOException if the model cannot be loaded properly
     */
    // @Test
    // TODO: The old binary format is no longer supported
    public void loadBinaryModel() throws ClassNotFoundException, IOException {
        final BDVModel model = new BDVModel("test-data/models/binary/libSVM_Cologne_EFS_MO-baseline-global-svm-weights-final-model-LDPSS");
        model.load(new DAVOptions());
        assertEquals("The dataset name does not match", "Cologne_EFS_MO_data.cologne", model.getDatasetName());

        final String[] symbolicClassLabels = model.getSymbolicClassLabel();
        assertNotNull("Class labels must not be null", symbolicClassLabels);
        assertEquals("There should be 2 class labels", 2, symbolicClassLabels.length);
        assertEquals("First class label should be 0", "0", symbolicClassLabels[0]);
        assertEquals("Second class label should be 1", "1", symbolicClassLabels[1]);

        assertEquals("Number of features is incorrect", 10, model.getNumberOfFeatures());

        assertNotNull("means map must not be null", model.probesetScaleMeanMap);
        assertEquals("means map should contain 10 elements", 10, model.probesetScaleMeanMap.size());

        assertNotNull("range map must not be null", model.probesetScaleRangeMap);
        assertEquals("range map should contain 10 elements", 10, model.probesetScaleRangeMap.size());

        assertFalse("Model should not be a consensus model", model.isConsensusModel());
    }

    /**
     * Validates that models in the "new" property based format will load properly.
     * @throws ClassNotFoundException if the model cannot be loaded properly
     * @throws IOException if the model cannot be loaded properly
     */
    @Test
    public void loadPropertyModel() throws ClassNotFoundException, IOException {
        final BDVModel model = new BDVModel("test-data/models/properties/libSVM_Cologne_EFS_MO-baseline-global-svm-weights-final-model-LDPSS");
        model.load(new DAVOptions());
        assertEquals("The dataset name does not match", "Cologne_EFS_MO_data.cologne", model.getDatasetName());

        final String[] symbolicClassLabels = model.getSymbolicClassLabel();
        assertNotNull("Class labels must not be null", symbolicClassLabels);
        assertEquals("There should be 2 class labels", 2, symbolicClassLabels.length);
        assertEquals("First class label should be 0", "0", symbolicClassLabels[0]);
        assertEquals("Second class label should be 1", "1", symbolicClassLabels[1]);

        assertEquals("Number of features is incorrect", 10, model.getNumberOfFeatures());

        assertNotNull("means map must not be null", model.probesetScaleMeanMap);
        assertEquals("means map should contain 10 elements", 10, model.probesetScaleMeanMap.size());

        assertNotNull("range map must not be null", model.probesetScaleRangeMap);
        assertEquals("range map should contain 10 elements", 10, model.probesetScaleRangeMap.size());

        assertFalse("Model should not be a consensus model", model.isConsensusModel());
    }

    /**
     * Validate the funtionality of
     * {@link BDVModel#checkReOrderTestSet(edu.mssm.crover.tables.Table)}.
     * @throws ClassNotFoundException if the model cannot be loaded properly
     * @throws IOException if the model cannot be loaded properly
     * @throws InvalidColumnException if the creation of the test set table fails
     */
    @Test
    public void checkReOrderTestSet()
            throws ClassNotFoundException, IOException, InvalidColumnException {
        final Table testSet = new ArrayTable();
        testSet.addColumn("ID_REF", String.class);
        testSet.addColumn("Hs343026.1", double.class);
        testSet.addColumn("A_32_P129689", double.class);
        testSet.addColumn("A_23_P8541", double.class);
        testSet.addColumn("A_23_P417918", double.class);
        testSet.addColumn("A_23_P251480", double.class);
        testSet.addColumn("A_23_P212482", double.class);
        testSet.addColumn("A_23_P20423", double.class);
        testSet.addColumn("A_23_P149153", double.class);
        testSet.addColumn("A_23_P11859", double.class);
        testSet.addColumn("A_23_P116145", double.class);

        // NOTE: removing any column (except for ID_REF) from the testSet table above will cause
        // the same exception listed in http://icbtools.med.cornell.edu/mantis/view.php?id=1301
        final BDVModel model = new BDVModel("test-data/models/properties/libSVM_Cologne_EFS_MO-baseline-global-svm-weights-final-model-LDPSS");
        model.load(new DAVOptions());
        model.checkReOrderTestSet(testSet);
    }
}
