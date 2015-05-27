package edu.cornell.med.icb.geo.tools;

import org.bdval.SplitPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ClassificationTask of this kind represent regression tasks.
 * Created by fac2003 on 5/26/15.
 */
public class RegressionTask extends ClassificationTask {

    public static final String REGRESSION = "REGRESSION";

    public RegressionTask(String experimentName) {
        super.setExperimentDataFilename(experimentName);
    }

    /**
     * Return an array of all condition names (classes).
     *
     * @return the condition names.
     */
    public String[] getConditionNames() {
        return new String[]{REGRESSION};
    }


    public void setConditionsIdentifiers(final ConditionIdentifiers conditionsIdentifiers) {
        // no need to check anything for regression, each sample has potentially a different value.
        super.conditionsIdentifiers = conditionsIdentifiers;
    }

    public String getConditionName(final int conditionIndex) {
        return REGRESSION;
    }

    public int getNumberOfConditions() {
        return 1;
    }

    public List<Set<String>> calculateLabelValueGroups() {
        final ClassificationTask classificationTask = this;
        final List<Set<String>> labelValueGroups;
        labelValueGroups = new ArrayList<Set<String>>();

        final ConditionIdentifiers conditionsIdentifiers = classificationTask.getConditionsIdentifiers();
        final RegressionLabels labels = (RegressionLabels) conditionsIdentifiers;
        labelValueGroups.add(labels.getLabelGroup(RegressionTask.REGRESSION));
        return labelValueGroups;
    }

    public RegressionLabels getLabels() {
        return (RegressionLabels) super.getConditionsIdentifiers();
    }

    public ClassificationTask filterBySplitPlan(SplitPlan splitPlan, int splitId, String splitType) {
        RegressionTask task = new RegressionTask(this.getExperimentDataFilename());

        RegressionLabels labels = getLabels();
        Set<String> samplesInSplit = labels.getLabelGroup(task.getFirstConditionName());

        assert samplesInSplit.size() > 0 : " Samples must remain after filtering for split. No samples left in splitId=" + splitId + " splitType=" + splitType;
        ConditionIdentifiers newIds = labels.reduceToSamples(samplesInSplit);
        task.setConditionsIdentifiers(newIds);
        return task;

    }

    public int getNumberSamplesSecondCondition() {
        return 0;
    }
}
