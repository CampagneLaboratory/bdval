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

package edu.cornell.med.icb.geo.mpss;

import edu.cornell.med.icb.identifier.IndexedIdentifier;
import edu.cornell.med.icb.io.TSVReader;
import edu.cornell.med.icb.tissueinfo.similarity.GeneTranscriptRelationships;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Converts MPSS megablast output to transcript-> tag mapping file.
 *
 * @author Fabien Campagne Date: Aug 28, 2007 Time: 12:08:52 PM
 */
public class MpssMegablast {
    static final GeneTranscriptRelationships gene2TranscriptRelationships = new GeneTranscriptRelationships();

    private MpssMegablast() {
    }

    public static void main(final String[] args) throws IOException {
        final String inputFilename = args[0];
        final String geneTranscriptFilename = args[1];
        final String outputFilename = args[2];
        final boolean skipAmbiguous = Boolean.parseBoolean(args[3]);

        final TSVReader reader = new TSVReader(new FileReader(inputFilename));
        int tagLength = -1;

        final IndexedIdentifier transcripts = gene2TranscriptRelationships.load(geneTranscriptFilename);
        final GeneTranscriptRelationships tagIndex2TranscriptIndex = new GeneTranscriptRelationships(transcripts);
        final IndexedIdentifier tags = new IndexedIdentifier();
        final Int2ObjectMap<String> transcriptIndex2transcriptId = new Int2ObjectOpenHashMap<String>();


        final ProgressLogger progressLogger = new ProgressLogger();
        progressLogger.start("Reading megablast output");
        while (reader.hasNext()) {
            reader.next();
            if (reader.numTokens() < 12) {
                continue;
            }
            final String tag = reader.getString();
            final String transcriptId = reader.getString();
            final float percentId = reader.getFloat();
            if (percentId != 100f) {
                continue;
            }
            final int alignmentLength = reader.getInt();

            if (tagLength == -1) {
                tagLength = tag.length();
            }
            if (alignmentLength != tagLength) {
                continue;
            }

            final MutableString tagMutable = new MutableString(tag).compact();

            final int tagIndex = tags.registerIdentifier(tagMutable);
            final MutableString transcriptIdMutable = new MutableString(transcriptId).compact();
            final int transcriptIndex = transcripts.registerIdentifier(transcriptIdMutable);
            tagIndex2TranscriptIndex.addRelationship(tagIndex, transcriptIndex);
            transcriptIndex2transcriptId.put(transcriptIndex, transcriptId);
            progressLogger.lightUpdate();
        }
        progressLogger.stop();

        final PrintWriter out = new PrintWriter(new FileWriter(outputFilename));

        // filter and output the results:
        out.write("#transcriptId\ttag\n");

        int count = 0;// number of tags that are written in output
        for (final MutableString tag : tags.keySet()) {

            final IntSet transcriptIndices = tagIndex2TranscriptIndex.getTranscriptSet(tags.getInt(tag));
            final IntSet geneIndices = gene2TranscriptRelationships.transcript2Genes(transcriptIndices.toIntArray());
            if (geneIndices.size() > 1) {
                if (skipAmbiguous) {
                    System.out.println("tag " + tag + " matches multiple genes");
                    continue;
                }
            }

            count++;
            for (final int transcriptIndex : transcriptIndices.toIntArray()) {
                final String transcriptId = transcriptIndex2transcriptId.get(transcriptIndex);

                out.write(transcriptId);
                out.write("\t");
                tag.write(out);
                out.write("\n");
            }
        }
        out.close();
        System.err.println(String.format("Wrote %d tags to output.", count));
    }
}
