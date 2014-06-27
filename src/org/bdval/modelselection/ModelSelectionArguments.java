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

package org.bdval.modelselection;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.io.PrintWriter;
import java.util.Map;

/**
 * @author Fabien Campagne
 * Date: Apr 15, 2009
 * Time: 1:59:09 PM
 */
public class ModelSelectionArguments {
    String cvResultsFilename;
    String cvcfResultsFilename;
    String testFilename;

    public String rankStrategyName;
    public CandidateModelSelection.RankStrategy rankStrategy;
    public int k;
    public String endpointName;
    public String datasetName;
    public CandidateModelSelection.RankBy rankBy;
    public CandidateModelSelection.RankBy rewardPerformance;
    public String outputFilename;
    public PrintWriter output;
    public boolean outputCreated;
    public String customRanking;
    public String modelIdMapFile;
    public ObjectList<CustomRanking> customRankings;
    public String dumpFilename;
    public PrintWriter pValuesOutput;
    public boolean pValuesOutputCreated;
    public String pValueFilename;
    public String modelConditionsFilename;
    public Map<String, Map<String, String>> modelConditions;
    public boolean excludeGeneLists;
    public String rankFilename;
    public boolean rankOutputCreated;
    public PrintWriter rankOutput;
    public String modelNameString;
    public CandidateModelSelection.ModelName modelName;
    public boolean noPValueEstimation;
    //      public boolean useAllModelsForNull;

    public ObjectList<String> getCustomModelRankingList(final String dataset, final String endpoint) {
        if (customRankings.size() > 0) {
            for (final CustomRanking ranking : customRankings) {
                if (ranking.datasetName.equals(dataset) && ranking.endpointCode.equals(endpoint)) {
                    return ranking.modelIds;
                }
            }
        }
        return (customRanking != null ? new ObjectArrayList<String>(customRanking.split("[,]")) : null);
    }

    public boolean hasTestSet() {
        return testFilename != null;
    }
}
