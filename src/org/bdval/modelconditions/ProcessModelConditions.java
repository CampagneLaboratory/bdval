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

package org.bdval.modelconditions;

import com.martiansoftware.jsap.JSAPException;
import edu.cornell.med.icb.util.VersionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bdval.*;

/**
 * The mode for programs that process a set of models described in a model conditions file
 * and associated results directory.
 *
 * @author Fabien Campagne Date: Oct 19, 2007 Time: 2:11:24 PM
 */
public class ProcessModelConditions implements WithProcessMethod {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(ProcessModelConditions.class);

    /**
     * Create a new ProcessModelConditions instance.
     */
    public ProcessModelConditions() {
        super();
    }

    public static void main(final String[] args) throws JSAPException,
            IllegalAccessException, InstantiationException {
        final String version = VersionUtils.getImplementationVersion(ProcessModelConditions.class);
        LOG.info(ProcessModelConditions.class.getName() + " Implementation-Version: " + version);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Running with: " + ArrayUtils.toString(args));
        }

        final ProcessModelConditions pmcTools = new ProcessModelConditions();
        pmcTools.process(args);
    }

    /**
     * Process command line arguments.  This method takes care of registering new
     * modes and delegates the processing to one or more of these modes.
     * @param args The list command line arguments
     * @throws com.martiansoftware.jsap.JSAPException if there is a problem parsing the command line.
     * @throws IllegalAccessException if a registered mode cannot be accessed
     * @throws InstantiationException if a registerd mode cannot be instantiated
     * @see org.bdval.DAVMode
     * @see org.bdval.DAVOptions
     */
    public void process(final String[] args) throws JSAPException,
            IllegalAccessException, InstantiationException {
        processReturnPmcMode(args);
    }

    /**
     * Process command line arguments.  This method takes care of registering new
     * modes and delegates the processing to one or more of these modes.
     * @param args The list command line arguments
     * @throws com.martiansoftware.jsap.JSAPException if there is a problem parsing the command line.
     * @throws IllegalAccessException if a registered mode cannot be accessed
     * @throws InstantiationException if a registerd mode cannot be instantiated
     * @return The DAVMode object created during this session.
     * @see org.bdval.DAVMode
     * @see org.bdval.DAVOptions
     */
    public ProcessModelConditionsMode processReturnPmcMode(final String[] args) throws JSAPException,
            IllegalAccessException, InstantiationException {
        final ProcessModelConditionsMode modes = new ProcessModelConditionsMode();

        modes.registerMode("stats", RestatMode.class);

        final TimeLoggingService timeService=new TimeLoggingService(args);
        timeService.start();
        final ProcessModelConditionsOptions options = new ProcessModelConditionsOptions();
        modes.process(args, options);
        timeService.stop();
        if (options.output != null) {
            options.output.flush();
        }
        return modes;
    }

}