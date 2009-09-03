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

package org.bdval;

import edu.cornell.med.icb.geo.GEOPlatformIndexed;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.FixedGeneList;
import edu.cornell.med.icb.learning.ClassificationHelper;
import edu.cornell.med.icb.learning.ClassificationModel;
import edu.cornell.med.icb.learning.ClassificationProblem;
import edu.cornell.med.icb.learning.FeatureScaler;
import edu.cornell.med.icb.learning.LoadClassificationProblem;
import edu.cornell.med.icb.util.VersionUtils;
import edu.mssm.crover.tables.ColumnTypeException;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Encapsulates the details of a BDVal model.
 *
 * @author Fabien Campagne
 *         Date: May 13, 2008
 *         Time: 4:47:06 PM
 */
public class BDVModel {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(BDVModel.class);
    private final String[] symbolicClassLabel = new String[2];
    private ClassificationHelper helper;
    private FixedGeneList geneList;
    private ConsensusBDVModel delegate;
    protected GEOPlatformIndexed trainingPlatform;
    protected ClassificationProblem modelSpecificProblem;
    protected int splitId;
    protected String splitType;
    protected Table splitSpecificTestSet;
    protected String datasetName;
    protected Object2DoubleMap<MutableString> probesetScaleMeanMap;
    protected Object2DoubleMap<MutableString> probesetScaleRangeMap;

    /**
     * The common base prefix for all BDVModel files.
     */
    protected final String modelFilenamePrefix;
    /**
     * The name of the properties file associated with this BDVModel.
     */
    protected final String modelPropertiesFilename;
    /**
     * The name of the actual model file associated with this DBVModel.
     */
    protected final String modelFilename;
    /**
     * The name of the probeset scale mean map file associated with this BDVModel.
     */
    private final String meansMapFilename;
    /**
     * The name of the probeset range mean map file associated with this BDVModel.
     */
    private final String rangeMapFilename;
    /**
     * The name of the training platform file associated with this BDVModel.
     */
    protected final String platformFilename;
    /**
     * The name of the zip file that contains this BDVModel.
     */
    protected final String zipFilename;

    /**
     * Properties associated with this model.
     */
    protected final Properties properties = new Properties();

    /**
     * Properties for this model.
     * @return Properties recorded when this model was trained and written to file.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Get the common base prefix for all BDVModel files.
     * @return The prefix for the model
     */
    public String getModelFilenamePrefix() {
        return modelFilenamePrefix;
    }

    /**
     * Format types supported.
     */
    protected static enum Format {
        /**
         * Store as {@link java.util.Properties}.
         */
        PROPERTIES,
        /**
         * Store as binary as in {@link java.io.Serializable}.
         */
        BINARY
    }

    /**
     * The format for the BDVModel files.
     */
    protected final Format format;

    /**
     * Default format to use when storing unless specified explicitly.
     */
    protected static final Format DEFAULT_STORAGE_FORMAT = Format.PROPERTIES;

    /**
     * Indicates that the BDVModel files should use the new format (zipped into a single file).
     */
    protected final boolean zipModel;

    /**
     * Various file extensions used for storing models.
     */
    protected enum ModelFileExtension {
        /**
         * The model itself (i.e., libsvm, weka).
         */
        model,
        /**
         * The {@link edu.cornell.med.icb.geo.GEOPlatformIndexed} training platform.
         */
        platform,
        /**
         * The probeset scale ranges.
         */
        ranges,
        /**
         * The probeset scale means.
         */
        means,
        /**
         * Properties associated with this model.
         */
        properties,
        /**
         * Properties associated with this model (old format).
         */
        props
    }

    /**
     * Create a new BDVModel with the specified prefix name.
     *
     * @param modelPrefix Prefix to use for all files associated with this BDVModel
     */
    public BDVModel(final String modelPrefix) {
        this(modelPrefix, DEFAULT_STORAGE_FORMAT);
    }

    /**
     * Create a new BDVModel with the specified prefix name.
     *
     * @param modelPrefix Prefix to use for all files associated with this BDVModel
     * @param format The format to store the files in
     */
    public BDVModel(final String modelPrefix, final Format format) {
        super();
        this.format = format;
        zipModel = format != Format.BINARY;

        final String prefix = removeSuffix(modelPrefix, "." + ModelFileExtension.model.toString());
        modelFilenamePrefix = removeSuffix(prefix, ".zip");

        zipFilename = modelFilenamePrefix + ".zip";
        modelFilename = modelFilenamePrefix + "." + ModelFileExtension.model.toString();

        // The platform "filename" for zip files is actually just the prefix
        platformFilename = modelFilenamePrefix + "." + ModelFileExtension.platform.toString();

        if (zipModel) {
            modelPropertiesFilename =
                    modelFilenamePrefix + "." + ModelFileExtension.properties.toString();
            meansMapFilename = modelFilenamePrefix + "." + ModelFileExtension.means.toString()
                    + "." + ModelFileExtension.properties.toString();
            rangeMapFilename = modelFilenamePrefix + "." + ModelFileExtension.ranges.toString()
                    + "." + ModelFileExtension.properties.toString();
        } else {
            modelPropertiesFilename =
                    modelFilenamePrefix + "." + ModelFileExtension.props.toString();
            meansMapFilename = modelFilenamePrefix + "." + ModelFileExtension.means.toString();
            rangeMapFilename = modelFilenamePrefix + "." + ModelFileExtension.ranges.toString();
        }
    }

    /**
     * Create a new BDVModel with the specified prefix name.
     *
     * @param modelPrefix Prefix to use for all files associated with this BDVModel
     */
    public BDVModel(final String modelPrefix, final ClassificationHelper helper, final Format format) {
        this(modelPrefix, format);
        this.helper = helper;
    }

    public double getTrainingSetMeanValue(final String featureId) {
        return probesetScaleMeanMap.getDouble(new MutableString(featureId));
    }

    public double getTrainingSetRangeValue(final String featureId) {
        return probesetScaleRangeMap.getDouble(new MutableString(featureId));
    }

    /**
     * Remove the suffix from a filename only if it matches the given string.
     * @param filename The filename to remove the suffix from
     * @param suffix The suffix to remove
     * @return The modified filename or the original if it didn't end with the suffix
     */
    public static String removeSuffix(final String filename, final String suffix) {
        final String newFilename;
        if (filename.endsWith(suffix)) {
            // If the --model includes ".model" at the end, remove the suffix
            newFilename = filename.substring(0,
                    filename.length() - suffix.length());
        } else {
            newFilename = filename;
        }
        return newFilename;
    }

    public int getNumberOfFeatures() {
        return getGeneList().getNumberOfProbesets();
    }

    /**
     * Loads a BDVal model from disk. BDVal models are generated with the
     * {@link org.bdval.DiscoverAndValidate} tools (BDVal).
     * @param options specific options to use when loading the model
     * @throws IOException if there is a problem accessing the model
     * @throws ClassNotFoundException if the type of the model is not recognized
     */
    public void load(final DAVOptions options) throws IOException, ClassNotFoundException {
        final boolean zipExists = new File(zipFilename).exists();
        if (LOG.isDebugEnabled()) {
            LOG.debug("model zip file exists: " + BooleanUtils.toStringYesNo(zipExists));
        }
        properties.clear();

        // check to see if a zip file exists - if it doesn't we assume it's an old binary format
        if (zipModel && zipExists) {
            LOG.info("Reading model from filename: " + zipFilename);

            final ZipFile zipFile = new ZipFile(zipFilename);
            try {
                final ZipEntry propertyEntry = zipFile.getEntry(FilenameUtils.getName(modelPropertiesFilename));
                // load properties
                properties.clear();
                properties.addAll(loadProperties(zipFile.getInputStream(propertyEntry), options));

                // the platform is more than one entry in the zip, so here we pass the whole zip
                trainingPlatform = options.trainingPlatform = loadPlatform(zipFile);

                if (isConsensusModel()) {
                    int index = 0;
                    final ObjectList<String> modelJurorFilePrefixes = new ObjectArrayList<String>();
                    String nextFilename;
                    while ((nextFilename = (String) properties.getProperty("bdval.consensus.model." + Integer.toString(index))) != null) {
                        modelJurorFilePrefixes.add(nextFilename);
                        index++;
                    }

                    delegate = new ConsensusBDVModel(modelFilenamePrefix,
                            modelJurorFilePrefixes.toArray(new String[modelJurorFilePrefixes.size()]));
                    delegate.load(options);
                    setGeneList(convertTrainingPlatformToGeneList(options));
                    return;
                } else {
                    probesetScaleMeanMap = options.probesetScaleMeanMap =
                            loadMeansMap(zipFile.getInputStream(zipFile.getEntry(FilenameUtils.getName(meansMapFilename))));
                    probesetScaleRangeMap = options.probesetScaleRangeMap =
                            loadRangeMap(zipFile.getInputStream(zipFile.getEntry(FilenameUtils.getName(rangeMapFilename))));
                    setGeneList(convertTrainingPlatformToGeneList(options));
                }

                final String modelParameters = properties.getString("training.classifier.parameters");

                LOG.info("Loading model " + modelFilename);
                final InputStream modelStream = zipFile.getInputStream(zipFile.getEntry(FilenameUtils.getName(modelFilename)));
                helper = ClassificationModel.load(modelStream, modelParameters);
                LOG.info("Model loaded.");

                options.classiferClass = helper.classifier.getClass();
                // we don't have a way to inspect the saved model for parameters used during training:
                options.classifierParameters = ClassificationModel.splitModelParameters(modelParameters);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException e) { // NOPMD
                    // ignore since there is not much we can do anyway
                }
            }
        } else {
            final File propertyFile =
                    new File(modelFilenamePrefix + "." + ModelFileExtension.props.toString());
            LOG.debug("Loading properties from " + propertyFile.getAbsolutePath());
            final Properties properties =
                    loadProperties(FileUtils.openInputStream(propertyFile), options);

            trainingPlatform = options.trainingPlatform =
                    (GEOPlatformIndexed) BinIO.loadObject(platformFilename);

            if (isConsensusModel()) {
                int index = 0;
                final ObjectList<String> modelJurorFilePrefixes = new ObjectArrayList<String>();
                String nextFilename = null;
                while ((nextFilename = (String) properties.getProperty("bdval.consensus.model." + Integer.toString(index))) != null) {
                    modelJurorFilePrefixes.add(nextFilename);
                    index++;
                }

                delegate = new ConsensusBDVModel(modelFilenamePrefix,
                        modelJurorFilePrefixes.toArray(new String[modelJurorFilePrefixes.size()]));
                delegate.load(options);
                setGeneList(convertTrainingPlatformToGeneList(options));
                return;
            } else {
                probesetScaleMeanMap = options.probesetScaleMeanMap =
                        (Object2DoubleMap<MutableString>) BinIO.loadObject(modelFilenamePrefix + ".means");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Number of entries in means map = " + probesetScaleMeanMap.size());
                }
                probesetScaleRangeMap = options.probesetScaleRangeMap =
                        (Object2DoubleMap<MutableString>) BinIO.loadObject(modelFilenamePrefix + ".ranges");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Number of entries in range map = " + probesetScaleRangeMap.size());
                }
                setGeneList(convertTrainingPlatformToGeneList(options));
            }

            final String modelParameters = properties.getString("training.classifier.parameters");

            LOG.info("Loading model " + modelFilename);
            helper = ClassificationModel.load(modelFilename, modelParameters);
            LOG.info("Model loaded.");

            options.classiferClass = helper.classifier.getClass();
            // we don't have a way to inspect the saved model for parameters used during training:
            options.classifierParameters = ClassificationModel.splitModelParameters(modelParameters);
        }
    }

    /**
     * Load the BDVModel training platform from the specified zip file.
     *
     * @param zipFile The file to read the platform from
     * @return A populated platform object
     * @throws IOException if there is a problem reading from the file
     */
    private GEOPlatformIndexed loadPlatform(final ZipFile zipFile) throws IOException {
        final String platformEntryName = FilenameUtils.getName(platformFilename);
        final Map<String, java.util.Properties> propertyMap =
                new HashMap<String, java.util.Properties>();
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            final String entryName = entry.getName();
            if (entryName.startsWith(platformEntryName)) {
                // we have a platform entry
                final String propertyName =
                        StringUtils.substringBetween(entryName, platformEntryName + ".",
                                "." + ModelFileExtension.properties.toString());
                final java.util.Properties properties = new java.util.Properties();
                properties.load(zipFile.getInputStream(entry));
                propertyMap.put(propertyName, properties);
            }
        }

        return new GEOPlatformIndexed(propertyMap);
    }

    /**
     * Load the BDVModel scale mean from the specified input stream.
     *
     * @param stream The stream to read the map from
     * @return A populated map
     * @throws IOException            if there is a reading from the stream
     * @throws ClassNotFoundException if the stream does not contain a map
     */
    private Object2DoubleMap<MutableString> loadMeansMap(final InputStream stream)
            throws IOException, ClassNotFoundException {
        Object2DoubleMap<MutableString> map = null;
        switch (format) {
            case BINARY:
                // !!! WARNING !!!
                // there may be a problem with fastutil 5.0.9 and loading from streams
                // it doesn't load the map completely, loading from a filename gets it all
                // !!! WARNING !!!
                map = (Object2DoubleMap<MutableString>) BinIO.loadObject(stream);
                break;
            case PROPERTIES:
                map = loadPropertiesFromMap(stream);
                break;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of entries in means map = " + map.size());
        }
        return map;
    }

    /**
     * Load the BDVModel scale mean from the specified input stream.
     *
     * @param stream The stream to read the map from
     * @return A populated map
     * @throws IOException            if there is a reading from the stream
     * @throws ClassNotFoundException if the stream does not contain a map
     */
    private Object2DoubleMap<MutableString> loadRangeMap(final InputStream stream)
            throws IOException, ClassNotFoundException {
        Object2DoubleMap<MutableString> map = null;
        switch (format) {
            case BINARY:
                // !!! WARNING !!!
                // there may be a problem with fastutil 5.0.9 and loading from streams
                // it doesn't load the map completely, loading from a filename gets it all
                // !!! WARNING !!!
                map = (Object2DoubleMap<MutableString>) BinIO.loadObject(stream);
                break;
            case PROPERTIES:
                map = loadPropertiesFromMap(stream);
                break;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of entries in range map = " + map.size());
        }
        return map;
    }

    /**
     * Loads the key/value pairs from an output stream and returns a map.
     *
     * @param stream The stream to read the map to
     * @return A map of string keys to doubles.
     * @throws IOException if there is a problem writing to the stream
     */
    private Object2DoubleMap<MutableString> loadPropertiesFromMap(final InputStream stream)
            throws IOException {
        final Object2DoubleMap<MutableString> map = new Object2DoubleOpenHashMap<MutableString>();
        final java.util.Properties properties = new java.util.Properties();
        properties.load(stream);
        for (final Map.Entry entry : properties.entrySet()) {
            map.put(new MutableString(entry.getKey().toString()),
                    NumberUtils.toDouble(entry.getValue().toString()));
        }
        return map;
    }

    /**
     * Load the BDVModel properties from the specified input stream.
     *
     * @param stream  The stream to read the properties from
     * @param options The options associated with this model
     * @return a populated properties object
     */
    protected Properties loadProperties(final InputStream stream, final DAVOptions options) {
        final Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (ConfigurationException e) {
            LOG.fatal("Cannot load model properties with filename " + modelPropertiesFilename, e);
            System.exit(10);
        }
        if (properties.containsKey("trained.from.split.split-id")) {
            splitId = properties.getInt("trained.from.split.split-id");
        }
        if (properties.containsKey("trained.from.split-type")) {
            splitType = properties.getString("trained.from.split-type");
        }
        if (properties.containsKey("trained.from.dataset")) {
            datasetName = properties.getString("trained.from.dataset");
        }

        // scaling and normalization options must match exactly the options used to train
        // the model. We restore these options from model properties:
        options.percentileScaling = properties.getBoolean("scaling.use.percentiles");
        options.scalerClassName = properties.getString("scaling.scaler.classname");
        options.scaleFeatures = properties.getBoolean("scaling.enabled");
        options.normalizeFeatures = properties.getBoolean("feature-normalization.enabled");
        options.scalerClass = getClass(properties);

        getSymbolicClassLabel()[0] = properties.getString("training.class0.label");
        getSymbolicClassLabel()[1] = properties.getString("training.class1.label");

        return properties;
    }

    protected FixedGeneList convertTrainingPlatformToGeneList(final DAVOptions options) {
        final FixedGeneList fixedGeneList;
        if (delegate != null) {
            fixedGeneList = delegate.convertTrainingPlatformToGeneList(options);
        } else {
            final ObjectSet<String> probeids = new ObjectOpenHashSet<String>();
            for (int probeIndex = 0; probeIndex < options.trainingPlatform.getNumProbeIds(); probeIndex++) {
                probeids.add(options.trainingPlatform.getProbesetIdentifier(probeIndex).toString());
            }
            fixedGeneList = new FixedGeneList(probeids.toArray(new String[probeids.size()]));
        }
        return fixedGeneList;
    }

    private Class<? extends FeatureScaler> getClass(final Properties modelProps) {
        try {
            return (Class<? extends FeatureScaler>) Class.forName(modelProps.getString("scaling.implementation.classname"));
        } catch (ClassNotFoundException e) {
            LOG.error("Class not found", e);
            return null;
        }
    }

    public String[] getSymbolicClassLabel() {
        return symbolicClassLabel;
    }

    public ClassificationHelper getHelper() {
        return helper;
    }

    public void setHelper(final ClassificationHelper helper) {
        this.helper = helper;
    }

    /**
     * Returns a gene list corresponding to the features of the model.
     *
     * @return A fixed gene list for this model.
     */
    public FixedGeneList getGeneList() {
        return geneList;
    }

    protected void setGeneList(final FixedGeneList geneList) {
        this.geneList = geneList;
    }

    protected Table loadTestSet(final DAVMode mode, final DAVOptions options,
                                final FixedGeneList geneList,
                                final List<Set<String>> labelValueGroups,
                                final ObjectSet<String> testSampleIds)
            throws TypeMismatchException, InvalidColumnException, ColumnTypeException,
            IOException, ClassNotFoundException {
        if (delegate != null) {
            return delegate.loadTestSet(mode, options, geneList, labelValueGroups, testSampleIds);
        } else {
            Table processedTable = mode.processTable(geneList, options.inputTable,
                    options, labelValueGroups, true);

            mode.scaleFeatures(options, true, processedTable);
            // reload the platform. We changed it in processTable.
            options.trainingPlatform = loadPlatform();

            if (testSampleIds != null) {
                // focus on a subset of samples in the input table: those in test-samples
                processedTable = mode.filterSamples(processedTable, testSampleIds);
            }
            splitSpecificTestSet = processedTable;
            return processedTable;
        }
    }

    private GEOPlatformIndexed loadPlatform() throws ClassNotFoundException, IOException {
        final GEOPlatformIndexed platform;
        final boolean zipExists = new File(zipFilename).exists();
        if (LOG.isDebugEnabled()) {
            LOG.debug("model zip file exists: " + BooleanUtils.toStringYesNo(zipExists));
        }
        if (zipModel && zipExists) {
            platform = loadPlatform(new ZipFile(zipFilename));
        } else {
            platform = (GEOPlatformIndexed) BinIO.loadObject(platformFilename);
        }
        return platform;
    }

    /**
     * Predict the class label of a sample.
     *
     * @param problem       Problem to which the sample belongs.
     * @param sampleIndex   Index of the sample to predict.
     * @param probabilities Array where estimated probabilities will be written.
     * @return predicted class label.
     */
    public double predict(final ClassificationProblem problem, final int sampleIndex,
                          final double[] probabilities) {
        return getHelper().classifier.predict(getHelper().model, problem, sampleIndex, probabilities);
    }

    public void setModelSpecificProblem(final ClassificationProblem modelSpecificProblem) {
        this.modelSpecificProblem = modelSpecificProblem;
    }

    public double predict(final int sampleIndex, final double[] probabilities) {
        if (delegate != null) {
            return delegate.predict(sampleIndex, probabilities);
        } else {
            return getHelper().classifier.predict(getHelper().model, modelSpecificProblem, sampleIndex, probabilities);
        }
    }

    /**
     * Save the model to a set of files. The files will contain all the information needed to
     * apply the BDVal model to new samples.
     *
     * @param options The options associated with this model
     * @param task The classification task used for this model
     * @param splitPlan The split plan used to generat this model
     * @param writeModelMode The mode saving the model
     * @throws IOException if there is a problem writing to the files
     */
    public void save(final DAVOptions options, final ClassificationTask task,
                     final SplitPlan splitPlan, final WriteModel writeModelMode)
            throws IOException {
        if (zipModel) {
            LOG.info("Writing model to filename: " + zipFilename);
            ZipOutputStream zipStream = null;
            try {
                // Create the ZIP file
                zipStream = new ZipOutputStream(new FileOutputStream(zipFilename));
                save(zipStream, options, task, splitPlan, writeModelMode);
            } finally {
                IOUtils.closeQuietly(zipStream);
            }
        } else {
            LOG.info("Writing model properties  to filename: " + modelPropertiesFilename);
            saveProperties(FileUtils.openOutputStream(new File(modelPropertiesFilename)),
                    options, task, splitPlan, writeModelMode);

            helper.model.write(modelFilename);
            if (options.scaleFeatures) {
                if (options.probesetScaleMeanMap.size() <= 0) {
                    throw new IllegalArgumentException("mean map must be populated.");
                }
                if (options.probesetScaleRangeMap.size() <= 0) {
                    throw new IllegalArgumentException("range map must be populated.");
                }
            }

            saveMeansMap(FileUtils.openOutputStream(new File(meansMapFilename)), options);
            saveRangeMap(FileUtils.openOutputStream(new File(rangeMapFilename)), options);

            savePlatform(FileUtils.openOutputStream(new File(platformFilename)), options);
        }
    }

    /**
     * Save the model to a set the specified zip stream. The files will contain all the
     * information needed to apply the BDVal model to new samples.
     *
     * @param zipStream The stream to store the model to
     * @param options The options associated with this model
     * @param task The classification task used for this model
     * @param splitPlan The split plan used to generat this model
     * @param writeModelMode The mode saving the model
     * @throws IOException if there is a problem writing to the files
     */
    protected void save(final ZipOutputStream zipStream, final DAVOptions options,
                        final ClassificationTask task, final SplitPlan splitPlan,
                        final WriteModel writeModelMode) throws IOException {
        setZipStreamComment(zipStream);

        // Add ZIP entry for the model properties to output stream.
        saveProperties(zipStream, options, task, splitPlan, writeModelMode);

        // Add ZIP entries for the model training platform to output stream.
        savePlatform(zipStream, options);

        // Add ZIP entry for the model to output stream.
        zipStream.putNextEntry(new ZipEntry(FilenameUtils.getName(modelFilename)));
        // use an intermediate stream here since the model writer will close the stream
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        helper.model.write(byteArrayOutputStream);
        byteArrayOutputStream.writeTo(zipStream);
        zipStream.closeEntry();

        if (options.scaleFeatures) {
            if (options.probesetScaleMeanMap.size() <= 0) {
                throw new IllegalArgumentException("mean map must be populated.");
            }
            if (options.probesetScaleRangeMap.size() <= 0) {
                throw new IllegalArgumentException("range map must be populated.");
            }
        }

        // Add ZIP entry for the scale mean map to output stream.
        saveMeansMap(zipStream, options);

        // Add ZIP entry for the scale range map to output stream.
        saveRangeMap(zipStream, options);
    }

    /**
     * Sets the comment for this model into zip stream.
     * @param zipStream The stream to set the comment for
     */
    protected void setZipStreamComment(final ZipOutputStream zipStream) {
        final String bdvalVersion =
                VersionUtils.getImplementationVersion(DiscoverAndValidate.class);
        final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        zipStream.setComment(getClass().getName() + " generated with BDVal version "
                + bdvalVersion + " on " + dateFormat.format(new Date()));
    }

    /**
     * Store the BDVModel training platform to the specified output stream.
     *
     * @param stream  The stream to store the properties to
     * @param options The options associated with this model
     * @throws IOException if there is a problem writing to the stream
     */
    private void savePlatform(final OutputStream stream, final DAVOptions options)
            throws IOException {
        switch (format) {
            case BINARY:
                BinIO.storeObject(options.trainingPlatform, stream);
                break;
            case PROPERTIES:
                final Map<String, java.util.Properties> propertyMap =
                        options.trainingPlatform.toPropertyMap();
                for (final Map.Entry<String, java.util.Properties> entry : propertyMap.entrySet()) {
                    final String entryName = platformFilename + "." + entry.getKey() + "."
                            + ModelFileExtension.properties.toString();
                    if (zipModel) {
                        final ZipOutputStream zipStream = (ZipOutputStream) stream;
                        zipStream.putNextEntry(new ZipEntry(FilenameUtils.getName(entryName)));
                        entry.getValue().store(zipStream, null);
                        zipStream.closeEntry();
                    } else {
                        // this is really deprecated anyway, but in case we need to bring it back
                        // TODO - need to store a bunch of files here if we want to store not in zip
                        entry.getValue().store(stream, null);
                    }
                }
                break;
        }
    }

    /**
     * Store the BDVModel scale mean to the specified output stream.
     *
     * @param stream  The stream to store the properties to
     * @param options The options associated with this model
     * @throws IOException if there is a problem writing to the stream
     */
    private void saveMeansMap(final OutputStream stream, final DAVOptions options)
            throws IOException {
        probesetScaleMeanMap = options.probesetScaleMeanMap;
        switch (format) {
            case BINARY:
                BinIO.storeObject(probesetScaleMeanMap, stream);
                break;
            case PROPERTIES:
                if (zipModel) {
                    final ZipOutputStream zipStream = (ZipOutputStream) stream;
                    zipStream.putNextEntry(new ZipEntry(FilenameUtils.getName(meansMapFilename)));
                    saveMapAsProperties(probesetScaleMeanMap, zipStream);
                    zipStream.closeEntry();
                } else {
                    saveMapAsProperties(probesetScaleMeanMap, stream);
                }
                break;
        }
    }

    /**
     * Store the BDVModel scale range to the specified output stream.
     *
     * @param stream  The stream to store the properties to
     * @param options The options associated with this model
     * @throws IOException if there is a problem writing to the stream
     */
    private void saveRangeMap(final OutputStream stream, final DAVOptions options)
            throws IOException {
        probesetScaleRangeMap = options.probesetScaleRangeMap;
        switch (format) {
            case BINARY:
                BinIO.storeObject(probesetScaleRangeMap, stream);
                break;
            case PROPERTIES:
                if (zipModel) {
                    final ZipOutputStream zipStream = (ZipOutputStream) stream;
                    zipStream.putNextEntry(new ZipEntry(FilenameUtils.getName(rangeMapFilename)));
                    saveMapAsProperties(probesetScaleRangeMap, zipStream);
                    zipStream.closeEntry();
                } else {
                    saveMapAsProperties(probesetScaleRangeMap, stream);
                }
                break;
        }
    }

    /**
     * Writes the key/value pairs to an output stream as a set of java properties.
     *
     * @param map    The map to store
     * @param stream The stream to store the map to
     * @throws IOException if there is a problem writing to the stream
     */
    private void saveMapAsProperties(final Map<MutableString, Double> map,
                                     final OutputStream stream) throws IOException {
        final java.util.Properties properties = new java.util.Properties();
        if (map != null) {
            for (final Map.Entry<MutableString, Double> entry : map.entrySet()) {
                properties.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        properties.store(stream, null);
    }

    /**
     * Store the BDVModel properties to the specified output stream.
     *
     * @param stream         The stream to store the properties to
     * @param options        The options associated with this model
     * @param task
     * @param splitPlan      The split plan assoicated with this model
     * @param writeModelMode
     * @throws IOException if there is a problem writing to the stream
     */
    protected void saveProperties(final OutputStream stream,
                                  final DAVOptions options, final ClassificationTask task,
                                  final SplitPlan splitPlan,
                                  final WriteModel writeModelMode) throws IOException {
        final Properties modelProperties = new Properties();
        modelProperties.addProperty("trained.from.dataset", task.getExperimentDataFilename());
        modelProperties.addProperty("training.class0.label", task.getFirstConditionName());
        modelProperties.addProperty("training.class0.encoding", -1);
        modelProperties.addProperty("training.class1.label", task.getSecondConditionName());
        modelProperties.addProperty("training.class1.encoding", +1);
        modelProperties.addProperty("training.classifier.classname",
                (isConsensusModel() ? "< meaningless for consensus-of-models >" : helper.classifier.getClass().getCanonicalName()));
        modelProperties.addProperty("training.classifier.parameters", options.classifierParametersAsString());
        modelProperties.addProperty("scaling.use.percentiles", options.percentileScaling);
        modelProperties.addProperty("scaling.scaler.classname", options.scalerClassName);
        modelProperties.addProperty("scaling.enabled", options.scaleFeatures);
        modelProperties.addProperty("feature-normalization.enabled", options.normalizeFeatures);
        if (splitPlan != null) {
            modelProperties.addProperty("trained.from.split.split-id", writeModelMode.getSplitId());
            modelProperties.addProperty("trained.from.split-type", writeModelMode.getSplitType());
            modelProperties.addProperty("trained.from.split-plan", writeModelMode.getSplitPlanFilename());
        }

        modelProperties.addProperty("scaling.implementation.classname", options.scalerClass.getCanonicalName());
        addProperties(modelProperties);

        modelProperties.addProperty("pathway.aggregation.method", options.pathwayAggregtionMethod);
        modelProperties.addProperty("pathway.option.pathways", options.pathwaysInfoFilename);
        modelProperties.addProperty("pathway.option.gene-to-probe", options.geneToProbeFilename);

        try {
            if (zipModel) {
                final ZipOutputStream zipStream = (ZipOutputStream) stream;
                zipStream.putNextEntry(new ZipEntry(FilenameUtils.getName(modelPropertiesFilename)));
                modelProperties.save(zipStream);
                zipStream.closeEntry();
            } else {
                modelProperties.save(stream);
            }
        } catch (ConfigurationException e) {
            throw new IOException("Cannot write model properties.", e);
        }
    }

    /**
     * Add properties specific to the model type.
     * @param modelProperties The property object to add properties to.
     */
    protected void addProperties(final Properties modelProperties) {
    }

    public void prepareClassificationProblem(final Table testSet) throws InvalidColumnException,
            TypeMismatchException {
        if (delegate != null) {
            delegate.prepareClassificationProblem(testSet);
        } else {
            checkReOrderTestSet(testSet);
            modelSpecificProblem = loadProblem(getHelper(), testSet);
        }
    }

    public static ClassificationProblem loadProblem(final ClassificationHelper helper,
                                                    final Table testSet)
            throws InvalidColumnException, TypeMismatchException {
        final LoadClassificationProblem loader = new LoadClassificationProblem();
        final ClassificationProblem problem = helper.classifier.newProblem(0);
        loader.load(problem, testSet);
        problem.prepareNative();
        return problem;
    }

    protected void checkReOrderTestSet(final Table testSet) throws InvalidColumnException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("model expects " + trainingPlatform.getProbeIds().size() + " features.");
        }

        // remove any columns in the testSet that do not exist in the trainingPlatform
        // TODO: What if the trainingPlatform contains columns not in the testSet?
        for (int columnIndex = 0; columnIndex < testSet.getColumnNumber(); columnIndex++) {
            final String columnName = testSet.getIdentifier(columnIndex);
            if (!trainingPlatform.getProbeIds().containsKey(new MutableString(columnName))) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("removing column: " + columnName);
                }
                testSet.removeColumn(columnName);
            }
        }

        // TODO: can we end up in an infinite loop here?
        boolean permutation;
        do {
            permutation = false;
            for (int columnIndex = 0; columnIndex < testSet.getColumnNumber(); columnIndex++) {
                final String columnName = testSet.getIdentifier(columnIndex);
                final int featureIndex = trainingPlatform.getProbeIds().getInt(new MutableString(columnName));
                assert featureIndex < testSet.getColumnNumber() : "feature index out of range";
                permutation |= testSet.permutateColumns(columnName, featureIndex);
            }
        } while (permutation);

        for (int featurePosition = 0; featurePosition < trainingPlatform.getProbeIds().size(); featurePosition++) {
            final String featureId = testSet.getIdentifier(featurePosition);   // +1 takes account of the label
            final int featureIndex = trainingPlatform.getProbeIds().get(new MutableString(featureId));

            if (featureIndex != featurePosition) {
                LOG.fatal("Feature order does not match between the filtered input table and the "
                        + "model. Cannot use model with input table.");
                LOG.fatal(String.format("Feature %s is at position %d in the model, but at "
                        + "position %d in the table. ", featureId, featureIndex, featurePosition));
                System.exit(10);
            }
        }

        final int modelNumFeatures = trainingPlatform.getProbeIds().size();
        final int tableNumFeatures = testSet.getColumnNumber();
        if (modelNumFeatures != tableNumFeatures) {
            LOG.fatal("The number of features must match exactly between the model and the table.");
            LOG.fatal("Model expected: " + modelNumFeatures);
            LOG.fatal("Table contained: " + tableNumFeatures);
            System.exit(10);
        }

        final int numberOfSamples = testSet.getRowNumber();
    }

    /**
     * Is this model an consensus of other models?
     *
     * @return true if this model is a consensus model
     */
    public boolean isConsensusModel() {
        return properties.getBoolean("bdval.consensus.model", false) || delegate != null;
    }

    /**
     * Get the name of the dataset associated with this model.
     *
     * @return The dataset name
     */
    public String getDatasetName() {
        return datasetName;
    }
}
