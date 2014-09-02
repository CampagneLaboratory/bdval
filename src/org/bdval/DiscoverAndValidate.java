/*
 * Copyright (C) 2007-2010 Institute for Computational Biomedicine,
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

import com.martiansoftware.jsap.JSAPException;
import edu.cornell.med.icb.util.VersionUtils;
import edu.mssm.crover.cli.CLI;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bdval.modelconditions.ProcessModelConditions;

import java.util.HashSet;

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
    private static final HashSet<String> modelConditionModes = new HashSet<String>();

    static {
        modelConditionModes.add("stats");
    }

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

        final String mode = CLI.getOption(args, "-m", CLI.getOption(args, "--mode", null));
        if (modelConditionModes.contains(mode)) {
            final ProcessModelConditions pmcTool = new ProcessModelConditions();
            pmcTool.process(args);
        } else {
            final DiscoverAndValidate davTool = new DiscoverAndValidate();
            davTool.process(args);
        }

    }

    /**
     * Process command line arguments.  This method takes care of registering new
     * modes and delegates the processing to one or more of these modes.
     *
     * @param args The list command line arguments
     * @throws JSAPException          if there is a problem parsing the command line.
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
     *
     * @param args The list command line arguments
     * @return The DAVMode object created during this session.
     * @throws JSAPException          if there is a problem parsing the command line.
     * @throws IllegalAccessException if a registered mode cannot be accessed
     * @throws InstantiationException if a registerd mode cannot be instantiated
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
        davMode.registerMode("hubs", DiscoverWithHubs.class);
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
        davMode.registerMode("cox-regression", DiscoverWithCoxRegression.class);
        davMode.registerMode("to-ranks", ToRanksMode.class);
        davMode.registerMode("permutation", DiscoverWithPermutation.class);

        final TimeLoggingService timeService = new TimeLoggingService(args);
        timeService.start();
        final DAVOptions options = new DAVOptions();
        davMode.process(args, options);
        timeService.stop();
        if (options.output != null) {
            options.output.flush();
        }
        return davMode;
    }
}
