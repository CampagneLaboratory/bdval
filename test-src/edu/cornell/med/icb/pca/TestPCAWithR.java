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

package edu.cornell.med.icb.pca;

import edu.mssm.crover.tables.ColumnTypeException;
import edu.mssm.crover.tables.DefineColumnFromRow;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import edu.mssm.crover.tables.readers.ColumbiaTmmReader;
import edu.mssm.crover.tables.readers.SyntaxErrorException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Fabien Campagne
 * Date: Apr 15, 2008
 * Time: 3:02:15 PM
 */
public class TestPCAWithR {
    double[][] matrix;
    private List<CharSequence> colIds;
    private List<CharSequence> rowIds;
    private final DefineColumnFromRow colHelper = new DefineColumnFromRow(0);

    @Before
    public void setUp() throws SyntaxErrorException, IOException,
            InvalidColumnException, ColumnTypeException, TypeMismatchException {
        final String dataSmall = "ID_REF\tMurder\tAssault\tUrbanPop\tRape\n" +
                "Alabama\t13.2\t236\t58\t21.2\n" +
                "Alaska\t10.0\t263\t48\t44.5\n" +
                "Arizona\t8.1\t294\t80\t31.0\n" +
                "Arkansas\t8.8\t190\t50\t19.5\n" +
                "California\t9.0\t276\t91\t40.6\n";

        final String data = "ID_REF\tMurder\tAssault\tUrbanPop\tRape\n" +
                "Alabama\t13.2\t236\t58\t21.2\n" +
                "Alaska\t10.0\t263\t48\t44.5\n" +
                "Arizona\t8.1\t294\t80\t31.0\n" +
                "Arkansas\t8.8\t190\t50\t19.5\n" +
                "California\t9.0\t276\t91\t40.6\n" +
                "Colorado\t7.9\t204\t78\t38.7\n" +
                "Connecticut\t3.3\t110\t77\t11.1\n" +
                "Delaware\t5.9\t238\t72\t15.8\n" +
                "Florida\t15.4\t335\t80\t31.9\n" +
                "Georgia\t17.4\t211\t60\t25.8\n" +
                "Hawaii\t5.3\t46\t83\t20.2\n" +
                "Idaho\t2.6\t120\t54\t14.2\n" +
                "Illinois\t10.4\t249\t83\t24.0\n" +
                "Indiana\t7.2\t113\t65\t21.0\n" +
                "Iowa\t2.2\t56\t57\t11.3\n" +
                "Kansas\t6.0\t115\t66\t18.0\n" +
                "Kentucky\t9.7\t109\t52\t16.3\n" +
                "Louisiana\t15.4\t249\t66\t22.2\n" +
                "Maine\t2.1\t83\t51\t7.8\n" +
                "Maryland\t11.3\t300\t67\t27.8\n" +
                "Massachusetts\t4.4\t149\t85\t16.3\n" +
                "Michigan\t12.1\t255\t74\t35.1\n" +
                "Minnesota\t2.7\t72\t66\t14.9\n" +
                "Mississippi\t16.1\t259\t44\t17.1\n" +
                "Missouri\t9.0\t178\t70\t28.2\n" +
                "Montana\t6.0\t109\t53\t16.4\n" +
                "Nebraska\t4.3\t102\t62\t16.5\n" +
                "Nevada\t12.2\t252\t81\t46.0\n" +
                "New Hampshire\t2.1\t57\t56\t9.5\n" +
                "New Jersey\t7.4\t159\t89\t18.8\n" +
                "New Mexico\t11.4\t285\t70\t32.1\n" +
                "New York\t11.1\t254\t86\t26.1\n" +
                "North Carolina\t13.0\t337\t45\t16.1\n" +
                "North Dakota\t0.8\t45\t44\t7.3\n" +
                "Ohio\t7.3\t120\t75\t21.4\n" +
                "Oklahoma\t6.6\t151\t68\t20.0\n" +
                "Oregon\t4.9\t159\t67\t29.3\n" +
                "Pennsylvania\t6.3\t106\t72\t14.9\n" +
                "Rhode Island\t3.4\t174\t87\t8.3\n"
                + "South Carolina\t14.4\t279\t48\t22.5\n" +
                "South Dakota\t3.8\t86\t45\t12.8\n" +
                "Tennessee\t13.2\t188\t59\t26.9\n" +
                "Texas\t12.7\t201\t80\t25.5\n" +
                "Utah\t3.2\t120\t80\t22.9\n" +
                "Vermont\t2.2\t48\t32\t11.2\n" +
                "Virginia\t8.5\t156\t63\t20.7\n"
                + "Washington\t4.0\t145\t73\t26.2\n" +
                "West Virginia\t5.7\t81\t39\t9.3\n"
                + "Wisconsin\t2.6\t53\t66\t10.8\n" +
                "Wyoming\t6.8\t161\t60\t15.6";

        final StringReader reader = new StringReader(data);
        final ColumbiaTmmReader parser = new ColumbiaTmmReader();
        Table t = parser.read(reader);
        t = t.transpose(colHelper);
        matrix = new double[t.getColumnNumber() - 1][t.getRowNumber()];
        colIds = new ArrayList<CharSequence>();
        for (int c = 1; c < t.getColumnNumber(); c++) {
            matrix[c - 1] = t.getDoubles(t.getIdentifier(c));
            colIds.add(t.getIdentifier(c));
        }

        rowIds = new ArrayList<CharSequence>();
        final String[] idRefs = t.getStrings("ID_REF");
        rowIds.addAll(Arrays.asList(idRefs));
    }

    @Test
    public void testPCA() {
        final PrincipalComponentAnalysisWithR calc = new PrincipalComponentAnalysisWithR();
        calc.setCollectRotationRowNames(true);
        calc.setDoScaling(true);
        calc.pca(matrix, colIds, rowIds);

        final List<CharSequence> rowNames = calc.getRotationRowNames();
        int index = 0;
        for (final CharSequence rowName : rowNames) {
            assertEquals("Rotation row names does not match exactly the column ids provided for "
                + "the input matrix at index " + index, rowName, colIds.get(index++));
        }

        assertNotNull(calc.getRotation());
        final double[][] rotation = calc.getRotation();
        /*  for (int col = 0; col < rotation.length; col++) {
            System.out.println("col "+col);
            TextIO.storeDoubles(rotation[col], System.out);
        }*/
        assertEquals(0.14239973d, rotation[ /* column 0 */ 0][/* row */ 0], 1E-4);
        assertEquals(0.13841416d, rotation[/* column 0 */ 0][/* row */ 1], 1E-4);
        assertEquals(-0.128388172d, rotation[ /* column 0 */ 1][/* row */ 0], 1E-4);
        assertEquals(0.311588857d, rotation[/* column 0 */ 2][/* row */ 1], 1E-4);

        final double[][] rotated = calc.rotate(matrix);
        assertEquals(55.40746d, rotated[ /* column  */ 0][/* row */ 0], 1E-4);
        assertEquals(1218.84743d, rotated[/* column  */ 0][/* row */ 1], 1E-4);
        assertEquals(-20.42092d, rotated[ /* column */ 1][/* row */ 0], 1E-4);
        assertEquals(-1.281673d, rotated[/* column  */ 2][/* row */ 1], 1E-4);
        assertEquals(41.653537d, rotated[/* column  */ 2][/* row */ 3], 1E-4);
    }
}
