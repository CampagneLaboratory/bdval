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

package edu.cornell.med.icb.svd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Fabien Campagne Date: Dec 13, 2007 Time: 2:56:55 PM
 */
public final class SVDFactory {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(SVDFactory.class);

    public enum ImplementationType {
        COLT   (SingularValueDecompositionWithColt.class),
        R      (SingularValueDecompositionWithR.class),
        LAPACK (SingularValueDecompositionWithLAPACK.class),
        MTJ    (SingularValueDecompositionWithMTJ.class),
        TCT    (SingularValueDecompositionWithTCT.class);

        private final Class<? extends SingularValueDecomposition> svdClass;
        ImplementationType(final Class<? extends SingularValueDecomposition> clazz) {
            svdClass = clazz;
        }
        public Class<? extends SingularValueDecomposition> getSvdClass() {
            return svdClass;
        }
    }

    private SVDFactory() {
        super();
    }

    public static SingularValueDecomposition getImplementation(final ImplementationType type) {
        final SingularValueDecomposition implementation;
        try {
            implementation = type.getSvdClass().newInstance();
        } catch (InstantiationException e) {
            LOG.error("No such implementation: " + type.toString(), e);
            throw new SVDRuntimeException(e);
        } catch (IllegalAccessException e) {
            LOG.error("No such implementation: " + type.toString(), e);
            throw new SVDRuntimeException(e);
        }
        return implementation;
    }

    public static SingularValueDecomposition getImplementation(final String name) {
        final SingularValueDecomposition implementation;
        try {
            implementation = getImplementation(ImplementationType.valueOf(name));
        } catch (IllegalArgumentException e) {
            LOG.error("No such implementation: " + name);
            throw new SVDRuntimeException(e);
        }
        return implementation;
    }
}
