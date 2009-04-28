/*
 * Copyright (C) 2006-2009 Institute for Computational Biomedicine,
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

import edu.cornell.med.icb.learning.tools.svmlight.EvaluationMeasure;
import edu.mssm.crover.tables.Table;

/**
 * @author campagne Date: Mar 2, 2006 Time: 1:22:09 PM
 */
public class OneRunResult {
    private EvaluationMeasure svmResult;

    public Table getTaskSpecificTable() {
        return taskSpecificTable;
    }

    private Table taskSpecificTable;

    public Table getFeatureTable() {
        return featureTable;
    }

    public void setFeatureTable(final Table featureTable) {
        this.featureTable = featureTable;
    }

    public EvaluationMeasure getSvmResult() {
        return svmResult;
    }

    public void setSvmResult(final EvaluationMeasure svmResult) {
        this.svmResult = svmResult;
    }

    private Table featureTable;

    public void setTaskSpecificTable(final Table result) {
        this.taskSpecificTable = result;
    }

    private int countShuffleGE;
    private int totalShufflingTests;

    public int getCountShuffleGE() {
        return countShuffleGE;
    }

    public int getTotalShufflingTests() {
        return totalShufflingTests;
    }

    public void setShufflingCountGE(final int countShuffleGE, final int totalShufflingTests) {
        this.countShuffleGE = countShuffleGE;
        this.totalShufflingTests = totalShufflingTests;
    }

    public int getOverlap() {
        return getFeatureTable().getColumnNumber() - 1;
    }
}
