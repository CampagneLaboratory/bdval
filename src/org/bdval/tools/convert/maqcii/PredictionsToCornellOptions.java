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
import com.martiansoftware.jsap.Switch;
import edu.cornell.med.icb.iterators.RecursiveFileListIterator;
import org.apache.commons.lang.StringUtils;
import org.bdval.tools.convert.OptionsConfigurationException;
import org.bdval.tools.convert.OptionsSupport;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Options for PredictionsToCornell.
 *
 * @author Kevin Dorff
 */
public class PredictionsToCornellOptions {

    /**
     * The input dir as a file.
     */
    private File inputDirectory;

    /**
     * The output dir as a file.
     */
    private File outputFile;

    /**
     * The sample ids map file as a file.
     */
    private File sampleIdsMapFile;

    /**
     * The sample ids map.
     */
    private final Map<String, String> sampleIdsMap;

    /**
     * The model ids map file as a file.
     */
    private File modelIdsMapFile;

    /**
     * The model ids map.
     */
    private final Map<String, String> modelIdsMap;

    /**
     * The model ids map.
     */
    private boolean omitUnknown;

    /**
     * The jsap command line parser.
     */
    private final JSAP jsap;

    /**
     * Create the options object.
     *
     * @param args the comand line args
     * @throws JSAPException                 jsap error parsing.
     * @throws org.bdval.tools.convert.OptionsConfigurationException error reading the options
     * @throws IOException                   error reading from a file
     */
    public PredictionsToCornellOptions(final String[] args)
            throws JSAPException, OptionsConfigurationException, IOException {
        super();
        jsap = new JSAP();
        defineOptions();
        interpretArguments(args);
        validateOptions();
        sampleIdsMap = OptionsSupport.readMapFileFromTsv(sampleIdsMapFile, true, 1, 0);
        modelIdsMap = OptionsSupport.readMapFileFromTsv(modelIdsMapFile, true, 1, 0);
        System.out.println("Configured, background data read.");
    }

    /**
     * Define command line options
     *
     * @throws com.martiansoftware.jsap.JSAPException
     *          error interpreting command line
     */
    public void defineOptions() throws JSAPException {
        final Parameter inputDirectoryParameter =
                new FlaggedOption("input-directory")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('i')
                        .setLongFlag("input-directory")
                        .setHelp("Input directory");
        jsap.registerParameter(inputDirectoryParameter);

        final Parameter outputFileParameter =
                new FlaggedOption("output-file")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('o')
                        .setLongFlag("output-file")
                        .setHelp("Output file");
        jsap.registerParameter(outputFileParameter);

        final Parameter sampleIdsMapFileParameter =
                new FlaggedOption("sample-ids-map-file")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('s')
                        .setLongFlag("sample-ids-map-file")
                        .setHelp("Sample IDs Map File");
        jsap.registerParameter(sampleIdsMapFileParameter);

        final Parameter modelIdsMapFileParameter =
                new FlaggedOption("model-ids-map-file")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault(JSAP.NO_DEFAULT)
                        .setRequired(true)
                        .setShortFlag('m')
                        .setLongFlag("model-ids-map-file")
                        .setHelp("Model IDs Map File");
        jsap.registerParameter(modelIdsMapFileParameter);

        final Parameter omitUnknownParameter =
                new Switch("omit-unknown", 'u', "omit-unknown")
                .setHelp("Omit unknown");
        jsap.registerParameter(omitUnknownParameter);
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

        setInputDirectory(result.getString("input-directory"));
        setOutputFile(result.getString("output-file"));
        setSampleIdsMapFile(result.getString("sample-ids-map-file"));
        setModelIdsMapFile(result.getString("model-ids-map-file"));
        setOmitUnknown(result.getBoolean("omit-unknown"));
    }

    /**
     * Set the input directory and the inputDirectoryAsFile.
     *
     * @param inputDirectory the input directory
     */
    public void setInputDirectory(final String inputDirectory) {
        if (StringUtils.isNotBlank(inputDirectory)) {
            this.inputDirectory = new File(inputDirectory);
        } else {
            this.inputDirectory = null;
        }
    }

    /**
     * Get the input directory as a file.
     *
     * @return the input directory as a file
     */
    public File getInputDirectory() {
        return inputDirectory;
    }

    /**
     * Get an iterator of all of the input files.
     *
     * @return an iterator of all of the input files
     */
    public Iterable<File> getAllInputFiles() {
        return new RecursiveFileListIterator(inputDirectory);
    }

    /**
     * Set the output file and outputFileAsFile.
     *
     * @param outputFile the output file
     */
    public void setOutputFile(final String outputFile) {
        if (StringUtils.isNotBlank(outputFile)) {
            this.outputFile = new File(outputFile);
        } else {
            this.outputFile = null;
        }
    }

    /**
     * Get the output file as a file.
     *
     * @return the output file as a file
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Set the sample id's map file and the sampleIdsMapFileAsFile.
     *
     * @param sampleIdsMapFile the the sample id's map file
     */
    public void setSampleIdsMapFile(final String sampleIdsMapFile) {
        if (StringUtils.isNotBlank(sampleIdsMapFile)) {
            this.sampleIdsMapFile = new File(sampleIdsMapFile);
        } else {
            this.sampleIdsMapFile = null;
        }
    }

    /**
     * Get the sample id's map file as a file.
     *
     * @return the the sample id's map file as a file
     */
    public File getSampleIdsMapFile() {
        return sampleIdsMapFile;
    }

    /**
     * Resolve a sample id to the alternate form.
     *
     * @param sample id the sample to resolve
     * @return the sample id as the alternate form.
     */
    public String resolveSample(final String sample) {
        final String resolvedSample = sampleIdsMap.get(sample);
        if (resolvedSample == null) {
            return "UNKNOWN";
        } else {
            return resolvedSample;
        }
    }

    /**
     * Set the model id's map file and the modelIdsMapFileAsFile.
     *
     * @param modelIdsMapFile the the sample id's map file
     */
    public void setModelIdsMapFile(final String modelIdsMapFile) {
        if (StringUtils.isNotBlank(modelIdsMapFile)) {
            this.modelIdsMapFile = new File(modelIdsMapFile);
        } else {
            this.modelIdsMapFile = null;
        }
    }

    /**
     * Get the model id's map file as a file.
     *
     * @return the the sample id's map file as a file
     */
    public File getModelIdsMapFile() {
        return modelIdsMapFile;
    }


    /**
     * Set if we should omit unknown models.
     *
     * @param omitUnknown if we should omit unknown models
     */
    public void setOmitUnknown(final boolean omitUnknown) {
        this.omitUnknown = omitUnknown;
    }

    /**
     * Get if we should omit unknown models.
     *
     * @return if we should omit unknown models
     */
    public boolean isOmitUnknown() {
        return omitUnknown;
    }

    /**
     * Resolve a model id to the alternate form.
     *
     * @param model id the model to resolve
     * @return the model id as the alternate form.
     */
    public String resolveModel(final String model) {
        final String resolvedModel = modelIdsMap.get(model);
        if (resolvedModel == null) {
            // System.out.println("!! WARNING Could not resolve model " + model);
            return "UNKNOWN";
            // return model;
        } else {
            return resolvedModel;
        }
    }

    /**
     * Validate the options.
     *
     * @throws OptionsConfigurationException error validating the options.
     */
    public void validateOptions() throws OptionsConfigurationException {
        OptionsSupport.verifyReadableDirectory(inputDirectory, "input-directory");
        OptionsSupport.createNewWritableFile(outputFile, "output-file");
        OptionsSupport.verifyReadableFile(sampleIdsMapFile, "sample-ids-map-file");
        OptionsSupport.verifyReadableFile(modelIdsMapFile, "model-ids-map-file");
    }

    /**
     * Human readable string of how this options object is configured.
     * @return the human readable options
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("inputDirectory:");
        sb.append(OptionsSupport.filenameFromFile(inputDirectory));
        sb.append("\noutputFile:");
        sb.append(OptionsSupport.filenameFromFile(outputFile));
        sb.append("\nsampleIdsMapFile:");
        sb.append(OptionsSupport.filenameFromFile(sampleIdsMapFile));
        sb.append("\nmodelIdsMapFile:");
        sb.append(OptionsSupport.filenameFromFile(modelIdsMapFile));
        sb.append("\nomitUnknown:");
        sb.append(omitUnknown);
        return sb.toString();
    }
}
