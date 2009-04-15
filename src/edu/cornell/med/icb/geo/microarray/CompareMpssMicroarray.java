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

package edu.cornell.med.icb.geo.microarray;

import edu.cornell.med.icb.tissueinfo.similarity.TissueESTCountsReader;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Igor Segota Date: Sep 18, 2007 Time: 11:28:10 AM
 */
public class CompareMpssMicroarray {
    private int[] globalCountsMpss;
    private int[] globalCountsMicroarray;
    private static String basenameMpss;
    private static String basenameMicroarray;
    private static String normalization;
    private BufferedWriter outCorrel;
    private final HashMap<String, Double> queryIdsCorrelations = new HashMap<String, Double>();

    public static void main(final String[] args) throws IOException {
        final CompareMpssMicroarray comparator = new CompareMpssMicroarray();
        comparator.process(args);

    }

    private void process(final String[] args) throws IOException {
        basenameMpss = CLI.getOption(args, "-bmp", null);
        final String queryIds = CLI.getOption(args, "-q",
                null); // comma separated list of query ids.
        basenameMicroarray = CLI.getOption(args, "-bma", null);
        normalization = CLI.getOption(args, "-normalize", null);

        final TissueESTCountsReader readerMpss =
                new TissueESTCountsReader(basenameMpss);
        final TissueESTCountsReader readerMicroarray =
                new TissueESTCountsReader(basenameMicroarray);

        final int tissueCountsMpss = readerMpss.getTissueCountsNumber();
        final int tissueCountsMicroarray =
                readerMicroarray.getTissueCountsNumber();

        ArrayList<String> tissueNames = new ArrayList<String>();
        //ObjectSet<MutableString> commonTranscripts = new ObjectOpenHashSet<MutableString>();
        final ArrayList<MutableString> commonTranscripts;
        final ArrayList<MutableString> mpssTranscripts =
                new ArrayList<MutableString>();
        final ArrayList<MutableString> microarrayTranscripts =
                new ArrayList<MutableString>();


        tissueNames = getCommonTissueNames(readerMpss, readerMicroarray,
                tissueCountsMpss,
                tissueCountsMicroarray);

        final int[] countsMpss = new int[tissueCountsMpss];
        final int[] countsMicroarray = new int[tissueCountsMicroarray];
        globalCountsMpss = readerMpss.getGlobalTissueRepresentationArray();
        globalCountsMicroarray = readerMicroarray.getGlobalTissueRepresentationArray();
        assert globalCountsMpss != null : "global counts cannot be null";

        // select only common tissues/conditions from each of the two sets

        if (queryIds == null) { // sequential access, full scan:

            // will update later
            boolean moreMpss = false;
            boolean moreMicroarray = false;
            final MutableString accessionCodeMpss = new MutableString();
            final MutableString accessionCodeMicroarray = new MutableString();

            final BufferedWriter mpssTranscriptsFile = new BufferedWriter(
                    new FileWriter("c:\\dev\\tinfo-2\\mpssTranscripts.txt"));
            //BufferedWriter microarrayTranscriptsFile = new BufferedWriter(new FileWriter("c:\\dev\\tinfo-2\\microarrayTranscripts.txt"));

            do {
                moreMpss = readerMpss
                        .readNextCounts(accessionCodeMpss, countsMpss);
                mpssTranscripts.add(accessionCodeMpss.copy());
                mpssTranscriptsFile.write(accessionCodeMpss + "\t MPSS" + "\n");
                /*
                System.out.println("MPSS: " + accessionCodeMpss + " size: "
                        + countsMpss.length);
                        */
            } while (moreMpss);

            do {
                moreMicroarray = readerMicroarray.readNextCounts(
                        accessionCodeMicroarray, countsMicroarray);
                int sum = 0;
                for (final int aCountsMicroarray : countsMicroarray) {
                    sum += aCountsMicroarray;
                }
                if (sum == 0) {
                    continue;
                }
                microarrayTranscripts.add(accessionCodeMicroarray.copy());
                //printTissueCounts(accessionCodeMpss, tissueCountsMpss, countsMpss, readerMpss, "MPSS");
                mpssTranscriptsFile.write(
                        accessionCodeMicroarray + "\t microarray" + "\n");
                // commonTranscripts.add(accessionCodeMicroarray.copy());
                /*
                System.out.println("adding: " + accessionCodeMicroarray
                        + " length:" + countsMicroarray[0]);
                        */
            } while (moreMicroarray);
            mpssTranscriptsFile.close();

            commonTranscripts = microarrayTranscripts;
            commonTranscripts.retainAll(mpssTranscripts);

            try {
                final BufferedWriter output = new BufferedWriter(new FileWriter(
                        "c:\\dev\\tinfo-2\\commonTranscripts.txt"));
                final int commonTranscriptsSize = commonTranscripts.size();
                //System.out.println("Size:"+commonTranscriptsSize+"\n");
                int j = 0;
                for (final MutableString transcriptId : commonTranscripts) {
                    transcriptId.write(output);
                    output.write("\n");
                    // analyze
                    //if (j < 250) {
                        final String transcriptIdString =
                                new MutableString(transcriptId).toString();
                        singleTranscriptCompare(transcriptIdString, readerMpss,
                                readerMicroarray,
                                tissueCountsMpss, tissueCountsMicroarray,
                                tissueNames,
                                countsMpss,
                                countsMicroarray);
                    //}
                    j++;
                }
                output.close();

            }
            catch (IOException e) {
                System.err.println("Error writing to commonTranscripts file.");
            }

            // make correlation statistics

        } else {
            // random access for the given query Ids:
            singleTranscriptCompare(queryIds, readerMpss, readerMicroarray,
                    tissueCountsMpss, tissueCountsMicroarray, tissueNames,
                    countsMpss,
                    countsMicroarray);

        }
        // Connect print stream to the output stream
        final BufferedWriter outCorrel = new BufferedWriter(new FileWriter(
                "c:\\dev\\tinfo-2\\analysis\\queryIdsCorrelations.txt"));
        for(final Object key : queryIdsCorrelations.keySet()) {
            final Object value = queryIdsCorrelations.get(key);
            outCorrel.write(key +"\t"+ value+"\n");
        }
        outCorrel.close();
    }

    private void singleTranscriptCompare(final String queryIds,
                                         final TissueESTCountsReader readerMpss,
                                         final TissueESTCountsReader readerMicroarray,
                                         final double tissueCountsMpss,
                                         final double tissueCountsMicroarray,
                                         final ArrayList<String> tissueNames,
                                         final int[] countsMpss,
                                         final int[] countsMicroarray)
            throws IOException {
        final String[] queryIdentifiers = queryIds.split("[,]");

        // relate tissue name with appropriate count
        final HashMap<String, Double> tissueNamesCountsMpss =
                new HashMap<String, Double>();
        final HashMap<String, Double> tissueNamesCountsMicroarray =
                new HashMap<String, Double>();
        final ArrayList<String> tissueNamesMpss = new ArrayList<String>();
        final ArrayList<String> tissueNamesMicroarray = new ArrayList<String>();
        ArrayList<String> tissueNamesCommon = new ArrayList<String>();
        final ArrayList<Double> countsMicroarrayNormalizedAll = new ArrayList<Double>();
        final ArrayList<Double> countsMpssNormalizedAll = new ArrayList<Double>();


        readerMpss.randomAccess();
        readerMicroarray.randomAccess();
        for (final String queryId : queryIdentifiers) {

            //System.out.print("queryId:" + queryId + "\n");
            //System.out.println(java.util.Arrays.toString(countsMpss));
            readerMpss.readCounts(queryId, countsMpss);
            readerMicroarray.readCounts(queryId, countsMicroarray);

            for (int i = 0; i < tissueCountsMpss; i++) {
                if (countsMpss[i] != 0) {
                    tissueNamesMpss.add(readerMpss.getTissueType(i).replaceAll(
                            "tissue:", ""));
                    final double countsMpssNormalized = ((double) countsMpss[i])
                            / ((double) globalCountsMpss[i]);
                    countsMpssNormalizedAll.add(countsMpssNormalized);

                    //System.out.println("..result is" + countsMpssNormalized);
                    tissueNamesCountsMpss.put(tissueNamesMpss.get(
                            tissueNamesMpss.size() - 1), countsMpssNormalized);
                    //System.out.println("Putting into Mpss hash: "+tissueNamesMpss.get(tissueNamesMpss.size()-1)+" : "+countsMpss[i]);
                }
            }
            double countsMicroarrayNormalized;
            for (int i = 0; i < tissueCountsMicroarray; i++) {
                if (countsMicroarray[i] != 0) {
                    tissueNamesMicroarray
                            .add(readerMicroarray.getTissueType(i).replaceAll(
                                    "tissue:", ""));
                    if (normalization.equals("off")) {
                        countsMicroarrayNormalized = (double)countsMicroarray[i];
                    }
                    else {
                        countsMicroarrayNormalized =
                                ((double) countsMicroarray[i])
                                        / ((double) globalCountsMicroarray[i]);
                    }
                    countsMicroarrayNormalizedAll.add(countsMicroarrayNormalized);
                    tissueNamesCountsMicroarray.put(tissueNamesMicroarray.get(
                            tissueNamesMicroarray.size() - 1),
                            countsMicroarrayNormalized);
                }
            }
            tissueNamesCommon = tissueNamesMpss;
            tissueNamesCommon.retainAll(tissueNamesMicroarray);
            if (tissueNamesCommon.size() < 5) {
                continue;
            }
            final double averageMicroarrayCounts = 0.0;
            final int tissueCounts = tissueNamesCommon.size();

            printTissueCountsToFile(queryId, tissueCountsMpss,
                    tissueCountsMicroarray,
                    countsMpss, countsMicroarray, readerMpss,
                    readerMicroarray, tissueNames, tissueNamesCommon,
                    tissueNamesCountsMpss, tissueNamesCountsMicroarray,
                    countsMicroarrayNormalizedAll, countsMpssNormalizedAll);

            // calc correlations
            //if (countsMicroarrayNormalizedAll.size() > 0) queryIdsCorrelations.put(queryId, PearsonCorrel(countsMicroarrayNormalizedAll, countsMpssNormalizedAll));


        }


    }
    public static double PearsonCorrel(final ArrayList<Double> x, final ArrayList<Double> y) {

         // calculate averages
        final int n = x.size();
        double sumx = 0;
        double sumy = 0;

        for (int i=0; i<n; i++) {
            sumx += x.get(i);
            sumy += y.get(i);
        }

        final double xavg = sumx / (double)n;
        final double yavg = sumy / (double)n;
        double sumXXAvgYYAvg = 0;
        double sumXXAvgSq = 0;
        double sumYYAvgSq = 0;

        for (int i=0; i<n; i++) {
            sumXXAvgYYAvg += (x.get(i)-xavg)*(y.get(i)-yavg);
            sumXXAvgSq += (x.get(i)-xavg)*(x.get(i)-xavg);
            sumYYAvgSq += (y.get(i)-yavg)*(y.get(i)-yavg);
        }

        return sumXXAvgYYAvg/(Math.sqrt(sumXXAvgSq*sumYYAvgSq));

    }
    private ArrayList<String> getCommonTissueNames(
            final TissueESTCountsReader readerMpss,
            final TissueESTCountsReader readerMicroarray,
            final int tissueCountsMpss, final int tissueCountsMicroarray
    ) {
        final ArrayList<String> tissueNames = new ArrayList<String>();

        for (int i = 0; i < tissueCountsMpss; i++) {
            final String tissueNameMpss = readerMpss.getTissueType(i);
            for (int j = 0; j < tissueCountsMicroarray; j++) {
                final String tissueNameMicroarray = readerMicroarray.getTissueType(j);
                if (tissueNameMpss.equals(tissueNameMicroarray)) {

                    tissueNames.add(tissueNameMpss.replaceAll("tissue:", ""));
                }
            }
        }
        return tissueNames;
    }

    private static void printTissueCounts(final CharSequence accessionCode,
                                          final int tissueCounts,
                                          final int[] counts,
                                          final TissueESTCountsReader reader,
                                          final String type
    ) {
        //System.out.print("Accession: " + accessionCode);
        for (int i = 0; i < tissueCounts; i++) {
            if (counts[i] != 0) {
                //System.out.print(" [ tissue " + reader.getTissueType(i) + " count: " + counts[i] + " ]");
            }
        }
        //System.out.print('\n');

    }

    private void printTissueCountsToFile(final String queryId,
                                                final Double tissueCountsMpss,
                                                final Double tissueCountsMicroarray,
                                                final int[] countsMpss,
                                                final int[] countsMicroarray,
                                                final TissueESTCountsReader readerMpss,
                                                final TissueESTCountsReader readerMicroarray,
                                                final ArrayList<String> tissueNames,
                                                final ArrayList<String> tissueNamesCommon,
                                                final HashMap<String, Double> tissueNamesCountsMpss,
                                                final HashMap<String, Double> tissueNamesCountsMicroarray,
                                                final ArrayList<Double> countsMicroarrayNormalizedAll,
                                                final ArrayList<Double> countsMpssNormalizedAll

    ) {
        /*try {

            BufferedWriter out = new BufferedWriter(new FileWriter(
                    "c:\\dev\\tinfo-2\\analysis\\comparison-" + queryId + ".txt"));
            // Connect print stream to the output stream
            out.write("tissueType\t"+basenameMpss+"\t"+basenameMicroarray+"\n");   */
            final int tissueCounts = tissueNamesCommon.size();
            final ArrayList<Double> countsMicroarrayNormalizedCommon = new ArrayList<Double>();
            final ArrayList<Double> countsMpssNormalizedCommon = new ArrayList<Double>();
            for (int i = 0; i < tissueCounts; i++) {
                if (countsMpss[i] != 0) {
                    /*
                    System.out.print(tissueNamesCommon.get(i) + "\t"
                            + tissueNamesCountsMpss
                            .get(tissueNamesCommon.get(i))
                            + "\t" + tissueNamesCountsMicroarray
                            .get(tissueNamesCommon.get(i)) + "\n");
                    System.out.println(
                            "Searching for key:" + tissueNamesCommon.get(i));
                    */
                    /*
                    out.write(tissueNamesCommon.get(i) + "\t"
                            + tissueNamesCountsMpss
                            .get(tissueNamesCommon.get(i)) + "\t"
                            + tissueNamesCountsMicroarray
                            .get(tissueNamesCommon.get(i)) + "\n");
                    */

                    countsMicroarrayNormalizedCommon.add(tissueNamesCountsMicroarray
                            .get(tissueNamesCommon.get(i)));
                    countsMpssNormalizedCommon.add(tissueNamesCountsMpss
                            .get(tissueNamesCommon.get(i)));
                }
            }
            //out.close();
            if (countsMpssNormalizedCommon.size() > 6) {
                queryIdsCorrelations.put(queryId, PearsonCorrel(countsMicroarrayNormalizedCommon, countsMpssNormalizedCommon));
            }
        /*}
        catch (IOException e) {
            System.err.println("Error writing to file.");
        } */
    }

}


