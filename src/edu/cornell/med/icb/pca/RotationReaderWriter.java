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

package edu.cornell.med.icb.pca;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.oro.io.GlobFilenameFilter;
import org.bdval.io.compound.CompoundDataInput;
import org.bdval.io.compound.CompoundDataOutput;
import org.bdval.io.compound.CompoundFileReader;
import org.bdval.io.compound.CompoundFileWriter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to read write PCA rotation matrices.
 *
 * @author Kevin Dorff
 * @author Fabien Campagne
 */
public class RotationReaderWriter {
    /**
     * The cache directory.
     */
    private final File cacheDirectoryFile;

    /**
     * Filter to match the cache files.
     */
    private static final FilenameFilter CACHE_FILES_FILTER =
            new GlobFilenameFilter("Rotation-Cache-*-Split-*.compound-file");

    private static final Pattern SPLIT_NUM_FROM_COMPOUND_FILE_PATTERN =
            Pattern.compile("Rotation-Cache-(.+)-Split-(.+).compound-file");

    private static final Pattern ROTATION_MATRIX_NAME_PATTERN =
            Pattern.compile("(.+)/split-(.+)/pca-rotation-(.+)\\.bin");

    /**
     * The logger to use.
     */
    private static final Logger LOG = Logger.getLogger(RotationReaderWriter.class);

    /**
     * The CompoundFileWriter.
     */
    private CompoundFileWriter cfw;

    /**
     * The CompoundFileReader.
     */
    private CompoundFileReader cfr;

    /**
     * Template used to create cached table filenames.
     */
    private static final String S_CACHED_FILENAME = "%s/split-%s/pca-rotation-%s.bin";
    private static final String S_MAP_FILENAME = "%s/split-%s/pca-rotation-%s-%s-map.bin";

    private int splitId;

    /**
     * Create a TableCache storing tables in the "current working directory".
     *
     * @throws java.io.IOException the cacheDirectory doesn't exist or isn't a directory
     *                             or can't be read from or written to.
     */
    public RotationReaderWriter() throws IOException {
        this(new File("."), "NO_ENDPOINT", 0);
    }

    /**
     * Create a TableCache with the cache located in the
     * cacheDirectory directory.
     *
     * @param cacheDirectory the directory in which to store the cached tables
     * @param splitId        the split Id that this reader/writer is associated with
     * @throws java.io.IOException the cacheDirectory doesn't exist or isn't a directory
     *                             or can't be read from or written to.
     */
    public RotationReaderWriter(
            final File cacheDirectory, final CharSequence datasetEndpointName,
            final int splitId) throws IOException {
        super();
        final String cacheFilename = cacheDirectory.getCanonicalPath().intern();
        this.splitId = splitId;
        synchronized (cacheFilename) {
            if (!cacheDirectory.exists()) {
                FileUtils.forceMkdir(cacheDirectory);
            }
        }
        if (!cacheDirectory.exists()) {
            throw new IOException(
                    "Specified directory does not exist "
                            + cacheDirectory.getCanonicalPath());
        }
        if (!cacheDirectory.isDirectory()) {
            throw new IOException(
                    "Specified file is not a directory "
                            + cacheDirectory.getCanonicalPath());
        }
        if (!cacheDirectory.canRead()) {
            throw new IOException(
                    "Specified directory is not readable "
                            + cacheDirectory.getCanonicalPath());
        }
        if (!cacheDirectory.canWrite()) {
            throw new IOException(
                    "Specified directory is not writable "
                            + cacheDirectory.getCanonicalPath());
        }
        String cacheDirectoryName = cacheDirectory.getCanonicalPath();
        if (!cacheDirectoryName.endsWith("/") && !cacheDirectoryName.endsWith("\\")) {
            cacheDirectoryName += "/";
        }

        cacheDirectoryFile = new File(cacheDirectoryName);

        // If you change this file name in any way
        // you will have to change some constants above!!!
        final String cacheCompoundFilename = cacheDirectoryName
                + "Rotation-Cache-" + datasetEndpointName + "-Split-" + splitId + ".compound-file";

        cfw = new CompoundFileWriter(cacheCompoundFilename);
        LOG.trace("Opened the compound cache file " + cacheCompoundFilename);
        cfr = cfw.getCompoundFileReader();
    }


    /**
     * Check if a table has been saved to the cache.
     *
     * @param datasetEndpointName the dataset to find a table for
     * @param pathwayId          the pathwayId to find a table for
     * @return true if table a table with the specified paramters has been cached
     */
    public boolean isTableCached(final CharSequence datasetEndpointName, final MutableString pathwayId) {
        final boolean result;
        final String cachedTableFile = getRotationFile(datasetEndpointName, pathwayId);
        if (LOG.isTraceEnabled()) {
            LOG.trace(Thread.currentThread().getName() + " entering synchronized block on "
                    + cachedTableFile.intern());
        }

        synchronized (cachedTableFile.intern()) {
            result = cfr.containsFile(cachedTableFile);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Checked for existance of table " + cachedTableFile + " : " + result);
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace(Thread.currentThread().getName() + " left synchronized block on "
                    + cachedTableFile.intern());
        }
        return result;
    }

    /**
     * Retrieve a saved rotation matrix. The method hasRotationMatrix should be called
     * before calling this to verify that the saved information exists. Null will
     * be returned if no saved data exists.
     *
     * @param datasetEndpointName the dataset to find a table for
     * @param pathwayId          the pathwayId to find a table for
     * @param rowIds             the row ids for the tabe we're reading
     * @return the rotation matrix.
     */
    public double[][] getRotationMatrix(final String datasetEndpointName, final MutableString pathwayId, final List<CharSequence> rowIds) {
        final String cachedTableFile = getRotationFile(datasetEndpointName, pathwayId);
        return getRotationMatrix(cachedTableFile, rowIds);
    }

    public double[][] getRotationMatrix(final String cachedTableFile, final List<CharSequence> rowIds) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("getting rotation matrix from " + cachedTableFile);
        }
        try {
            final CompoundDataInput dataInput = cfr.readFile(cachedTableFile);

            final int numberOfColumns = dataInput.readInt();
            final int numberOfRows = dataInput.readInt();
            final double[][] result = new double[numberOfColumns][numberOfRows];

            //        System.out.printf("Reading rotation matrix with %d columns%n", numberOfColumns);
            //read row identifiers:
            for (int r = 0; r < numberOfRows; r++) {
                rowIds.add(dataInput.readUTF());
            }
            for (int c = 0; c < numberOfColumns; c++) {
                // the number of row elements for the column is ignored, but needs to be read.
                final int numR = dataInput.readInt();
                for (int r = 0; r < numberOfRows; r++) {
                    result[c][r] = dataInput.readDouble();
                }
            }
            LOG.trace("Rotation loaded from cache.");
            return result;
        } catch (IOException e) {
            LOG.error(e);
            return null;
        }
    }

    public ObjectSet<CharSequence> getTableColumnIds(final CharSequence datasetEndpointName,
                                                     final MutableString pathwayId) {
        final String cachedTableFile = getRotationFile(datasetEndpointName, pathwayId);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Attempting to read cached table from " + cachedTableFile);
        }
        final ObjectSet<CharSequence> result = new ObjectArraySet<CharSequence>();
        try {
            final CompoundDataInput dataInput = cfr.readFile(cachedTableFile);
            final int numberOfColumns = dataInput.readInt();
            LOG.trace("Reading cached table with %d columns%n" + numberOfColumns);
            for (int i = 0; i < numberOfColumns; i++) {
                final String colType = dataInput.readUTF();
                final String colId = dataInput.readUTF();

                result.add(colId);

                if (colType.equals("s")) {
                    final int numStrings = dataInput.readInt();

                    for (int j = 0; j < numStrings; j++) {
                        dataInput.readUTF();
                    }
                } else if (colType.equals("d")) {
                    final int numDoubles = dataInput.readInt();
                    // we don't need to read these doubles, just skip them;
                    dataInput.skipBytes(Double.SIZE * numDoubles / 8);

                } else {
                    LOG.error("UNKNOWN COLUMN TYPE " + colType
                            + " cannot read cached table from file "
                            + cachedTableFile);
                    return null;
                }
            }
            return result;
        } catch (IOException e) {
            LOG.error(e);
            return null;
        }
    }

    /**
     * Given the specified identifiers, save the rotation matrix to the cache.
     *
     * @param datasetEndpointName the dataset to find a table for
     * @param pathwayId          the pathwayId to find a table for
     * @param rowIds             the row ideas to save
     * @param rotation           the rotation table to save
     */
    public void saveRotationMatrix(final CharSequence datasetEndpointName,
                                   final MutableString pathwayId, final List<CharSequence> rowIds,
                                   final double[][] rotation) {

        final int numColumns = rotation.length;
        final int numRows = rowIds.size();
        //    System.out.println(String.format("numColumns: %d numRows: %d", numColumns, numRows));
        for (final double[] column : rotation) {
            assert column.length == rowIds.size() : "number of rows of rotation matrix must match number of row identifiers.";
        }

        final String cachedTableFile = getRotationFile(datasetEndpointName, pathwayId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving " + pathwayId + " to rotation file " + cachedTableFile);
        }
        CompoundDataOutput dataOutput = null;
        try {
            dataOutput = cfw.addFile(cachedTableFile);

            // Write the number of columns
            dataOutput.writeInt(numColumns);
            // Write the number of rows
            dataOutput.writeInt(numRows);

            for (final CharSequence rowId : rowIds) {
                dataOutput.writeUTF(rowId.toString());
            }

            for (int c = 0; c < numColumns; c++) {
                // write each column:

                final double[] doubleColumnData = rotation[c];
                final int numDoubles = doubleColumnData.length;
                dataOutput.writeInt(numDoubles);
                for (final double doubleColumnItem : doubleColumnData) {
                    // Each double
                    dataOutput.writeDouble(doubleColumnItem);
                }
            }

            LOG.trace("Rotation saved to cache.");
        } catch (Exception e) {
            LOG.error("Cannot cache table. ", e);
        } finally {
            if (dataOutput != null) {
                try {

                    dataOutput.close();
                } catch (IOException e) {
                    LOG.error("Error closing CompoundDataOutput", e);
                }
            }
        }
    }

    /**
     * Given the specified parameters, return the file where rotation information is stored.
     *
     * @param datasetEndpointName the dataset to find a table for
     * @param pathwayId          the pathwayId to find a table for
     * @return the file where rotation information is stored.
     */
    private String getRotationFile(CharSequence datasetEndpointName, MutableString pathwayId) {
        pathwayId = pathwayId.replace(' ', '_');
        pathwayId = pathwayId.replace('/', '_');
        datasetEndpointName = datasetEndpointName.toString().replace('/', '_');
        final String cachedTableFilename = String.format(S_CACHED_FILENAME,
                datasetEndpointName, splitId, pathwayId);
        LOG.trace("rotation filename:  " + cachedTableFilename);
        return cachedTableFilename;
    }

    private String getMapFile(CharSequence datasetEndpointName, MutableString pathwayId, final String mapType) {
        pathwayId = pathwayId.replace(' ', '_');
        pathwayId = pathwayId.replace('/', '_');
        datasetEndpointName = datasetEndpointName.toString().replace('/', '_');
        final String cachedTableFilename = String.format(S_MAP_FILENAME,
                datasetEndpointName, splitId, pathwayId, mapType);
        LOG.trace("map filename:  " + cachedTableFilename);
        return cachedTableFilename;
    }

    /**
     * Given the specified parameters, return the filenames that contain
     * rotation matrices across all splits. The result is a map of
     * compound files to the filenames within the compound files.
     *
     * @param datasetEndpointName the dataset to find a table for
     * @param pathwayId          the pathwayId to find a table for
     * @return the files where rotation information is stored.
     */
    public Map<String, Set<String>> getRotationFiles(
            final CharSequence datasetEndpointName, MutableString pathwayId) {
        pathwayId = pathwayId.replace(' ', '_');
        pathwayId = pathwayId.replace('/', '_');
        final String datasetEndpointNameComparison =
                datasetEndpointName.toString().replace('/', '_') + "/";
        final String pathwayIdComparison = pathwayId.toString();

        final Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        final File[] compoundFiles = cacheDirectoryFile.listFiles(CACHE_FILES_FILTER);
        for (final File file : compoundFiles) {
            String compoundFileName = "[uninitialized]";
            try {
                compoundFileName = file.getCanonicalPath();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Scanning for rotation files within compound file "
                            + compoundFileName);
                }
                final CompoundFileReader compoundReader = new CompoundFileReader(compoundFileName);
                final Set<String> output = new HashSet<String>();
                results.put(compoundFileName, output);

                final Set<String> names = compoundReader.getFileNames();
                for (final String name : names) {
                    final Matcher m = ROTATION_MATRIX_NAME_PATTERN.matcher(name);
                    if (!m.matches()) {
                        continue;
                    }
                    final String fileDatasetEndpointName = m.group(1);
                    // String fileSplitId = m.group(2);
                    final String filePathwayId = m.group(3);
                    if (filePathwayId.endsWith("-map")) {
                        // A map file, not a rotation matrix file
                        continue;
                    }

                    if (!datasetEndpointNameComparison.equals(fileDatasetEndpointName)) {
                        // Wrong datasetEndpointName
                        continue;
                    }
                    if (!pathwayIdComparison.equals(filePathwayId)) {
                        // Wrong pathwayId
                        continue;
                    }
                    // Looks good
                    output.add(name);
                }
                compoundReader.close();
            } catch (IOException e) {
                LOG.error("Error opening/reading compound file " + compoundFileName, e);
            }
        }
        return results;
    }

    public void saveMap(final String datasetEndpointName, final MutableString pathwayId, final List<CharSequence> rowIds, final Object2DoubleOpenHashMap<MutableString> map, final String type) {
        final String mapFileName = getMapFile(datasetEndpointName, pathwayId, type);
        CompoundDataOutput dataOutput = null;
        try {
            dataOutput = cfw.addFile(mapFileName);
            dataOutput.writeObject(map);
            LOG.trace("Map saved.");
        } catch (IOException e) {
            LOG.error("Error storing scaling map to compound sub-file " + mapFileName, e);
        } finally {
            if (dataOutput != null) {
                try {
                    dataOutput.close();
                } catch (IOException e) {
                    LOG.error("Error closing CompoundDataOutput", e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public  Object2DoubleOpenHashMap<MutableString> loadMap(final String datasetEndpointName, final MutableString pathwayId, final String type) {
        final String mapFileName = getMapFile(datasetEndpointName, pathwayId, type);

        try {
            final CompoundDataInput dataInput = cfr.readFile(mapFileName);
            final Object2DoubleOpenHashMap<MutableString> o =
                    (Object2DoubleOpenHashMap<MutableString>) dataInput.readObject();
            LOG.trace("Map loaded.");
            return o;
        } catch (IOException e) {
            LOG.error("Error loading scaling map to compound sub-file " + mapFileName, e);
        } catch (ClassNotFoundException e) {
            LOG.error("Error loading scaling map to compound sub-file " + mapFileName, e);
        }
        return null;
    }

    public static int splitIdFromCompoundFilename(final String compoundFilename) {
        final Matcher m = SPLIT_NUM_FROM_COMPOUND_FILE_PATTERN.matcher(compoundFilename);
        if (!m.matches()) {
            return -1;
        }
        return Integer.parseInt(m.group(2));
    }

}
