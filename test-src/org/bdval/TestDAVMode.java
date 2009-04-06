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

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import edu.cornell.med.icb.tools.geo.FullGeneList;
import edu.mssm.crover.tables.ColumnTypeException;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import edu.mssm.crover.tables.cache.TableCache;
import edu.mssm.crover.tables.readers.SyntaxErrorException;
import edu.mssm.crover.tables.readers.UnsupportedFormatException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

/**
 * Validates functionality of {@link DAVMode}.
 */
public class TestDAVMode {
    /**
     * Default constructor.
     */
    public TestDAVMode() {
        super();
    }

    /**
     * Validates functionality of {@link DAVMode#readInputFile(String)}.
     * @throws SyntaxErrorException if there is an error in the file
     * @throws IOException if the input file cannot be read
     * @throws UnsupportedFormatException if the file format is not recognized
     */
    @Test(expected = NullPointerException.class)
    public void testReadNullInputFile()
            throws SyntaxErrorException, IOException, UnsupportedFormatException {
        final DAVMode davMode = new DAVMode();
        davMode.readInputFile(null);
    }

    /**
     * Validates functionality of {@link DAVMode#readInputFile(String)}.
     * @throws SyntaxErrorException if there is an error in the file
     * @throws IOException if the input file cannot be read
     * @throws UnsupportedFormatException if the file format is not recognized
     */
    @Test(expected = FileNotFoundException.class)
    public void testReadEmptyInputFile()
            throws SyntaxErrorException, IOException, UnsupportedFormatException {
        final DAVMode davMode = new DAVMode();
        davMode.readInputFile("");
    }

    /**
     * Validates functionality of {@link DAVMode#readInputFile(String)}.
     * @throws SyntaxErrorException if there is an error in the file
     * @throws IOException if the input file cannot be read
     * @throws UnsupportedFormatException if the file format is not recognized
     */
    @Test(expected = UnsupportedFormatException.class)
    public void testReadUnknownInputFile()
            throws SyntaxErrorException, IOException, UnsupportedFormatException {
        // it is unlikely that mp3 will provide anything useful
        final File file = File.createTempFile("file", ".mp3");
        final DAVMode davMode = new DAVMode();
        davMode.readInputFile(file.getAbsolutePath());
    }

    /**
     * Validates functionality of {@link DAVMode#readInputFile(String)}.
     * @throws SyntaxErrorException if there is an error in the file
     * @throws IOException if the input file cannot be read
     * @throws UnsupportedFormatException if the file format is not recognized
     */
    @Test
    public void testReadTMMInputFile()
            throws SyntaxErrorException, IOException, UnsupportedFormatException {
        final DAVMode davMode = new DAVMode();
        final Table table = davMode.readInputFile("data/test/bdval/test.tmm");
        assertNotNull("Table should not be null", table);
        assertEquals("There should be 3 columns", 3, table.getColumnNumber());
        assertEquals("There should be 5 rows", 5, table.getRowNumber());
    }

    /**
     * Validates functionality of {@link DAVMode#readInputFile(String)}.
     * @throws SyntaxErrorException if there is an error in the file
     * @throws IOException if the input file cannot be read
     * @throws UnsupportedFormatException if the file format is not recognized
     */
    @Test
    public void testReadCompressedTMMInputFile()
            throws SyntaxErrorException, IOException, UnsupportedFormatException {
        final DAVMode davMode = new DAVMode();
        final Table table = davMode.readInputFile("data/test/bdval/test.tmm.gz");
        assertNotNull("Table should not be null", table);
        assertEquals("There should be 3 columns", 3, table.getColumnNumber());
        assertEquals("There should be 5 rows", 5, table.getRowNumber());
    }

    /**
     * Gets the name of a directory to use for a cache during the tests.  The directory
     * itself is not created.
     * @return The full path of a directory that can be used for testing.
     * @throws IOException if no valid directory name can be created
     */
    private String getTempCacheDirectory() throws IOException {
        final File tmpFile = File.createTempFile("davMode", "test");
        final String tmpDirName = tmpFile.getAbsolutePath();
        tmpFile.delete();
        return FilenameUtils.concat(tmpDirName, "cache");
    }

    /**
     * Validates that the cache is not created in DAVMode when default options are given.
     * @throws JSAPException If there was a problem setting up the test
     * @throws IOException If there was a problem with the cache
     */
    @Test
    public void testTableCacheDisabled() throws IOException, JSAPException {
        final String cacheDirectoryName = getTempCacheDirectory();
        final String inputFileName = "data/test/bdval/test.tmm.gz";

        final String[] args = { "--cache-dir", cacheDirectoryName,
                "--input", inputFileName};

        final DAVMode davMode = new DAVMode();
        final DAVOptions davOptions = new DAVOptions();
        final JSAP jsap = new JSAP();
        davMode.defineOptions(jsap);

        final JSAPResult jsapResult = jsap.parse(args);
        davMode.setupTableCache(jsapResult, davOptions);

        final File cacheDirectory = new File(cacheDirectoryName);
        assertFalse("Cache directory should not have been created", cacheDirectory.exists());

        // check to see that no exceptions occur here
        davMode.setupInput(jsapResult, davOptions);
        davMode.removeFromCache(42, null, null);
    }

    /**
     * Validates that the cache is created properly in DAVMode when enabled.
     * @throws JSAPException If there was a problem setting up the test
     * @throws IOException If there was a problem with the cache
     * @throws ColumnTypeException If there was a problem reading the tables
     * @throws InvalidColumnException If there was a problem reading the tables
     * @throws TypeMismatchException If there was a problem reading the tables
     */
    @Test
    public void testTableCacheEnabled() throws IOException, JSAPException, InvalidColumnException,
            ColumnTypeException, TypeMismatchException {
        final String cacheDirectoryName = getTempCacheDirectory();
        final String inputFileName = "data/test/bdval/test.tmm.gz";

        final String[] args = { "--enable-cache", "--cache-dir", cacheDirectoryName,
            "--input", inputFileName};

        final DAVMode davMode = new DAVMode();
        final DAVOptions davOptions = new DAVOptions();
        final JSAP jsap = new JSAP();
        davMode.defineOptions(jsap);

        final JSAPResult jsapResult = jsap.parse(args);
        davMode.setupTableCache(jsapResult, davOptions);

        final File cacheDirectory = new File(cacheDirectoryName);
        assertTrue("Cache directory should have been created", cacheDirectory.exists());

        // create the actual cache directory
        davMode.setupPathwayOptions(jsapResult, davOptions);
        davMode.setupInput(jsapResult, davOptions);
        davMode.processTable(new FullGeneList(null),
                davOptions.inputTable, davOptions, new ArrayList<Set<String>>(), true);

        final File cacheFile = new File(FilenameUtils.concat(cacheDirectoryName, "pathways=false"
                + SystemUtils.FILE_SEPARATOR + "cached-table-null-complete-0.bin"));
        assertTrue("Cache file should have been created", cacheFile.exists());

        final TableCache tableCache =
                new TableCache(new File(FilenameUtils.concat(cacheDirectoryName, "pathways=false")));
        assertTrue("Table should be cached at this point", tableCache.isTableCached(0, null, null));

        // note that the cached table is not the same as the input dataset
        Table table = tableCache.getCachedTable(0, null, null);
        assertNotNull("The table from the cache should not be null", table);
        assertEquals("There should be 6 columns", 6, table.getColumnNumber());
        assertEquals("There should be 2 rows", 2, table.getRowNumber());

        davMode.removeFromCache(0, null, null);
        assertFalse("Cache file should have been deleted", cacheFile.exists());
        assertFalse("Table should no longer be cached at this point",
                tableCache.isTableCached(0, null, null));
        table = tableCache.getCachedTable(0, null, null);
        assertNull("The table from the cache should be null", table);
    }
}
