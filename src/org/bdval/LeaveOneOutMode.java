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

package org.bdval;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import edu.cornell.med.icb.learning.ClassificationHelper;
import edu.cornell.med.icb.learning.CrossValidation;
import edu.cornell.med.icb.learning.FeatureScaler;
import edu.cornell.med.icb.tools.geo.ClassificationTask;
import edu.cornell.med.icb.tools.geo.MicroarrayTrainEvaluate;
import edu.cornell.med.icb.tools.svmlight.EvaluationMeasure;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;

/**
 * Evaluate performance by leave-one-out approach.
 *
 * @author Fabien Campagne Date: Oct 25, 2007 Time: 3:31:04 PM
 */
public class LeaveOneOutMode extends CrossValidationMode {
    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        super.defineOptions(jsap);
        jsap.unregisterParameter(jsap.getByID("folds"));
    }

    @Override
    protected EvaluationMeasure measureCV(final ClassificationTask task,
                                          final Table processedTable,
                                          final int crossValidationFoldNumber, final int seed,
                                          final Class<? extends FeatureScaler> scalerClass)
            throws TypeMismatchException, InvalidColumnException {
        final ClassificationHelper helper = getClassifier(processedTable,
                        MicroarrayTrainEvaluate.calculateLabelValueGroups(task));
        final RandomEngine randomEngine = new MersenneTwister(seed);
        final CrossValidation crossValidation =
                new CrossValidation(helper.classifier, helper.problem, randomEngine);
        return crossValidation.leaveOneOutEvaluation();
    }
}
