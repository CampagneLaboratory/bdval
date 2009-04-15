/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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