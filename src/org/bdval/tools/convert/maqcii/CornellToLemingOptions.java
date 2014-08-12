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

package org.bdval.tools.convert.maqcii;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang.StringUtils;
import org.bdval.tools.convert.OptionsConfigurationException;
import org.bdval.tools.convert.OptionsSupport;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Describe class here.
 *
 * @author Kevin Dorff
 */
public class CornellToLemingOptions {

    /** The input file as a file. */
    private File inputFile;

    /** The output directory as a file. */
    private File outputDirectory;

    /** The cologne sample multimap file. */
    private File cologneSampleMultimapFile;

    /** The date prefix, will go on the filenames. */
    public String datePrefix;

    /** The jsap command line parser. */
    private final JSAP jsap;

    /** Map of cologne sample id's to patient id's. From the cologne sample multimap. */
    private final Map<String, String> cologneSampleToPatientIdMap =
            new Object2ObjectOpenHashMap<String, String>();

    /** Map of cologne sample id's to array01 filenames. From the cologne sample multimap. */
    private final Map<String, String> cologneSampleToArray01FilenameMap =
            new Object2ObjectOpenHashMap<String, String>();

    /** Map of cologne sample id's to array02 filenames. From the cologne sample multimap. */
    private final Map<String, String> cologneSampleToArray02FilenameMap =
            new Object2ObjectOpenHashMap<String, String>();


    /**
     * Convert Cornell format to Leming format.
     * @param args the command line arguments
     * @throws JSAPException jsap error parsing.
     * @throws org.bdval.tools.convert.OptionsConfigurationException error reading the options
     * @throws IOException error reading from a file
     */
    public CornellToLemingOptions(final String[] args)
            throws JSAPException, OptionsConfigurationException, IOException {
        super();
        jsap = new JSAP();
        defineOptions();
        interpretArguments(args);
        validateOptions();
        readCologneSampleMultiMapFile();
        System.out.println("Configured, background data read.");
    }

    /**
     * Define the JSAP options used to parse the command line.
     * @throws JSAPException error adding JSAP options
     */
    private void defineOptions() throws JSAPException {
        final Parameter outputDirectoryParameter =
                new FlaggedOption("output-directory")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('o')
                        .setLongFlag("output-directory")
                        .setHelp("Output directory");
        jsap.registerParameter(outputDirectoryParameter);

        final Parameter inputFileParameter =
                new FlaggedOption("input-file")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('i')
                        .setLongFlag("input-file")
                        .setHelp("Input file");
        jsap.registerParameter(inputFileParameter);

        final Parameter cologneSampleMultiMapFileParameter =
                new FlaggedOption("cologne-sample-multimap-file")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('c')
                        .setLongFlag("cologne-sample-multimap-file")
                        .setHelp("Cologne sample multimap file");
        jsap.registerParameter(cologneSampleMultiMapFileParameter);

        final Parameter datePrefixParameter =
                new FlaggedOption("date-prefix")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('d')
                        .setLongFlag("date-prefix")
                        .setHelp("The date prefix for output filenames");
        jsap.registerParameter(datePrefixParameter);
    }

    /**
     * Parse the actual command line options using JSAP.
     * @param args the command line options
     * @throws OptionsConfigurationException error rarsing the options
     */
    private void interpretArguments(final String[] args) throws OptionsConfigurationException {
        final JSAPResult result = jsap.parse(args);

        if (!result.success()) {
            System.err.println(jsap.getHelp());
            throw new OptionsConfigurationException("Error parsing command line.");
        }

        setOutputDirectory(result.getString("output-directory"));
        setInputFile(result.getString("input-file"));
        setCologneSampleMultimapFile(result.getString("cologne-sample-multimap-file"));
        setDatePrefix(result.getString("date-prefix"));
    }

    /**
     * Validate the options as they were configured.
     *
     * @throws OptionsConfigurationException error validating the options.
     */
    public void validateOptions() throws OptionsConfigurationException {
        OptionsSupport.verifyWritableDirectory(outputDirectory, "output-directory");
        OptionsSupport.verifyReadableFile(inputFile, "input-file");
        OptionsSupport.verifyReadableFile(cologneSampleMultimapFile,
                "cologne-sample-multimap-file");
    }

    /**
     * Set the input file. Also defines inputFileAsFile.
     * @param inputFile the input file
     */
    public void setInputFile(final String inputFile) {
        if (StringUtils.isNotBlank(inputFile)) {
            this.inputFile = new File(inputFile);
        } else {
            this.inputFile = null;
        }
    }

    /**
     * Get the input file as a file.
     * @return the input file as a file
     */
    public File getInputFile() {
        return inputFile;
    }

    /**
     * Set the output directory. Also defines outputDirectoryAsFile.
     * @param outputDirectory the output directory
     */
    public void setOutputDirectory(final String outputDirectory) {
        if (StringUtils.isNotBlank(outputDirectory)) {
            this.outputDirectory = new File(outputDirectory);
        } else {
            this.outputDirectory = null;
        }
    }

    /**
     * Get the output directory as a file.
     * @return the output directory as a file
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Set the cologneSampleMultimapFile.
     * @param cologneSampleMultimapFile the input file
     */
    public void setCologneSampleMultimapFile(final String cologneSampleMultimapFile) {
        if (StringUtils.isNotBlank(cologneSampleMultimapFile)) {
            this.cologneSampleMultimapFile = new File(cologneSampleMultimapFile);
        } else {
            this.cologneSampleMultimapFile = null;
        }
    }

    /**
     * Get the cologneSampleMultimapFile.
     * @return the input file as a file
     */
    public File getCologneSampleMultimapFile() {
        return cologneSampleMultimapFile;
    }

    /**
     * Given the cologneSampleMultiMapFile, read the data into the multiple maps that will
     * be used to resolve the various bits of data.
     * @throws IOException error reading the file
     */
    private void readCologneSampleMultiMapFile() throws IOException {
        int lineNumber = 0;
        final TsvToFromMap cologneMultimapCsv = TsvToFromMapMaqciiFactory.getMapForType(
                TsvToFromMapMaqciiFactory.TsvToFromMapType.COLOGNE_SAMPLE_MULTIMAP);
        for (final String line : new TextFileLineIterator(cologneSampleMultimapFile)) {
            if (lineNumber++ == 0) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            final Map<String, String> data = cologneMultimapCsv.readDataToMap(line);

            final String sampleId = data.get("Column Heading in the Normalized Array Data File");
            cologneSampleToPatientIdMap.put(sampleId, data.get("PatientID"));
            cologneSampleToArray01FilenameMap.put(sampleId, data.get("Array_A01_FileName (S3R5)"));
            cologneSampleToArray02FilenameMap.put(sampleId, data.get("Array_A02_FileName (R3S5)"));
        }
    }

    /**
     * Resolve a given sampleId to a given patientId (for Leming-Cologne files).
     * From the multi-map file.
     * @param sampleId the sampleId
     * @return the patientId
     */
    public String resolveSampleIdToPatientId(final String sampleId) {
        final String id = cologneSampleToPatientIdMap.get(sampleId);
        if (id == null) {
            System.out.println("!! Cologne multimap missing entry for " + sampleId);
        }
        return id;
    }

    /**
     * Resolve a given sampleId to a given Array01Filename (for Leming-Cologne files).
     * From the multi-map file.
     * @param sampleId the sampleId
     * @return the Array01Filename
     */
    public String resolveSampleIdToArray01Filename(final String sampleId) {
        return cologneSampleToArray01FilenameMap.get(sampleId);
    }

    /**
     * Resolve a given sampleId to a given Array02Filename (for Leming-Cologne files).
     * From the multi-map file.
     * @param sampleId the sampleId
     * @return the Array02Filename
     */
    public String resolveSampleIdToArray02Filename(final String sampleId) {
        return cologneSampleToArray02FilenameMap.get(sampleId);
    }

    /**
     * Set the date prefix for written files.
     * @param datePrefix the new datePrefix
     */
    public void setDatePrefix(final String datePrefix) {
        this.datePrefix = datePrefix;
    }

    /**
     * Get the date prefix for written files.
     * @return the datePrefix
     */
    public String getDatePrefix() {
        return this.datePrefix;
    }

    /**
     * Human readable string of how this options object is configured.
     * @return the human readable options
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("inputFile:");
        sb.append(OptionsSupport.filenameFromFile(inputFile));
        sb.append("\noutputDirectory:");
        sb.append(OptionsSupport.filenameFromFile(outputDirectory));
        sb.append("\ncologneSampleMultimapFile:");
        sb.append(OptionsSupport.filenameFromFile(cologneSampleMultimapFile));
        sb.append("\ndatePrefix:");
        sb.append(datePrefix);
        return sb.toString();
    }
}
