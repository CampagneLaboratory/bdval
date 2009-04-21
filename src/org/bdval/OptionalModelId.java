package org.bdval;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
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


    public OptionalModelId(String columnIdentifier) {
        this.columnIdentifier = columnIdentifier;
        this.excludeArgumentNames = new ObjectArrayList<String>();
        this.skipValueForArgumentName = new IntArrayList();

    }

    /**
     * Name of arguments to exclude from command line when calculating hash code modelid.
     */
    ObjectList<String> excludeArgumentNames;
    /**
     * The number of arguments that follow the command line arguments to ignore when creating
     * the hash key for this optional model id.
     */
    IntArrayList skipValueForArgumentName;

    public void addExcludeArgument(String excludeArgumentName, int skipForArgument) {
        excludeArgumentNames.add(excludeArgumentName);
        skipValueForArgumentName.add(skipForArgument);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("column-id: ");
        buffer.append(columnIdentifier);
        buffer.append('\n');
        int i = 0;
        for (String argName : excludeArgumentNames) {
            buffer.append("skip.arg-name: ");
            buffer.append(argName);
            buffer.append('\n');
            buffer.append("skip.value: ");
            buffer.append(skipValueForArgumentName.get(i++));
            buffer.append('\n');
        }
        return buffer.toString();
    }

    public int skipValue(String argumentName) {
        int index = excludeArgumentNames.indexOf(argumentName);
        return skipValueForArgumentName.get(index);
    }
}
