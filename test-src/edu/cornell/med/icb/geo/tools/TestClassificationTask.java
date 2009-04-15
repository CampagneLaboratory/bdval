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

package edu.cornell.med.icb.geo.tools;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.Set;

/**
 * @author Fabien Campagne
 *         Date: May 7, 2008
 *         Time: 10:40:36 AM
 */
public class TestClassificationTask extends TestCase {
    public void testParseOneClass() {
        String task = "one-class\tfilename\tPositive\t2";
        String cids = "#class\tsampleId\n" +
                "Positive\tsample1\n" +
                "Positive\tsample2\n" +
                "Negative\tsample3\n";
        ClassificationTask[] tasks = ClassificationTask.parseTaskAndConditions(new StringReader(task), new StringReader(cids));
        assertNotNull(tasks);
        assertEquals("one-class must have a single condition", 1, tasks[0].getNumberOfConditions());
        assertEquals("Class 0 must have two samples", tasks[0].getNumberSamples(0), 2);
        assertEquals("Class 0 must be called Positive", "Positive", tasks[0].getConditionName(0));

    }

    public void testParseMultiClass() {
        String task = "multi-class\tfilename\tPositive\t2\tNegative\t1\tOther\t1";
        String cids = "#class\tsampleId\n" +
                "Positive\tsample1\n" +
                "Positive\tsample2\n" +
                "Negative\tsample3\n" +
                "Other\tsample4\n";
        ClassificationTask[] tasks = ClassificationTask.parseTaskAndConditions(new StringReader(task), new StringReader(cids));
        assertNotNull(tasks);
        assertEquals("task must have three conditions", 3, tasks[0].getNumberOfConditions());
        assertEquals("Class 0 must have two samples", 2, tasks[0].getNumberSamples(0));
        assertEquals("Class 0 must be called Positive", "Positive", tasks[0].getConditionName(0));
        assertEquals("Class 1 must have 1 sample", 1, tasks[0].getNumberSamples(1));
        assertEquals("Class 1 must be called Negative", "Negative", tasks[0].getConditionName(1));
        assertEquals("Class 2 must have 1 sample", 1, tasks[0].getNumberSamples(2));
        assertEquals("Class 1 must be called Negative", "Other", tasks[0].getConditionName(2));

    }

    public void testParseRegression() {
        String task = "regression";
        String cids = "#class\tsampleId\n" +
                "Positive\tsample1\n" +
                "Positive\tsample2\n" +
                "Negative\tsample3\n" +
                "Other\tsample4\n";
        try {
        ClassificationTask[] tasks = ClassificationTask.parseTaskAndConditions(new StringReader(task), new StringReader(cids));
        } catch (UnsupportedOperationException e) {
            return;
        }
        fail("regression keyword must throw unsupported exception.");
    }


    public void testParseLegacyFormat() {
        String taskString = "filename\tPositive\tNegative\t2\t1";
        String cids = "#class\tsampleId\n" +
                "Positive\tsample1\n" +
                "Positive\tsample2\n" +
                "Negative\tsample3\n" +
                "Other\tsample4\n";

        ClassificationTask[] tasks = ClassificationTask.parseTaskAndConditions(new StringReader(taskString), new StringReader(cids));
        assertNotNull(tasks);
        assertEquals("task must have two conditions", 2, tasks[0].getNumberOfConditions());
        assertEquals("Class 0 must have two samples", 2, tasks[0].getNumberSamples(0));
        assertEquals("Class 0 must be called Positive", "Positive", tasks[0].getConditionName(0));
        assertEquals("Class 1 must have 1 sample", 1, tasks[0].getNumberSamples(1));
        assertEquals("Class 1 must be called Negative", "Negative", tasks[0].getConditionName(1));
        ClassificationTask task = tasks[0];
        final Set<String> samplesForClass0 = task.getConditionsIdentifiers().getLabelGroup(task.getFirstConditionName());
        final Set<String> samplesForClass1 = task.getConditionsIdentifiers().getLabelGroup(task.getSecondConditionName());
        assertTrue(samplesForClass0.contains("sample1"));
        assertTrue(samplesForClass0.contains("sample2"));
        assertEquals(2, samplesForClass0.size());
        assertEquals(1, samplesForClass1.size());
        assertTrue(samplesForClass1.contains("sample3"));

    }

}
