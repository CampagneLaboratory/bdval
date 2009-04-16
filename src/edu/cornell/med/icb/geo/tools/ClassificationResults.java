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

import edu.cornell.med.icb.tools.svmlight.EvaluationMeasure;
import edu.cornell.med.icb.tools.svmlight.EvaluationMeasureCache;

import java.io.File;
import java.io.PrintWriter;
import java.text.Format;

/**
 * @author Fabien Campagne Date: Mar 5, 2006 Time: 3:03:11 PM
 */
public class ClassificationResults {
    private String datasetName;
    private String geneListName;
    private int overlap;
    private EvaluationMeasureCache looPerformance;
    private double percentRandomGEP;
    private ClassificationTask task;
    private String classificationUnique;
    private int rank;
    private int countShuffleGE;
    boolean shufflingLabelRun;

    public int getTotalShufflingTests() {
        return totalShufflingTests;
    }

    public int getCountShuffleGE() {
        return countShuffleGE;
    }

    private int totalShufflingTests;

    public int getRank() {
        return rank;
    }

    public void setRank(final int rank) {
        this.rank = rank;
    }

    private ClassificationResults() {
        super();

    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getGeneListName() {
        return geneListName;
    }

    public int getOverlap() {
        return overlap;
    }

    public EvaluationMeasure getLooPerformance() {
        return looPerformance;
    }

    public double getPercentRandomGEP() {
        return percentRandomGEP;
    }

    public ClassificationTask getTask() {
        return task;
    }

    public String getClassificationUnique() {
        return classificationUnique;
    }

    public static ClassificationResults create(final String line) {
        final ClassificationResults result = new ClassificationResults();
        result.parse(line);
        return result;
    }

    public void parse(final String line) {

        final String[] tokens = line.split("[\t]");
        final int LENGTH_WHEN_SHUFFLING_RUN = 17;
        assert tokens.length == 12 ||
                tokens.length
                        == LENGTH_WHEN_SHUFFLING_RUN : "Statistics must have 12 or 15 fields per line, separated by tab.";

        int tokenCount = 0;
        this.datasetName = tokens[tokenCount++];
        this.geneListName = tokens[tokenCount++];
        this.overlap = Integer.parseInt(tokens[tokenCount++]);
        this.looPerformance = new EvaluationMeasureCache();
        final double accuracy = Double.parseDouble(tokens[tokenCount++]);
        final double precision = Double.parseDouble(tokens[tokenCount++]);
        final double recall = Double.parseDouble(tokens[tokenCount++]);
        looPerformance.setErrorRate(100 - accuracy);
        looPerformance.setPrecision(precision);
        looPerformance.setRecall(recall);
        tokenCount++; // F-1 is ignored. It is recalculated.

        this.percentRandomGEP = Double.parseDouble(tokens[tokenCount++]);
        this.task = new ClassificationTask();
        task.setFirstConditionName(tokens[tokenCount++]);
        task.setSecondConditionName(tokens[tokenCount++]);
        task.setNumberSamplesFirstCondition(Integer.parseInt(tokens[tokenCount++]));
        task.setNumberSamplesSecondCondition(Integer.parseInt(tokens[tokenCount++]));
        this.classificationUnique = tokens[tokenCount++];

        if (tokens.length == LENGTH_WHEN_SHUFFLING_RUN) {
            this.shufflingLabelRun = true;
            tokenCount++; // GEP is ignored. Calculated.
            countShuffleGE = Integer.parseInt(tokens[tokenCount++]);
            totalShufflingTests = Integer.parseInt(tokens[tokenCount++]);
        }
    }


    public static void writeHeader(final PrintWriter statWriter, final boolean shufflingLabelRun) {

// format is :
// input-filename gene-list-name overlap accuracy precision recall f-1 percentRandomGreaterOrEqual condition1 condition2 n1 n2
        statWriter.write("Dataset");
        statWriter.write('\t');
        statWriter.write("Gene List");
        statWriter.write('\t');
        statWriter.write("Overlap");
        statWriter.write('\t');
        statWriter.write("Accuracy");
        statWriter.write('\t');
        statWriter.write("Precision");
        statWriter.write('\t');
        statWriter.write("Recall");
        statWriter.write('\t');
        statWriter.write("F-1 measure");
        statWriter.write('\t');
        statWriter.write("Percent Random GEP");
        statWriter.write('\t');
        statWriter.write("First Condition");
        statWriter.write('\t');
        statWriter.write("Second Condition");
        statWriter.write('\t');
        statWriter.write("#Samples First Condition");
        statWriter.write('\t');
        statWriter.write("#Samples Second Condition");
        statWriter.write('\t');
        statWriter.write("ClassificationUnique");

        if (shufflingLabelRun) {
            statWriter.write('\t');
            statWriter.write("ShufflingRunGEP");
            statWriter.write('\t');
            statWriter.write("ShufflingRunGE");
            statWriter.write('\t');
            statWriter.write("ShufflingTestCount");

        }
        statWriter.write('\t');
        statWriter.write("Rank");
        statWriter.write('\n');

        statWriter.flush();
    }

    public void write(final PrintWriter statWriter, final Format formatter) {
        String shortName = datasetName;

        final File file = new File(datasetName);
        shortName = file.getName();

        statWriter.write(shortName);
        statWriter.write('\t');
        statWriter.write(geneListName);
        statWriter.write('\t');
        statWriter.write(Integer.toString(overlap));
        statWriter.write('\t');
        statWriter.write(formatter.format(looPerformance.getAccuracy()));
        statWriter.write('\t');
        statWriter.write(formatter.format(looPerformance.getPrecision()));
        statWriter.write('\t');
        statWriter.write(formatter.format(looPerformance.getRecall()));
        statWriter.write('\t');
        statWriter.write(formatter.format(looPerformance.getF1Measure()));
        statWriter.write('\t');
        statWriter.write(formatter.format(percentRandomGEP));
        statWriter.write('\t');
        statWriter.write(task.getFirstConditionName());
        statWriter.write('\t');
        statWriter.write(task.getSecondConditionName());
        statWriter.write('\t');
        statWriter.write(Integer.toString(task.getNumberSamplesFirstCondition()));
        statWriter.write('\t');
        statWriter.write(Integer.toString(task.getNumberSamplesSecondCondition()));
        statWriter.write('\t');
        statWriter.write(classificationUnique);

        if (shufflingLabelRun) {
            statWriter.write('\t');
            statWriter.write(Float.toString(getShuffleGEP()));
            statWriter.write('\t');
            statWriter.write(Integer.toString(this.countShuffleGE));
            statWriter.write('\t');
            statWriter.write(Integer.toString(this.totalShufflingTests));
        }
        statWriter.write('\t');
        statWriter.write(Integer.toString(rank));
        statWriter.write('\n');
        statWriter.flush();
    }

    public ClassificationResults(final String datasetName, final String geneListName, final int overlap,
                                 final EvaluationMeasure looPerformance,
                                 final double percentRandomGEP, final ClassificationTask task) {
        super();
        this.datasetName = datasetName;
        this.geneListName = geneListName;
        this.overlap = overlap;
        this.looPerformance = new EvaluationMeasureCache(looPerformance);
        this.percentRandomGEP = percentRandomGEP;
        this.task = task;
        classificationUnique = task.getFirstConditionName() + "_" + task.getSecondConditionName();
    }


    public void setShuffleTestGECounts(final int countShuffleGE, final int totalShufflingTests) {
        shufflingLabelRun = true;
        this.countShuffleGE = countShuffleGE;
        this.totalShufflingTests = totalShufflingTests;
    }

    public float getShuffleGEP() {
        if (totalShufflingTests == 0) {
            return 100;
        } else {
            float percent = countShuffleGE;
            percent /= totalShufflingTests;
            percent *= 100;
            return percent;
        }
    }
}


