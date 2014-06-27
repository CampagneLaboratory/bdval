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

package org.bdval;

import edu.cornell.med.icb.io.TSVReader;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.io.FileReader;
import java.io.IOException;

/**
 * @author Fabien Campagne
 * Date: Apr 2, 2008
 * Time: 6:27:54 PM
 */
public class SplitPlan {
    /**
     * A list of splits that make up the entire plan.
     */
    private final ObjectList<SplitInfo> splitInfoList;

    /**
     * Create a new empty split plan.
     */
    public SplitPlan() {
        super();
        splitInfoList = new ObjectArrayList<SplitInfo>();
    }

    /**
     * Load a split plan from the given tab-delimited file.
     * @param filename The name of the file to load
     * @throws IOException if there is a problem loading the file.
     */
    public void load(final String filename) throws IOException {
        TSVReader reader = null;
        try {
            reader = new TSVReader(new FileReader(filename));
            while (reader.hasNext()) {
                if (reader.isCommentLine() || reader.isEmptyLine()) {
                    reader.skip();
                } else {
                    reader.next();
                    final int splitId = reader.getInt();
                    final int repeatId = reader.getInt();
                    final int foldId = reader.getInt();
                    final String splitType = reader.getString();
                    final String sampleId = reader.getString();
                    final double label = reader.getDouble();
                    final int sampleIndex = reader.getInt();
                    final SplitInfo si = new SplitInfo(splitId, repeatId, foldId,
                            splitType, sampleId, label, sampleIndex);
                    splitInfoList.add(si);
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) { // NOPMD
                    // silently ignore
                }
            }
        }
    }

    public int getMaxSplitIndex() {
        int maxIndex = 0;
        for (final SplitInfo si : splitInfoList) {
            maxIndex = Math.max(si.splitId, maxIndex);
        }
        return maxIndex;
    }

    /**
     * Return the list of sample Ids that match a split index and split type.
     * @param splitIndex The id of the split
     * @param splitType The type of the split
     * @return a (possibly empty) list of sample ids
     */
    public ObjectSet<String> getSampleIds(final int splitIndex, final String splitType) {
        final ObjectSet<String> result = new ObjectOpenHashSet<String>();
        for (final SplitInfo si : splitInfoList) {
            if (si.splitId == splitIndex && si.splitType.equals(splitType)) {
                result.add(si.sampleId);
            }
        }
        return result;
    }

    /**
     * Get the repeat Id corresponding to a split Id.
     * @param splitIndex The id of the split
     * @return the repeatId for the specified split, or -1 if the split is not found.
     */
    public int getRepeatId(final int splitIndex) {
        for (final SplitInfo si : splitInfoList) {
            if (si.splitId == splitIndex) {
                return si.repeatId;
            }
        }
        return -1;
    }
}
