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

import edu.mssm.crover.cli.CLI;
import gominer.Fisher;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Calculate the overlap between two gene lists.
 *
 * @author Fabien Campagne Date: Mar 29, 2006 Time: 4:48:17 PM
 */
public class OverlapGeneLists {
    public static void main(final String[] args) throws IOException {
        final OverlapGeneLists processor = new OverlapGeneLists();
        processor.process(args);
    }

    private void process(final String[] args) throws IOException {
        final String filename1 = CLI.getOption(args, "-i", null);
        final String filename2 = CLI.getOption(args, "-j", null);
        final String geneListDirectory = CLI.getOption(args, "-d", "data/gene-lists");
        if (filename2 == null) {
            final File dir = new File(geneListDirectory);

            final FilenameFilter filter = new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    if (name.endsWith("-refseq")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };
            final File[] list = dir.listFiles(filter);
            final Fisher fisher = new Fisher();
            for (final File geneListFile : list) {
                final Overlap overlap = overlap(filename1, geneListFile.getCanonicalPath());

                final int countInGenome = 16093;
                System.out.println(reduce(filename1) + " - " + reduce(geneListFile.getName()) + " overlap: " + overlap.count
                        + " size1: " + overlap.size1 + " size2: " + overlap.size2 + " P-value: " +
                        fisher.fisher(overlap.size1,
                                overlap.count,
                                countInGenome,
                                overlap.size2));
                for (final String id : overlap.overlappingIds) {
                    System.out.println(id);
                    System.out.flush();
                }
                System.out.flush();
            }
            System.out.println("Global overlap across all lists of size>=1:");
            for (final String id : globalOverlap) {
                System.out.println(id);
            }
        } else {
            final Overlap overlap = overlap(filename1, filename2);
            System.out.println("overlap: " + overlap.count);

        }
    }

    private static String reduce(final String canonicalPath) {
        final File file = new File(canonicalPath);
        final String shortName = file.getName();
        int index = shortName.indexOf("-genes");
        if (index == -1) {
            index = shortName.indexOf("-ens");
        }
        if (index == -1) {
            index = shortName.length();
        }
        return shortName.substring(0, index);

    }

    private class Overlap {
        final int count;
        final int size1;
        final int size2;
        final Set<String> overlappingIds;

        public Overlap(final Set<String> ids, final int count, final int size1, final int size2) {
            super();
            this.count = count;
            this.size1 = size1;
            this.size2 = size2;
            this.overlappingIds = ids;
        }
    }

    final Set<String> globalOverlap = new HashSet<String>();

    private Overlap overlap(final String filename1, final String filename2) throws IOException {
        final String[] tokens1 = {filename1, filename1};
        final FileGeneList list1 = new FileGeneList(tokens1);
        final String[] tokens2 = {filename2, filename2};
        final FileGeneList list2 = new FileGeneList(tokens2);


        final Set<String> ids = new HashSet<String>();
        ids.addAll(list1.getPrimaryIDs());
        ids.retainAll(list2.getPrimaryIDs());


        if (globalOverlap.isEmpty()) {
            globalOverlap.addAll(ids);
        } else {
            globalOverlap.retainAll(ids);
        }
        final Overlap overlap = new Overlap(ids, ids.size(), list1.getPrimaryIDs().size(), list2.getPrimaryIDs().size());
        return overlap;
    }
}
