package org.bdval;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
/*
 * Copyright (C) 2007-2008 Institute for Computational Biomedicine,
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
 * An optional model id may be created to hash a subset of model creation parameters.
 * This class helps specify which model parameters should be excluded from the hash
 * key. Excluding parameters helps compare two or several conditions, where the models
 * differ only by some attribute. Since the optional key ignores these parameters, the
 * key can be used to match models even when they differ by one or a few attributes.
 * This is useful for instance to compare model statistics when models are built with
 * or without feature scaling. Several model attribute can be excluded from the optional
 * hash keys.
 *
 * @author Fabien Campagne
 *         Date: Apr 20, 2009
 *         Time: 7:54:13 PM
 */
public class OptionalModelId {
    /**
     * The name of the column that will hold this optional model id.
     */
    String columnIdentifier;
    /**
     * The number of arguments that follow the command line arguments to ignore when creating
     * the hash key for this optional model id.
     */
    public int skip;

    public OptionalModelId(String columnIdentifier) {
        this.columnIdentifier = columnIdentifier;
        this.excludeArgumentNames=new ObjectArraySet();
        this.skip=0;
    }

    ObjectSet<String> excludeArgumentNames;
}
