/*
 * Copyright (C) 2006-2010 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.geo.tools;

import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.ColumnTypeException;
import edu.mssm.crover.tables.InvalidColumnException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.util.Set;

/**
 * @author Fabien Campagne
 *         Date: Mar 1, 2006
 *         Time: 12:18:19 PM
 */
public class TestGeneLists extends TestCase {

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    ArrayTable source;
    int columnIDRefIndex;

    @Override
    protected void setUp() throws InvalidColumnException, ColumnTypeException {
        this.source = new ArrayTable();
        source.addColumn("ID_REF", String.class);
        source.addColumn("values", float.class);
        final int columnA = source.getColumnIndex("ID_REF");
        columnIDRefIndex = columnA;
        final int columnB = source.getColumnIndex("values");
        source.parseAppend(columnA, "ID_1");
        source.parseAppend(columnA, "ID_2");
        source.parseAppend(columnA, "ID_3");
        source.parseAppend(columnB, "12");
        source.parseAppend(columnB, "13");
        source.parseAppend(columnB, "14");

    }

    @Override
    protected void tearDown() {

    }

    public static Test suite() {
        return new TestSuite(TestGeneLists.class);
    }

    public void testFullGeneList() throws IOException {
        final String [] tokens = {"full"};
        final GeneList geneList = GeneList.createList(tokens);
        assertNotNull(geneList);
        assertTrue(geneList instanceof FullGeneList);
        final Set<String> probesetList = geneList.calculateProbeSetSelection(source, columnIDRefIndex);
        assertNotNull(probesetList);
        assertEquals(3, probesetList.size());
        assertTrue(probesetList.contains("ID_1"));
        assertTrue(probesetList.contains("ID_2"));
        assertTrue(probesetList.contains("ID_3"));
    }

    public void testRandomGeneList() throws IOException {
        final String [] tokens = {"random", "1", "203"};  // 1 probeset, seed= 203

        RandomGeneList geneList = (RandomGeneList) GeneList.createList(tokens);
        assertNotNull(geneList);
        assertTrue(geneList instanceof RandomGeneList);
        Set<String> probesetList = geneList.calculateProbeSetSelection(source, columnIDRefIndex);
        assertNotNull(probesetList);
        assertEquals(1, probesetList.size());

        assertTrue(probesetList.contains("ID_2"));

        final String[] tokens2 = {"random", "1", "6"};  // 1 probeset, seed= 3

        geneList = (RandomGeneList) GeneList.createList(tokens2);
        assertNotNull(geneList);
        assertTrue(geneList instanceof RandomGeneList);
        probesetList = geneList.calculateProbeSetSelection(source, columnIDRefIndex);
        assertNotNull(probesetList);
        assertEquals(1, probesetList.size());

        assertTrue(probesetList.contains("ID_3"));

        final String[] tokens3 = {"random", "1", "35"};  // 1 probeset, seed= 34

        geneList = (RandomGeneList) GeneList.createList(tokens3);
        assertNotNull(geneList);
        assertTrue(geneList instanceof RandomGeneList);
        geneList.setSeed(35);
        probesetList = geneList.calculateProbeSetSelection(source, columnIDRefIndex);
        assertNotNull(probesetList);
        assertEquals(1, probesetList.size());
      //  System.out.println("probesetList: " + probesetList);
        assertTrue(probesetList.contains("ID_1"));

        // now same conditions as before, but we exclude all valid IDs with an exclusion gene list:
        final String[]excludedProbesets = {"ID_3", "ID_1"};
        final GeneList exclusionGeneList = new FixedGeneList(excludedProbesets);
        geneList = (RandomGeneList) GeneList.createList(tokens3);
        geneList.setExclusionGeneList(exclusionGeneList);
        geneList.setSeed(34);
        assertNotNull(geneList);
        assertTrue(geneList instanceof RandomGeneList);
        probesetList = geneList.calculateProbeSetSelection(source, columnIDRefIndex);
        assertNotNull(probesetList);
        //
        assertEquals(1, probesetList.size());
        System.out.println("probesetList: "+probesetList);

        assertTrue(probesetList.contains("ID_2"));// ID_2 is the only valid choice.


    }
}
