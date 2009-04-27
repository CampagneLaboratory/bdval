package org.bdval;

import cern.colt.Timer;
import org.apache.log4j.Logger;
import edu.mssm.crover.cli.CLI;
/*
 * Copyright (C) 2009 Institute for Computational Biomedicine,
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

/**
 * @author Fabien Campagne
 *         Date: Apr 27, 2009
 *         Time: 1:20:42 PM
 */
public class TimeLoggingService {
    private static final Logger LOGGER = Logger.getLogger(TimeLoggingService.class);
    private String mode;
    private String modelId;

    public TimeLoggingService(String args[]) {
        mode = CLI.getOption(args, "--mode", CLI.getOption(args, "-m", "no-mode-argument"));
        modelId = CLI.getOption(args, "--model-id", "no-model-id");
    }

    public TimeLoggingService(String mode, String modelId) {
        this.mode = mode;
        this.modelId = modelId;
    }

    public TimeLoggingService(String mode) {

        this.mode = mode;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    Timer timer = new Timer();

    public void start() {
        LOGGER.trace("TIMING:modelId:" + modelId + ":MODE:" + mode + ":START");
        timer.start();
    }

    public void stop() {

        LOGGER.trace("TIMING:modelId:" + modelId + ":MODE:" + mode + ":STOP");
        LOGGER.trace("TIMING:modelId:" + modelId + ":MODE:" + mode + ":DURATION_SECONDS:" + timer.seconds());
        LOGGER.trace("TIMING:modelId:" + modelId + ":MODE:" + mode + ":DURATION_MINUTES:" + timer.minutes());

        timer.stop();
    }
}
