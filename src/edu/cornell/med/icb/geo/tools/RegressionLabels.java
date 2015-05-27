package edu.cornell.med.icb.geo.tools;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by fac2003 on 5/26/15.
 */
public class RegressionLabels extends ConditionIdentifiers {
    private static final Log LOG = LogFactory.getLog(RegressionLabels.class);
    private Object2DoubleOpenHashMap sampleToLabelMap = new Object2DoubleOpenHashMap();


    public void add(String sample, double label) {
        sampleToLabelMap.put(sample, label);
    }

    /**
     * Return the set of sample identifiers included in a condition. Sample names for samples annotated with the condition.
     */
    public Set<String> getLabelGroup(final String conditionName) {
        if (RegressionTask.REGRESSION.equals(conditionName)) {
            return sampleToLabelMap.keySet();
        } else {
            return new HashSet<String>();
        }
    }
    public static ConditionIdentifiers readLabels(final Reader fileReader) {
        // read labels/sample name mapping:
        final RegressionLabels labels = new RegressionLabels();
        final BufferedReader conditionReader = new BufferedReader(fileReader);
        String line;
        try {
            while ((line = conditionReader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                final String[] tokens = line.split("[\t]");
                if (tokens.length != 2) {
                    LOG.fatal("condition-id line must have 2 fields separated by tabs. "
                            + "Line was :" + line);
                    System.exit(1);
                }
                //   System.out.println("counting: "+tokens[0]+" "+ tokens[1]);
                labels.add(tokens[1],Double.parseDouble(tokens[0]));
            }
        } catch (IOException e) {
            LOG.error(e);
            return null;
        }

        return labels;
    }

    public boolean hasLabel(String sampleId) {
        return this.sampleToLabelMap.containsKey(sampleId);
    }

    public double getLabel(String sampleId) {
        return this.sampleToLabelMap.getDouble(sampleId);
    }

    /**
     * Return a new RegressionLabels object reduced to the samples provided as argument.
     * @param sampleIds sampleIds to keep.
     * @return
     */
    public RegressionLabels reduceToSamples(Set<String> sampleIds) {
        RegressionLabels result=new RegressionLabels();
        for (String sampleId: sampleIds) {
            if (this.sampleToLabelMap.containsKey(sampleId)) {
                result.sampleToLabelMap.put(sampleId, this.sampleToLabelMap.getDouble(sampleId));
            }
        }
        return result;
    }
}
