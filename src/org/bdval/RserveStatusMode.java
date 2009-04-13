/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.R.RUtils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.REngine.Rserve.RserveException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This mode is used to validate and check the status of Rserve instances configured to
 * be used with BDVal.
 */
public final class RserveStatusMode extends DAVMode {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(RserveStatusMode.class);

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        // remove options specific to other modes
        final Iterator ids = jsap.getIDMap().idIterator();
        final List<String> idsToRemove = new LinkedList<String>();
        while (ids.hasNext()) {
            final String id = (String) ids.next();
            if (!"mode".equals(id) && !"help".equals(id)) {
                idsToRemove.add(id);
            }
        }
        for (final String id : idsToRemove) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Removing parameter: " + id);
            }
            jsap.unregisterParameter(jsap.getByID(id));
        }

        // allow the user to specify a port number to communicate with the Rserve process
        final Parameter portOption = new FlaggedOption("port")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("port")
                .setHelp("Use specified port to communicate with the Rserve process");
        jsap.registerParameter(portOption);

        // allow the user to specify a host that is running the Rserve process
        final Parameter hostOption = new FlaggedOption("host")
                .setStringParser(JSAP.INETADDRESS_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("host")
                .setHelp("Communicate with the Rserve process on the given host");
        jsap.registerParameter(hostOption);

        // allow the user to specify the username to send to the Rserve process
        final Parameter userOption = new FlaggedOption("username")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("username")
                .setHelp("Username to send to the Rserve process");
        jsap.registerParameter(userOption);

        // allow the user to specify a password to the Rserve process
        final Parameter passwordOption = new FlaggedOption("password")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(false)
                .setLongFlag("password")
                .setHelp("Password to send to the Rserve process");
        jsap.registerParameter(passwordOption);

        // allow the user to specify a configuration file for the rserve processes
        final Parameter configurationOption = new FlaggedOption("configuration")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault("config/RConnectionPool.xml")
                .setRequired(false)
                .setLongFlag("configuration")
                .setHelp("Configuration file or url to read from");

        jsap.registerParameter(configurationOption);
    }

    /**
     * Interpret the command line arguments.
     * @param jsap the JSAP command line parser
     * @param result the results of command line parsing
     * @param options the DAVOptions
     */
    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final DAVOptions options) {
        final List<String> argList = new LinkedList<String>();
        argList.add("--validate");

        boolean hasSpecificArgs = false;
        if (result.contains("host")) {
            argList.add("--host");
            argList.add(result.getInetAddress("host").getHostName());
            hasSpecificArgs = true;
        }

        if (result.contains("port")) {
            argList.add("--port");
            argList.add(Integer.toString(result.getInt("port")));
            hasSpecificArgs = true;
        }

        final String user = result.getString("username");
        if (StringUtils.isNotBlank(user)) {
            argList.add("--username");
            argList.add(user);
            hasSpecificArgs = true;
        }

        final String password = result.getString("password");
        if (StringUtils.isNotBlank(password)) {
            argList.add("--password");
            argList.add(password);
            hasSpecificArgs = true;
        }

        if (hasSpecificArgs && result.userSpecified("configuration")) {
            LOG.error("configuration option cannot be used with other options");
            printHelp(jsap);
            return;
        }

        if (!hasSpecificArgs) {
            final String configuration = result.getString("configuration");
            if (StringUtils.isNotBlank(configuration)) {
                argList.add("--configuration");
                argList.add(configuration);
            }
        }

        final String[] args = argList.toArray(new String[argList.size()]);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Calling RUtils with: " + ArrayUtils.toString(args));
        }

        try {
            RUtils.main(args);
        } catch (ParseException e) {
            LOG.error("Error parsing options", e);
        } catch (RserveException e) {
            LOG.error("Rserve connection error", e);
        } catch (ConfigurationException e) {
            LOG.error("Rserve configuration error", e);
        }
    }
}
