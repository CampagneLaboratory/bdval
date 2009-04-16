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

package edu.cornell.med.icb.svd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests SVD functions using the R implementation.
 */
public final class TestSVDWithR extends AbstractTestSVD {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(TestSVDWithR.class);

    /**
     * Gets the {@link SingularValueDecomposition} type
     * used for the tests.
     * @return The {@link edu.cornell.med.icb.svd.SVDFactory.ImplementationType}
     * used for the tests
     */
    @Override
    public SVDFactory.ImplementationType getSVDImplementationType() {
        return SVDFactory.ImplementationType.R;
    }

    @BeforeClass
    public static void initializeRConnectionPool() {
        LOG.debug("using connection pool");
        rConnectionPoolUsed = true;
    }

    @Test
    public void testR2x3() {
        svdImplementation.svd(matrix2x3, 2, 0);
        test2x3(matrix2x3, svdImplementation);
    }
}