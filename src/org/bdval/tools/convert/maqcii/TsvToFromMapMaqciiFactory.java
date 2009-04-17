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

package org.bdval.tools.convert.maqcii;

import edu.cornell.med.icb.io.TsvToFromMap;


/**
 * Describe class here.
 *
 * @author Kevin Dorff
 */
public class TsvToFromMapMaqciiFactory {
    /** Private constructor for this factory class. */
    private TsvToFromMapMaqciiFactory() {
        super();
    }

    /** The columns for a TSV file in PREDICTION format. */
    public static final String[] PREDICTION_FORMAT_COLUMSN = new String[] {
            "#splitId", "splitType", "repeatId", "modelFilenamePrefix", "sampleIndex", "sampleId",
            "probabilityOfClass1", "predictedSymbolicLabel", "probabilityOfPredictedClass",
            "probabilityClass1", "trueLabel", "numericTrueLabel", "correct", "modelNumFeatures"
    };

    /** The columns for a TSV file in CORNELL format. */
    public static final String[] CORNELL_FORMAT_COLUMNS = new String[] {
            "SampleID1", "SampleID2", "OrganizationCode", "DatasetCode", "EndpointCode",
            "MAQCII ModelID", "OrganizationSpecificModelID", "DecisionValue",
            "SymbolicClassPrediction", "Threshold"
    };

    /** The columns for a LEMING (non-Cologne) format. */
    public static final String[] LEMING_FORMAT_STANDARD_COLUMNS = new String[] {
            "Column Heading (in the Normalized Array Data File)",
            "Raw Array Data File",
            "OrganizationCode", "DatasetCode", "EndpointCode"
    };

    /** The columns for a LEMING-COLOGNE format. */
    public static final String[] LEMING_FORMAT_COLOGNE_COLUMNS = new String[] {
            "PatientID", "Column Heading in the Normalized Array Data File",
            "Array_A01_FileName (S3R5)", "Array_A02_FileName (R3S5)",
            "OrganizationCode", "DatasetCode", "EndpointCode"
    };

    /** The columns for a Cologne-Sample-Multimap format. */
    public static final String[] COLOGNE_SAMPLE_MULTIMAP_COLUMNS = new String[] {
            "PatientID", "Column Heading in the Normalized Array Data File",
            "Array_A01_FileName (S3R5)", "Array_A02_FileName (R3S5)"
    };

    /** The columns for a Cologne-Sample-Multimap format. */
    public static final String[] TRUE_LABELS_COLUMS = new String[] {
            "SampleId1", "SampleId2", "ClassLabel", "DatasetCode", "EndpointCode"
    };

    /** An enum of the known file formats. */
    public enum TsvToFromMapType {
        /** Cornell format. */
        CORNELL_FORMAT(CORNELL_FORMAT_COLUMNS),
        /** Prediction file format. */
        PREDICTION_FORMAT(PREDICTION_FORMAT_COLUMSN),
        /** Leming-Cologne file format. */
        LEMING_FORMAT_COLOGNE(LEMING_FORMAT_COLOGNE_COLUMNS),
        /** Leming (non-Cologne) file format. */
        LEMING_FORMAT_STANDARD(LEMING_FORMAT_STANDARD_COLUMNS),
        /** Cologne sample-multimap file format. */
        COLOGNE_SAMPLE_MULTIMAP(COLOGNE_SAMPLE_MULTIMAP_COLUMNS),
        /** True labels format. */
        TRUE_LABELS_FORMAT(TRUE_LABELS_COLUMS);

        /** The (initial) column headers for the specific TsvToFromMap object. */
        private final String[] columnHeaders;

        /**
         * Create a TsvToFromMapType enum object with the given column headers.
         * @param columnHeaderNames the (initial) column headers
         */
        private TsvToFromMapType(final String[] columnHeaderNames) {
            this.columnHeaders = columnHeaderNames;
        }

        /**
         * Getter for the (initial) column headers.
         * @return the column headers
         */
        public String[] getColumnHeaders() {
            return columnHeaders;
        }
    }

    /**
     * Construct a TsvToFromMap for the given known file type.
     * @param type the type of file to make the object TsvToFromMap for
     * @return the TsvToFromMap for the given type
     */
    public static TsvToFromMap getMapForType(final TsvToFromMapType type) {
        return new TsvToFromMap(type.getColumnHeaders());
    }
}
