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

package edu.cornell.med.icb.svd;

public final class TestSVDWithColt extends AbstractTestSVD {
    /* TODO
public void testRvsColt() {
   double[][] matrix = new double[2][3];         // two columns, three rows
   matrix[0] = new double[]{1d, 2d, 3d};
   matrix[1] = new double[]{4d, 5d, 6d};

   // matrix:*/
    /*
   1  4
   2  5
   3  6
    */
    /*
        assertEquals(1, matrix[0][0], EPSILON);
        assertEquals(4, matrix[1][0], EPSILON);
        assertEquals(2, matrix[0][1], EPSILON);
        SingularValueDecomposition svdR = SVDFactory.getImplementation("R");
        SingularValueDecomposition svdColt = SVDFactory.getImplementation("COLT");
        svdR.svd(matrix);
        svdColt.svd(matrix);
        compare(svdR.getSingularValues(), svdColt.getSingularValues());
        compare(svdR.getU(), svdColt.getU(), svdColt.rank());

        compare(svdR.getV(), svdColt.getV(), matrix.length);
    }
      */

    /**
     * Gets the {@link SingularValueDecomposition} type used for the tests.
     *
     * @return The {@link edu.cornell.med.icb.svd.SVDFactory.ImplementationType} used for the tests
     */
    @Override
    public SVDFactory.ImplementationType getSVDImplementationType() {
        return SVDFactory.ImplementationType.COLT;
    }
}
