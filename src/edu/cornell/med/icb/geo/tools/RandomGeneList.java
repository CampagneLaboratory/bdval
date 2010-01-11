/*
 * Copyright (C) 2006-2010 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.geo.tools;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Fabien Campagne Date: Mar 1, 2006 Time: 12:09:36 PM
 */
public class RandomGeneList extends GeneList {
    private RandomEngine randomGenerator;
    /**
     * Genes that are in the exclusion gene list cannot be added to the random gene list.
     */
    private GeneList exclusionGeneList;
    private Set<String> cachedProbesetIds;

    public void setExclusionGeneList(final GeneList exclusionGeneList) {
        this.exclusionGeneList = exclusionGeneList;
    }

    public RandomGeneList(final int numberOfProbesets, final int seed) {
        super("random");
        init(seed, numberOfProbesets);
    }

    public RandomGeneList(final int numberOfProbesets, final RandomEngine randomNumberGenerator) {
        super("random");
        init(0, numberOfProbesets);
        this.randomGenerator = randomNumberGenerator;
    }

    public RandomGeneList(final String[] tokens) {
        super(tokens[0]);
        final int numProbeSets = Integer.parseInt(tokens[1]);
        final int seed = Integer.parseInt(tokens[2]);
        init(seed, numProbeSets);
    }

    private void init(final int seed, final int numProbeSets) {
        setSeed(seed);
        setNumberOfProbesets(numProbeSets);
        setExclusionGeneList(new EmptyGeneList());
    }

    private int seed; // if TYPE_RANDOM
    private int numberOfProbesets; // if TYPE_RANDOM

    public int getNumberOfProbesets() {
        return numberOfProbesets;
    }

    public void setNumberOfProbesets(final int numberOfProbesets) {
        this.numberOfProbesets = numberOfProbesets;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(final int seed) {
        this.seed = seed;
        randomGenerator = new MersenneTwister(seed);
    }

    @Override
    public Set<String> calculateProbeSetSelection(final Table result, final int idColumnIndex) {
        assert result.getRowNumber() > numberOfProbesets : "Cannot sample more PrimaryIDSet than available. ";
        assert exclusionGeneList != null : "Exclusion gene list must not be null.";
        final Set<String> probesetIds = new HashSet<String>();
        final float collectRatio = ((float) numberOfProbesets) / (float) result.getRowNumber();
        // Each value in the ID column is a probeset id.
        final int trialNumber = 0;
        // collect all probesets in the array into availableProbesets
        final String[] availableProbesets = new String[result.getRowNumber()];
        final Table.RowIterator riSource = result.firstRow();
        int i = 0;
        while (!riSource.end()) {
            try {
                availableProbesets[i++] = result.getValue(idColumnIndex, riSource).toString();
                riSource.next();
            } catch (TypeMismatchException e) {
                assert false : "This exception must not be thrown, but was: " + e;
            } catch (InvalidColumnException e) {
                assert false : "This exception must not be thrown, but was: " + e;
            }
        }

        while (probesetIds.size() < numberOfProbesets) {
            final int index = nextRandomInt(availableProbesets.length);
            final String probesetId = availableProbesets[index];
            if (!exclusionGeneList.isProbesetInList(probesetId)) {
                probesetIds.add(probesetId);
            }
        }

        assert probesetIds.size() == numberOfProbesets : "Failed to collect exactly the number of PrimaryIDSet requested.";
        cachedProbesetIds = probesetIds;
        return probesetIds;
    }

    @Override
    public Set<String> calculateProbeSetSelection(final ObjectSet<CharSequence> tableProbesetIds) {
        throw new InternalError("This method is not implemented.");
    }

    /**
     * Returns a random number between 0 (inclusive) and max (exclusive).
     *
     * @param max Upper bound (exclusive) on the value returned.
     * @return The next integer in the range [0 - max[
     */
    private int nextRandomInt(final int max) {
        final float randomValue = randomGenerator.nextFloat();
        final int newRandom = Math.round(randomValue * (max - 1));
        return newRandom;
    }

    @Override
    public boolean isProbesetInList(final String probesetId) {
        return cachedProbesetIds.contains(probesetId);
    }

    public void excludeGenesFromList(final GeneList currentGeneList) {
        this.exclusionGeneList = currentGeneList;

    }
}




