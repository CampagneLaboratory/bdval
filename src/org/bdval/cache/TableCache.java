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

package org.bdval.cache;

import edu.cornell.med.icb.geo.tools.FullGeneList;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.mssm.crover.tables.ArrayTable;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class to cache (read/write) Tables.
 *
 * @author Kevin Dorff
 */
public class TableCache {
    /**
     * The logger to use.
     */
    private static final Logger LOG = Logger.getLogger(TableCache.class);

    /**
     * The directory to store the tables to.
     */
    private String cacheDirectoryName;

    /**
     * Template used to create cached table filenames.
     */
    private static final String S_CACHED_FILENAME = "%s/cached-table-%s-%s-%d.bin";

    /**
     * Create a TableCache storing tables in the "current working directory".
     *
     * @throws IOException the cacheDirectory doesn't exist or isn't a directory
     *                     or can't be read from or written to.
     */
    public TableCache() throws IOException {
        this(new File("."));
    }

    /**
     * Create a TableCache with the cache located in the
     * cacheDirectory directory.
     *
     * @param cacheDirectory the directory in which to store the cached tables
     * @throws IOException the cacheDirectory doesn't exist or isn't a directory
     *                     or can't be read from or written to.
     */
    public TableCache(final File cacheDirectory) throws IOException {
        super();
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
        cacheDirectoryName = cacheDirectory.getCanonicalPath();
        if (!cacheDirectoryName.endsWith("/") && !cacheDirectoryName.endsWith("\\")) {
            cacheDirectoryName += "/";
        }
    }

    /**
     * Check if a table has been saved to the cache.
     *
     * @param splitId     The split id
     * @param splitType   The Split type
     * @param datasetName The dataset name
     * @return true if table a table with the specified paramters has been cached
     */
    public boolean isTableCached(final int splitId, final String splitType,
                                 final String datasetName) {
        final File cachedTableFile = getCachedTableFile(splitId, splitType, datasetName);
        final String filename;
        try {
            filename = cachedTableFile.getCanonicalPath();
        } catch (IOException e) {
            return false;
        }
        synchronized (filename.intern()) {
            return cachedTableFile.exists();
        }
    }
    /**
     * Remove a specific table from the cache.
     *
     * @param splitId     The split id
     * @param splitType   The Split type
     * @param datasetName The dataset name
     */
    public void clearFromCache(final int splitId, final String splitType,
                               final String datasetName) {
        final File cachedTableFile = getCachedTableFile(splitId, splitType, datasetName);
        if (cachedTableFile.exists()) {
            if (!cachedTableFile.delete()) {
                LOG.warn("Unable to delete cache file " + cachedTableFile.getAbsolutePath());
            }
        }
    }

    /**
     * Retrieve a cached table from the cache. isTableCached should be called
     * before calling this to verify that the table is cached. Null will
     * be returned if the table wasn't in the cache or there was a problem
     * reading the table.
     *
     * @param splitId     The split id
     * @param splitType   The Split type
     * @param datasetName The dataset name
     * @return the table read from the cache (or null if it could not be read)
     */
    public Table getCachedTable(final int splitId, final String splitType,
                                final String datasetName) {
        return getCachedTable(splitId, splitType, datasetName, null);
    }

    /**
     * Retrieve a cached table from the cache. isTableCached should be called
     * before calling this to verify that the table is cached. Null will
     * be returned if the table wasn't in the cache or there was a problem
     * reading the table.
     *
     * @param splitId        The split id
     * @param splitType      The Split type
     * @param datasetName    The dataset name
     * @param geneListFilter A gene list. If not null, the gene list is queried with each double
     * column identifier to determine if the identifier is contained in the gene list. Columns
     * that do not match the gene list are not loaded.
     * @return the table read from the cache (or null if it could not be read)
     */
    public Table getCachedTable(final int splitId, final String splitType,
                                final String datasetName, final GeneList geneListFilter) {
        final File cachedTableFile = getCachedTableFile(splitId, splitType, datasetName);

        if (geneListFilter != null && !(geneListFilter instanceof FullGeneList)) {
            final ObjectSet<CharSequence> tableColumnIds =
                    getTableColumnIds(splitId, splitType, datasetName);
            geneListFilter.calculateProbeSetSelection(tableColumnIds);
        }
        DataInputStream dataInput = null;
        try {
            dataInput = new DataInputStream(
                    new FastBufferedInputStream(new FileInputStream(cachedTableFile)));
            final ArrayTable result = new ArrayTable();
            final int numberOfColumns = dataInput.readInt();
            LOG.info("Reading cached table with " + numberOfColumns + " columns");
            for (int i = 0; i < numberOfColumns; i++) {
                final String colType = dataInput.readUTF();
                final String colId = dataInput.readUTF();

                if ("s".equals(colType)) {
                    final int numStrings = dataInput.readInt();
                    resize(result, numStrings);
                    final int columnIndex = result.addColumn(colId, String.class);
                    for (int j = 0; j < numStrings; j++) {
                        result.appendObject(columnIndex, dataInput.readUTF());
                    }
                } else if ("d".equals(colType)) {
                    final int numDoubles = dataInput.readInt();
                    resize(result, numDoubles);
                    if (geneListFilter != null && !geneListFilter.isProbesetInList(colId)) {
                        // the column does not match the gene list. Skip this column
                        // we don't need to read these doubles, just skip them;
                        final int numBytes = Double.SIZE * numDoubles / 8;
                        final int actualBytes = dataInput.skipBytes(numBytes);
                        if (actualBytes != numBytes) {
                            LOG.warn("actual bytes skipped (" + actualBytes + ") does"
                                    + "not equal expected of " + numBytes);
                        }
                        continue;
                    }

                    final int columnIndex = result.addColumn(colId, double.class);
                    for (int j = 0; j < numDoubles; j++) {
                        result.appendDoubleValue(columnIndex, dataInput.readDouble());
                    }

                } else {
                    LOG.error("UNKNOWN COLUMN TYPE " + colType
                            + " cannot read cached table from file "
                            + filenameOf(cachedTableFile));
                    return null;
                }
            }
            return result;
        } catch (IOException e) {
            LOG.error(e);
            return null;
        } catch (TypeMismatchException e) {
            LOG.error("TypeMismatchException adding data to Table "
                    + filenameOf(cachedTableFile), e);
            return null;
        } finally {
            IOUtils.closeQuietly(dataInput);
        }
    }

    public ObjectSet<CharSequence> getTableColumnIds(final int splitId, final String splitType,
                                                     final String datasetName) {
        final File cachedTableFile = getCachedTableFile(splitId, splitType, datasetName);
        final ObjectSet<CharSequence> result = new ObjectOpenHashSet<CharSequence>();
        DataInputStream dataInput = null;
        try {
            dataInput = new DataInputStream(
                    new FastBufferedInputStream(new FileInputStream(cachedTableFile)));
            final int numberOfColumns = dataInput.readInt();
            LOG.info("Reading cached table with " + numberOfColumns + " columns");
            for (int i = 0; i < numberOfColumns; i++) {
                final String colType = dataInput.readUTF();
                final String colId = dataInput.readUTF();

                result.add(colId);

                if ("s".equals(colType)) {
                    final int numStrings = dataInput.readInt();

                    for (int j = 0; j < numStrings; j++) {
                        dataInput.readUTF();
                    }
                } else if ("d".equals(colType)) {
                    final int numDoubles = dataInput.readInt();
                    // we don't need to read these doubles, just skip them;
                    final int numBytes = Double.SIZE * numDoubles / 8;
                    final int actualBytes = dataInput.skipBytes(numBytes);
                    if (actualBytes != numBytes) {
                        LOG.warn("actual bytes skipped (" + actualBytes + ") does "
                                + "not equal expected of " + numBytes);
                    }
                } else {
                    LOG.error("UNKNOWN COLUMN TYPE " + colType
                            + " cannot read cached table from file "
                            + filenameOf(cachedTableFile));
                    return null;
                }
            }
            return result;
        } catch (IOException e) {
            LOG.error("Error getting column ids", e);
            return null;
        } finally {
            IOUtils.closeQuietly(dataInput);
        }
    }

    private void resize(final ArrayTable result, final int numRows) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Resizing table to " + numRows + 1);
        }
        result.setInitialSize(numRows + 1);
        result.setChunk(numRows + 1);
    }

    /**
     * Given the specified paramters, save the table to the cache.
     *
     * @param splitId     The split id
     * @param splitType   The Split type
     * @param datasetName The dataset name
     * @param table       the Table to save.
     */
    public void saveTableToCache(final int splitId, final String splitType,
            final String datasetName, final Table table) {
        DataOutputStream dataOutput = null;
        try {
            if (!checkTableConfiguration(table)) {
                return;
            }

            final int numColumns = table.getColumnNumber();

            final File cachedTableFile = getCachedTableFile(splitId, splitType, datasetName);
            dataOutput = new DataOutputStream(
                    new FastBufferedOutputStream(new FileOutputStream(cachedTableFile)));

            // Write the number of columns
            dataOutput.writeInt(numColumns);
            LOG.info("Writing " + numColumns + " columns");
            int numWritten = 0;
            for (int i = 0; i < numColumns; i++) {
                // For each column write if it is a "d"ouble or "s"tring column
                final String id = table.getIdentifier(i);
                if (table.getType(i) == String.class) {
                    dataOutput.writeUTF("s");
                    dataOutput.writeUTF(id);
                    final String[] stringColumnData = table.getStrings(id);
                    final int numStrings = stringColumnData.length;
                    dataOutput.writeInt(numStrings);
                    for (final String stringColumnItem : stringColumnData) {
                        // Each String
                        dataOutput.writeUTF(stringColumnItem);
                    }
                    numWritten++;
                } else if (table.getType(i) == double.class) {
                    dataOutput.writeUTF("d");
                    dataOutput.writeUTF(id);
                    final double[] doubleColumnData = table.getDoubles(id);
                    final int numDoubles = doubleColumnData.length;
                    dataOutput.writeInt(numDoubles);
                    for (final double doubleColumnItem : doubleColumnData) {
                        // Each double
                        dataOutput.writeDouble(doubleColumnItem);
                    }
                    numWritten++;
                }
            }

            dataOutput.flush();

            LOG.info("Wrote " + numWritten + " columns");
            LOG.info("++ SAVED TABLE TO CACHE for split-id=" + splitId
                    + ", split-type=" + splitType + ", dataset-name=" +  datasetName);
        } catch (IOException e) {
            LOG.error("Cannot cache table. ", e);
        } catch (InvalidColumnException e) {
            LOG.error("Invalid table data", e);
        } finally {
            IOUtils.closeQuietly(dataOutput);
        }
    }

    /**
     * Given the specified paramters, return the File that will
     * be used for by TableCache.
     *
     * @param splitId     the split id
     * @param splitType   the split type
     * @param datasetName the dataset name
     * @return the File for the TableCache
     */
    private File getCachedTableFile(final int splitId,
                                    final String splitType,
                                    final String datasetName) {
        final String cachedTableFilename = String.format(S_CACHED_FILENAME,
                cacheDirectoryName, datasetName, StringUtils.defaultString(splitType, "complete"),
                splitId);
        return new File(cachedTableFilename);
    }

    /**
     * Return the complete path filename for a file.
     *
     * @param file the file to get the filename of
     * @return the complete path filename for the file
     */
    private String filenameOf(final File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return "<error obtaining filename>";
        }
    }

    /**
     * Verify the table is configured appropriately,
     * two columns and only String and double columns.
     *
     * @param table the table to verify
     * @return true if the table checks out appropriately.
     */
    private boolean checkTableConfiguration(final Table table) {
        final int numColumns = table.getColumnNumber();
        if (numColumns <= 1) {
            LOG.error("TableCache only supports tables with at least two columns.");
        }
        for (int i = 0; i < numColumns; i++) {
            if (table.getType(i) != String.class && table.getType(i) != double.class) {
                LOG.error("TableCache only supports tables where the "
                        + "columns are String.class or double.class");
                return false;
            }
        }
        return true;
    }
}
