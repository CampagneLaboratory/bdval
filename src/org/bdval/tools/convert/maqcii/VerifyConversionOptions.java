/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

package edu.cornell.med.icb.biomarkers.tools.convert.maqcii;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.biomarkers.tools.convert.OptionsConfigurationException;
import edu.cornell.med.icb.biomarkers.tools.convert.OptionsSupport;
import edu.cornell.med.icb.iterators.RecursiveFileListIterator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Describe class here.
 *
 * @author Kevin Dorff
 */
public class VerifyConversionOptions {

    /**
     * The input dir as a file.
     */
    private File predictionsDirectory;

    /**
     * The input dir as a file.
     */
    private File validationsDirectory;

    /**
     * The jsap command line parser.
     */
    private final JSAP jsap;

    /**
     * Create the options object.
     *
     * @param args the comand line args
     * @throws com.martiansoftware.jsap.JSAPException                 jsap error parsing.
     * @throws edu.cornell.med.icb.biomarkers.tools.convert.OptionsConfigurationException error reading the options
     * @throws java.io.IOException                   error reading from a file
     */
    public VerifyConversionOptions(final String[] args)
            throws JSAPException, OptionsConfigurationException, IOException {
        super();
        jsap = new JSAP();
        defineOptions();
        interpretArguments(args);
        validateOptions();
        System.out.println("Configured, background data read.");
    }

    /**
     * Define command line options
     *
     * @throws com.martiansoftware.jsap.JSAPException
     *          error interpreting command line
     */
    public void defineOptions() throws JSAPException {
        final Parameter predictionsDirectoryParameter =
                new FlaggedOption("predictions-directory")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('p')
                        .setLongFlag("predictions-directory")
                        .setHelp("Predictions directory");
        jsap.registerParameter(predictionsDirectoryParameter);

        final Parameter validationsDirectoryParameter =
                new FlaggedOption("validations-directory")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('v')
                        .setLongFlag("validations-directory")
                        .setHelp("Validations directory");
        jsap.registerParameter(validationsDirectoryParameter);
    }

    /**
     * Parse the command line options.
     *
     * @param args the command line arguments
     * @throws OptionsConfigurationException error interpreting configuration
     */
    public void interpretArguments(final String[] args) throws OptionsConfigurationException {
        final JSAPResult result = jsap.parse(args);

        if (!result.success()) {
            System.err.println(jsap.getHelp());
            throw new OptionsConfigurationException("Error parsing command line.");
        }

        setPredictionsDirectory(result.getString("predictions-directory"));
        setValidationsDirectory(result.getString("validations-directory"));
    }

    /**
     * Validate the options.
     *
     * @throws OptionsConfigurationException error validating the options.
     */
    public void validateOptions() throws OptionsConfigurationException {
        OptionsSupport.verifyReadableDirectory(predictionsDirectory, "predictions-directory");
        OptionsSupport.verifyReadableDirectory(validationsDirectory, "validations-directory");
    }

    /**
     * Set the predictionsDirectory .
     *
     * @param predictionsDirectory the input directory
     */
    public void setPredictionsDirectory(final String predictionsDirectory) {
        if (StringUtils.isNotBlank(predictionsDirectory)) {
            this.predictionsDirectory = new File(predictionsDirectory);
        } else {
            this.predictionsDirectory = null;
        }
    }

    /**
     * Get the predictionsDirectory.
     *
     * @return the predictionsDirectory
     */
    public File getPredictionsDirectory() {
        return predictionsDirectory;
    }

    /**
     * Get an iterator of all of the input files.
     *
     * @return an iterator of all of the input files
     */
    public Iterable<File> getAllPredictionsFiles() {
        return new RecursiveFileListIterator(predictionsDirectory);
    }

    /**
     * Set the validationsDirectory.
     *
     * @param validationsDirectory the validationsDirectory
     */
    public void setValidationsDirectory(final String validationsDirectory) {
        if (StringUtils.isNotBlank(validationsDirectory)) {
            this.validationsDirectory = new File(validationsDirectory);
        } else {
            this.validationsDirectory = null;
        }
    }

    /**
     * Get the validationsDirectory.
     *
     * @return the validationsDirectory
     */
    public File getValidationsDirectory() {
        return validationsDirectory;
    }

    /**
     * Get an iterator of all of the input files.
     *
     * @return an iterator of all of the input files
     */
    public List<File> getAllLemingFiles() {
        final List<File> results = new LinkedList<File>();
        for (final File file : new RecursiveFileListIterator(validationsDirectory)) {
            final String filename = OptionsSupport.filenameFromFile(file);
            final String ext = FilenameUtils.getExtension(filename).toLowerCase();
            final String base = FilenameUtils.getBaseName(filename);
            if (!ext.equals("txt")) {
                continue;
            }
            if (!base.contains("-validation-predictions-template-format-cornell-")) {
                continue;
            }
            results.add(file);
        }
        return results;
    }

    public File getCornellFile() {
        for (final File file : new RecursiveFileListIterator(validationsDirectory)) {
            final String filename = OptionsSupport.filenameFromFile(file);
            final String ext = FilenameUtils.getExtension(filename).toLowerCase();
            final String base = FilenameUtils.getBaseName(filename);
            if (!ext.equals("txt")) {
                continue;
            }
            if (!base.contains("-validation-predictions-cornell-format-all-endpoints")) {
                continue;
            }
            return file;
        }
        return null;
    }

    /**
     * Human readable string of how this options object is configured.
     * @return the human readable options
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("predictionsDirectory:");
        sb.append(OptionsSupport.filenameFromFile(predictionsDirectory));
        sb.append("\nvalidationsDirectory:");
        sb.append(OptionsSupport.filenameFromFile(validationsDirectory));
        return sb.toString();
    }
}