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

package edu.cornell.med.icb.biomarkers;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import edu.cornell.med.icb.biomarkers.pathways.PathwayInfo;
import edu.cornell.med.icb.geo.DoubleIndexedIdentifier;
import edu.cornell.med.icb.geo.GEOPlatformIndexed;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.GEOPlatform;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.learning.FeatureScaler;
import edu.mssm.crover.tables.Table;
import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.lang.StringUtils;

import java.io.PrintWriter;
import java.util.Vector;

/**
 * Options common to Discovery and Validate Modes.
 *
 * @author Fabien Campagne Date: Oct 19, 2007 Time: 2:14:01 PM
 */
public class DAVOptions {
    public String input;
    public PrintWriter output;
    public int crossValidationFoldNumber;
    public ClassificationTask[] classificationTasks;
    public Table inputTable;
    public GeneList[] geneLists;
    public Vector<GEOPlatform> platforms;
    public final String IDENTIFIER_COLUMN_NAME = "ID_REF";
    public boolean adjustSignalToFloorValue;
    public double signalFloorValue;
    public RandomEngine randomGenerator = new MersenneTwister();
    public int randomSeed;
    public int rservePort = -1;
    private String geneFeaturesDir = "./";
    public Object2DoubleMap<MutableString> probesetScaleMeanMap =
            new Object2DoubleLinkedOpenHashMap<MutableString>();
    public Object2DoubleMap<MutableString> probesetScaleRangeMap =
            new Object2DoubleLinkedOpenHashMap<MutableString>();
    /**
     * Populated when pathway and gene2Probe options are used.
     */
    Gene2Probesets gene2Probe;
    DoubleIndexedIdentifier probeIndexMapping;
    DoubleIndexedIdentifier pathwayIndexMapping;
    public ObjectSet<PathwayInfo> pathways;

    /** Submission file variables. */
    public String submissionFilename;
    public PrintWriter submissionOutput;
    public boolean submissionFilePreexist;

    public String datasetName;
    public String datasetRoot;

    public String modelId;

    /**
     * True if the output file was not created by this program. In that case, header are assumed
     * to have been written already and are not repeated in the output.
     */
    public boolean outputFilePreexist;
    public String[] classifierParameters;
    public Class classiferClass;
    public boolean scaleFeatures;
    public boolean quiet;
    public boolean normalizeFeatures;
    public boolean percentileScaling;
    public boolean overwriteOutput;
    public Class<? extends FeatureScaler> scalerClass;
    public boolean loggedArray;
    public String scalerClassName;
    public String pathwayAggregtionMethod;
    public String pathwaysInfoFilename;
    public String geneToProbeFilename;

    public DAVOptions() {
        super();
        this.trainingPlatform = new GEOPlatformIndexed();
    }

    public boolean oneChannelArray = true;
    /**
     * Platform for probeset used for training.
     */
    GEOPlatformIndexed trainingPlatform;

    public int registerProbeset(final String identifier) {
        trainingPlatform.registerProbeId(identifier, identifier);
        return trainingPlatform.getProbeIds()
                .get(new MutableString(identifier));
    }


    public MutableString getProbesetIdentifier(final int probesetIndex) {
        return trainingPlatform.getProbesetIdentifier(probesetIndex);
    }

    public void resetTrainingPlatform() {
        trainingPlatform = new GEOPlatformIndexed();
    }

    /**
     * Reset the randomSeed as initially set through arguments.
     */
    public void resetRandomSeed() {
        randomGenerator = new MersenneTwister(randomSeed);
    }

    public String classifierParametersAsString() {
        final StringBuilder out = new StringBuilder();
        for (final String option : classifierParameters) {
            out.append(option);
            out.append(',');
        }
        return out.toString();
    }

    public void setGeneFeaturesDir(final String geneFeaturesDir) {
        if (StringUtils.isBlank(geneFeaturesDir))  {
            this.geneFeaturesDir = "";
        } else {
            if (geneFeaturesDir.endsWith("/") || geneFeaturesDir.endsWith("\\")) {
                this.geneFeaturesDir = geneFeaturesDir;
            } else {
                this.geneFeaturesDir = geneFeaturesDir + "/";
            }
        }
    }

    public String getGeneFeaturesDir() {
        return geneFeaturesDir;
    }

}
