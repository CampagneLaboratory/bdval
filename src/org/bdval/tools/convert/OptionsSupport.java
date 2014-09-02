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

package org.bdval.tools.convert;

import edu.cornell.med.icb.iterators.TextFileLineIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Utility methods for validating options.
 *
 * @author Kevin Dorff
 */
public final class OptionsSupport {
    /**
     * Private constructor for utility class.
     */
    private OptionsSupport() {
        super();
    }

    /**
     * Check that a directory exists and is writable.
     * @param outputDirAsFile the directory to check
     * @param key the associated JSAP option / key
     * @throws OptionsConfigurationException problem with directory
     */
    public static void verifyWritableDirectory(final File outputDirAsFile, final String key)
            throws OptionsConfigurationException {
        if (outputDirAsFile == null) {
            throw new OptionsConfigurationException(key + " not properly defined");
        }

        final String inputDir = filenameFromFile(outputDirAsFile);
        if (inputDir == null) {
            throw new OptionsConfigurationException("Could not determine full path for " + key);
        }

        try {
            // Make sure it exists.
            FileUtils.forceMkdir(outputDirAsFile);
        } catch (IOException e) {
            throw new OptionsConfigurationException(
                    "Couldn't verify / create directory for " + key + " "
                            + filenameFromFile(outputDirAsFile), e);
        }
        if (!outputDirAsFile.exists() || !outputDirAsFile.isDirectory()
                || !outputDirAsFile.canWrite()) {
            throw new OptionsConfigurationException(
                    "Could not find readable " + key + " " + inputDir);
        }
    }

    /**
     * Check that a directory exists and is readable.
     * @param inputDirAsFile the directory to check
     * @param key the associated JSAP option / key
     * @throws OptionsConfigurationException problem with directory
     */
    public static void verifyReadableDirectory(final File inputDirAsFile, final String key)
            throws OptionsConfigurationException {
        if (inputDirAsFile == null) {
            throw new OptionsConfigurationException(key + " not properly defined");
        }

        final String inputDir = filenameFromFile(inputDirAsFile);
        if (inputDir == null) {
            throw new OptionsConfigurationException("Could not determine full path for " + key);
        }

        if (!inputDirAsFile.exists() || !inputDirAsFile.isDirectory()
                || !inputDirAsFile.canRead()) {
            throw new OptionsConfigurationException(
                    "Could not find readable " + key + " " + inputDir);
        }
    }

    /**
     * Check that a file is writable. If one exists it will be deleted.
     * A new, empty file will be created.
     * @param outputFileAsFile the writable file to make (will be empty)
     * @param key the associated JSAP option / key
     * @throws OptionsConfigurationException problem with directory
     */
    public static void createNewWritableFile(final File outputFileAsFile, final String key)
            throws OptionsConfigurationException {
        if (outputFileAsFile == null) {
            throw new OptionsConfigurationException(key + " not properly defined");
        }

        final String outputFile = filenameFromFile(outputFileAsFile);
        if (outputFile == null) {
            throw new OptionsConfigurationException(
                    "Could not determine full filename path for " + key);
        }

        FileUtils.deleteQuietly(outputFileAsFile);
        try {
            outputFileAsFile.createNewFile();
        } catch (IOException e) {
            throw new OptionsConfigurationException("Could not create " + key + " " + outputFile,
                    e);
        }
        if (!outputFileAsFile.exists() || !outputFileAsFile.isFile() || !outputFileAsFile
                .canWrite()) {
            throw new OptionsConfigurationException(
                    "Could not create writable " + key + " " + outputFile);
        }
    }

    /**
     * Check that a file exists and is readable.
     * @param inputFileAsFile the file that should be readable
     * @param key the associated JSAP option / key
     * @throws OptionsConfigurationException problem with directory
     */
    public static void verifyReadableFile(final File inputFileAsFile, final String key)
            throws OptionsConfigurationException {
        if (inputFileAsFile == null) {
            throw new OptionsConfigurationException(key + " not properly defined");
        }

        final String inputFile = filenameFromFile(inputFileAsFile);
        if (inputFile == null) {
            throw new OptionsConfigurationException(
                    "Could not determine full filename path for " + key);
        }

        if (!inputFileAsFile.exists() || !inputFileAsFile.isFile() || !inputFileAsFile.canRead()) {
            throw new OptionsConfigurationException(
                    "Could not find readable " + key + " " + inputFile);
        }
    }

    /**
     * Read a TSV file into a Map[String, String].
     * @param file the file to read
     * @param skipFirstNonCommentLine if true, the first non comment line will be skipped.
     * @param keyColumn the column (0-based) to be used as the map's key
     * @param valueColumn the column (0-based) to be used as the map's value
     * @return the populated map
     * @throws IOException error reading the file
     */
    public static Map<String, String> readMapFileFromTsv(
            final File file, final boolean skipFirstNonCommentLine,
            final int keyColumn, final int valueColumn) throws IOException {
        final Map<String, String> result = new Object2ObjectOpenHashMap<String, String>();
        final int maxCol = Math.max(keyColumn, valueColumn);
        try {
            int lineNum = 0;
            for (final String line : new TextFileLineIterator(file)) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (lineNum++ == 0 && skipFirstNonCommentLine) {
                    continue;
                }
                final String[] columns = StringUtils.split(line, '\t');
                if (columns.length - 1 < maxCol) {
                    // Not enough values on this line
                    continue;
                }
                result.put(columns[keyColumn], columns[valueColumn]);
                lineNum++;
            }
        } catch (IOException e) {
            throw new IOException("Could not read map file " + filenameFromFile(file), e);
        }
        return result;
    }

    /**
     * Return a complete path for the given file. If there is an error getting the filename
     * this will return a null, not an exception.
     * @param file the file to get the filename for
     * @return the filename or null if there is a problem
     */
    public static String filenameFromFile(final File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }
}
