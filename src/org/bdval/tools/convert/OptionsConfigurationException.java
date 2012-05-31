/*
 * Copyright (C) 2008-2010 Institute for Computational Biomedicine,
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

package org.bdval.tools.convert;

/**
 * Error configuring options.
 * @author Kevin Dorff
 */
public class OptionsConfigurationException extends Exception {

    /** A no args exception. */
    public OptionsConfigurationException() {
        super();
    }

    /**
     * Exception with just a message.
     * @param message the exception message
     */
    public OptionsConfigurationException(final String message) {
        super(message);
    }

    /**
     * Exception with a message and a root cause.
     * @param message the exception message
     * @param cause the root cause
     */
    public OptionsConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception with just a root cause.
     * @param cause the root cause
     */
    public OptionsConfigurationException(final Throwable cause) {
        super(cause);
    }

}
