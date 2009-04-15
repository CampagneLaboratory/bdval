/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *                         All rights reserved.
 */

package org.bdval;

import com.martiansoftware.jsap.JSAPException;

/**
 * @author Fabien Campagne Date: Feb 6, 2008 Time: 10:44:57 AM
 */
public interface WithProcessMethod {
    void process(final String[] args) throws JSAPException,
            IllegalAccessException, InstantiationException;
}
