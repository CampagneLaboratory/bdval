/*
 * Copyright (C) 2007-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.bdval;

import com.martiansoftware.jsap.JSAPException;
import edu.cornell.med.icb.util.VersionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A set of programs to discover and validate biomarkers.
 *
 * @author Fabien Campagne Date: Oct 19, 2007 Time: 2:11:24 PM
 */
public class DiscoverAndValidate implements WithProcessMethod {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(DiscoverAndValidate.class);

    /**
     * Create a new DiscoverAndValidate instance.
     */
    public DiscoverAndValidate() {
        super();
    }

    public static void main(final String[] args) throws JSAPException,
            IllegalAccessException, InstantiationException {
        final String version = VersionUtils.getImplementationVersion(DiscoverAndValidate.class);
        LOG.info(DiscoverAndValidate.class.getName() + " Implementation-Version: " + version);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Running with: " + ArrayUtils.toString(args));
        }

        final DiscoverAndValidate davTool = new DiscoverAndValidate();
        davTool.process(args);
    }

    /**
     * Process command line arguments.  This method takes care of registering new
     * modes and delegates the processing to one or more of these modes.
     * @param args The list command line arguments
     * @throws JSAPException if there is a problem parsing the command line.
     * @throws IllegalAccessException if a registered mode cannot be accessed
     * @throws InstantiationException if a registerd mode cannot be instantiated
     * @see org.bdval.DAVMode
     * @see org.bdval.DAVOptions
     */
    public void process(final String[] args) throws JSAPException,
            IllegalAccessException, InstantiationException {
        processReturnDavMode(args);
    }

    /**
     * Process command line arguments.  This method takes care of registering new
     * modes and delegates the processing to one or more of these modes.
     * @param args The list command line arguments
     * @throws JSAPException if there is a problem parsing the command line.
     * @throws IllegalAccessException if a registered mode cannot be accessed
     * @throws InstantiationException if a registerd mode cannot be instantiated
     * @return The DAVMode object created during this session.
     * @see org.bdval.DAVMode
     * @see org.bdval.DAVOptions
     */
    public DAVMode processReturnDavMode(final String[] args) throws JSAPException,
            IllegalAccessException, InstantiationException {
        final DAVMode davMode = new DAVMode();

        davMode.registerMode("cross-validation", CrossValidationMode.class);
        davMode.registerMode("leave-one-out", LeaveOneOutMode.class);
        davMode.registerMode("svm-weights", DiscoverWithSvmWeights.class);
        davMode.registerMode("svm-weights-iterative", DiscoverWithSvmWeightsIterative.class);
        davMode.registerMode("t-test", DiscoverWithTTest.class);
        davMode.registerMode("fold-change", DiscoverWithFoldChange.class);
        davMode.registerMode("kendal-tau", DiscoverWithKendallTau.class);
        davMode.registerMode("ga-wrapper", DiscoverWithGeneticAlgorithm.class);
        davMode.registerMode("write-model", WriteModel.class);
        davMode.registerMode("predict", Predict.class);
        davMode.registerMode("stats", StatsMode.class);
        davMode.registerMode("sequence", SequenceMode.class);
        davMode.registerMode("min-max", DiscoverWithMinMax.class);
        davMode.registerMode("reformat", Reformat.class);
        davMode.registerMode("define-splits", DefineSplitsMode.class);
        davMode.registerMode("execute-splits", ExecuteSplitsMode.class);
        davMode.registerMode("stats-maqcii", StatsMAQCIIMode.class);
        davMode.registerMode("distribution-difference-by-feature",
                DistributionDifferenceByFeatureMode.class);
        davMode.registerMode("distribution-difference", DistributionDifferenceByModelMode.class);
        davMode.registerMode("rserve-status", RserveStatusMode.class);
// TODO:        davMode.registerMode("combine-data", CombineDataMode.class);
        davMode.registerMode("cox-regression", DiscoverWithCoxRegression.class);

        final DAVOptions options = new DAVOptions();
        davMode.process(args, options);
        if (options.output != null) {
            options.output.flush();
        }
        return davMode;
    }
}
