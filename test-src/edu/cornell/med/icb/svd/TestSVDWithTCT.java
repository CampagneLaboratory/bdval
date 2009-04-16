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

/**
 * Tests SVD functions using the TCT implementation.
 */
public final class TestSVDWithTCT extends AbstractTestSVD {
    /**
     * Gets the {@link SingularValueDecomposition} type used for the tests.
     *
     * @return The {@link edu.cornell.med.icb.svd.SVDFactory.ImplementationType} used for the tests
     */
    @Override
    public SVDFactory.ImplementationType getSVDImplementationType() {
        return SVDFactory.ImplementationType.TCT;
    }
}
