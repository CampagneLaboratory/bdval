package org.bdval.modelselection;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.PrintWriter;
import java.util.Map;

/**
 * @author: Fabien Campagne
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
