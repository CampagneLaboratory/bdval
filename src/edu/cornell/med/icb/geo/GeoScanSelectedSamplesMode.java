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

package edu.cornell.med.icb.geo;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.LineIterator;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Scan a Gene Expression Omnibus file in Soft format and process the content. Note that this class determines if the
 * Soft file has a Series section by looking at the filename (filenames that start with GSE are assumed to have a Series
 * section). This is done for efficiency, so that we scan the GEO file only once (determining that a file has no Series
 * section by scanning the file would require scanning past the end of the file unless a Series section is found).
 *
 * @author Fabien Campagne Date: Aug 16, 2007 Time: 2:25:57 PM
 */
public class GeoScanSelectedSamplesMode extends GeoScanMode {
    private String transcriptProbesetFilename;

    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
        super.defineOptions(jsap);
        if (jsap.getByID("sample-ids") == null) {
            final String sampleIdFilename = "sample-ids";

            final FlaggedOption sampleIdFilenameFlag =
                    new FlaggedOption(sampleIdFilename)
                            .setStringParser(JSAP.STRING_PARSER)
                            .setDefault(JSAP.NO_DEFAULT)
                            .setRequired(false)
                            .setShortFlag('s')
                            .setLongFlag("sample-ids");
            sampleIdFilenameFlag.setHelp(
                    "Identifiers of sample to scan. Name of a file with one sample id per line.");
            jsap.registerParameter(sampleIdFilenameFlag);
        }
        if (jsap.getByID("transcript-probeset") == null) {
            final String transcriptProbesetFlagName = "transcript-probeset";

            final FlaggedOption transcriptProbesetFlag =
                    new FlaggedOption(transcriptProbesetFlagName)
                            .setStringParser(JSAP.STRING_PARSER)
                            .setDefault(JSAP.NO_DEFAULT)
                            .setRequired(false)
                            .setShortFlag('p')
                            .setLongFlag("transcript-probeset");
            transcriptProbesetFlag.setHelp(
                    "File with transcript/probeset relationships, one per line.");
            jsap.registerParameter(transcriptProbesetFlag);
        }
        if (jsap.getByID("adapter-options") == null) {
            final String adapterOptionsFlag = "adapter-options";

            final FlaggedOption adapterOption =
                    new FlaggedOption(adapterOptionsFlag)
                            .setStringParser(JSAP.STRING_PARSER)
                            .setDefault(JSAP.NO_DEFAULT)
                            .setRequired(false)
                            .setShortFlag('a')
                            .setLongFlag("adapter-options");
            adapterOption.setHelp(
                    "Adapter options. A set of options that will be passed to the format adapter.");
            jsap.registerParameter(adapterOption);
        }
    }

    @Override
    public void interpretArguments(final JSAP jsap, final JSAPResult result,
                                   final GeoScanOptions options) {
        super.interpretArguments(jsap, result, options);
        final String sampleIdFilename = result.getString("sample-ids");
        if (sampleIdFilename != null) {
            sampleIdSelection = readSampleIds(sampleIdFilename);
        }
        transcriptProbesetFilename = result.getString("transcript-probeset");
        options.adapterOptions = result.getString("adapter-options");

    }

    private TranscriptProbesetRelationship readRelationships(
            final GEOPlatformIndexed platform, final String filename) {
        try {
            final TranscriptProbesetRelationship tpr =
                    new TranscriptProbesetRelationship();
            final LineIterator it = new LineIterator(
                    new FastBufferedReader(new FileReader(filename)));
            while (it.hasNext()) {
                final MutableString line = it.next();
                if (line.startsWith("#")) {
                    continue;
                }
                final String[] tokens = line.toString().split("[\t]");
                if (tokens.length == 2) {
                    final String transcriptId = tokens[0];
                    final String probesetId = tokens[1];
                    if (probesetId.length() > 0) {
                        // skip lines where only transcript id
                        tpr.addRelationship(
                                new MutableString(transcriptId).compact(),
                                platform.getProbeIds().getInt(new MutableString(
                                        probesetId).compact()));
                    }
                }

            }
            return tpr;
        } catch (FileNotFoundException e) {
            System.err.println("Cannot read transcript probeset relationships "
                    + filename);
            System.exit(10);
        }
        return null;
    }

    public static ObjectArrayList<MutableString> readSampleIds(
            final String sampleIdFilename) {
        final ObjectArrayList<MutableString> list =
                new ObjectArrayList<MutableString>();
        try {
            final LineIterator it = new LineIterator(
                    new FastBufferedReader(new FileReader(sampleIdFilename)));
            while (it.hasNext()) {
                final MutableString sampleIdSelected = it.next();
                if (sampleIdSelected.startsWith("#")) {
                    continue;
                }
                list.add(sampleIdSelected.copy().compact());
            }
            return list;
        } catch (FileNotFoundException e) {
            System.err.println(
                    "Cannot open sample id filename " + sampleIdFilename);
            System.exit(1);
        }
        return null;
    }

    ObjectList<MutableString> sampleIdSelection;
    int sampleScanned;

    @Override
    public void process(final GeoScanOptions options) {
        try {
            final GeoSoftFamilyParser parser =
                    new GeoSoftFamilyParser(options.softFilename);
            if (parser.skipToDatabaseSection()) {
                final MutableString databaseName = parser.getSectionAttribute();
                System.out.println("Database: " + databaseName);
            } else {
                System.out.println("No database section found.");
            }
            boolean hasSeriesSection = false;
            // assume the file has a Series section if the filename starts with GSE..
            final String softFilename = options.softFilename;
            final File file = new File(softFilename);
            final String filename = file.getCanonicalFile().getName();
            hasSeriesSection = filename.startsWith("GSE");
            System.out.println("filename: " + filename);
            System.out.println("hasSeriesSection: " + hasSeriesSection);

            MutableString seriesName = null;

            if (hasSeriesSection && parser.skipToSeriesSection()) {
                seriesName = parser.getSectionAttribute();
                System.out.println("Series: " + seriesName);

                updateSampleSelection(parser);
            }
            GEOPlatformIndexed platform = null;
            if (parser.skipToPlatformSection()) {
                final MutableString platformName = parser.getSectionAttribute();
                System.out.println("Platform: " + platformName);

                updateSampleSelection(parser);
                platform = parser.parsePlatform();
                //set the name of the series in priority if the current file contains only a series.
                platform.setName(
                        seriesName != null ? seriesName : platformName);

            } else {
                System.out.println("No Platform section found.");
                System.exit(10);
            }

            if (transcriptProbesetFilename != null) {
                options.tpr =
                        readRelationships(platform, transcriptProbesetFilename);
            }
            final ProgressLogger progressLogger = new ProgressLogger();
            progressLogger.expectedUpdates = sampleIdSelection.size();
            System.out.println("Processing with " + options.formatAdapter
                    .getClass().getName());

            progressLogger.start("Scanning samples");
            options.formatAdapter.setOptions(options);
            options.formatAdapter.preSeries(platform);
            final int numProcessed = 0;
            while (parser.skipToSampleSection()) {
                final MutableString sampleName = parser.getSectionAttribute();


                if (sampleIdSelection.contains(sampleName.compact())) {
                    System.out.print("Sample " + sampleName + " ");
                    final SampleDataCallback callback =
                            options.formatAdapter.getCallback(platform);
                    parser.parseSampleData(platform, callback);
                    options.formatAdapter
                            .analyzeSampleData(platform, callback, sampleName);
                    sampleScanned++;
                    progressLogger.update();
                    if (sampleScanned == sampleIdSelection.size()) {
                        break;
                    }
                } else {
                    System.out.println("Skipping " + sampleName);
                }
            }

            options.formatAdapter.postSeries(platform, sampleIdSelection);
            progressLogger.stop("Finished scanning samples.");
        } catch (IOException e) {
            System.err.println("An error occurred reading soft file " + options
                    .softFilename);
            e.printStackTrace();
            System.exit(10);

        }
    }

    private void updateSampleSelection(final GeoSoftFamilyParser parser) {
        if (sampleIdSelection == null) {
            // set selection to include all sample ids listed in platform section.
            final SectionProperties props = parser.parseSectionProperties();
            sampleIdSelection = new ObjectArrayList<MutableString>();
            sampleIdSelection.addAll(props.getCollection("sample_id"));
            int counter = 0;
            for (final MutableString sampleId : sampleIdSelection) {
                System.out.println("sample: " + sampleId);
                if (counter++ > 10) { // show only the first 10 samples IDs,

                    System.out.println("...(other sample ids not shown)");
                    break;
                }
            }
            System.out.println("A total of "+sampleIdSelection.size() + " samples will be processed.");
        }
    }
}
