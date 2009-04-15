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

package edu.cornell.med.icb.geo;

import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.LineIterator;
import it.unimi.dsi.lang.MutableString;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * An efficient parser for the GEO Soft family file format. Files in this format
 * consists of the following sections: <LI>DATABASE (1)</LI> <LI>PLATFORM
 * (1)</LI> <LI>SAMPLE  (1+)</LI> This parser performs a single pass over the
 * input file and exposes data read in each section as objects. This parser aims
 * to minimize parsing time and memory consumption, so that even very large
 * files (2Gb+) can be processed.
 *
 * @author Fabien Campagne Date: Aug 15, 2007 Time: 1:11:39 PM
 */
public class GeoSoftFamilyParser {
    private LineIterator lineIterator;
    private MutableString currentSectionType;


    private enum PositionInInputFile {
        UNKNOWN,
        START_OF_SECTION,
        AFTER_SECTION_PROPERTIES,
    }

    private enum CurrentSectionType {
        DATABASE_SECTION,
        SERIES_SECTION,
        PLATFORM_SECTION,
        SAMPLE_SECTION,
        OTHER_SECTION,
    }

    PositionInInputFile readerState;
    CurrentSectionType currentSection;

    /**
     * Construct a parser over the file.
     *
     * @param filename Name of the file to parse.
     * @throws IOException If an error occurs reading the file.
     */
    public GeoSoftFamilyParser(final String filename) throws IOException {
        super();
        setFilename(filename);
    }

    public GeoSoftFamilyParser(final Reader fileReader) {
        super();
        setReader(fileReader);
    }

    /**
     * Reinitialize the parser with a new input file.
     *
     * @param fileReader Where to read input data.
     */
    public void setReader(final Reader fileReader) {
        final FastBufferedReader reader = new FastBufferedReader(fileReader);
        lineIterator = new LineIterator(reader);
        readerState = PositionInInputFile.UNKNOWN;
    }

    public void setFilename(final String filename) throws IOException {
        Reader fileReader = null;

        if (filename.endsWith(".gz")) {
            fileReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)));
        } else if (filename.endsWith(".zip")) {
            fileReader = new InputStreamReader(new ZipInputStream(new FileInputStream(filename)));
        } else {
            fileReader = new FileReader(filename);
        }

        setReader(fileReader);
    }

    MutableString currentLine;

    /**
     * Skip to the given file section.
     *
     * @param sectionName Name of the section to skip to.
     * @return True if the section was found, false otherwise.
     */
    public boolean skipToSection(final MutableString sectionName) {
        if (tryLine(currentLine, sectionName)) {
            return true;
        }

        MutableString line;
        while (lineIterator.hasNext()) {
            line = lineIterator.next();

            if (tryLine(line, sectionName)) {
                return true;
            }
        }
        return false;
    }


    private boolean tryLine(final MutableString line, final MutableString sectionName) {
        if (line == null) {
            return false;
        }

        if (line.startsWith("^")) {
            final MutableString sectionTypeRead = line.substring(1, sectionName.length() + 1);
            if (sectionTypeRead.equalsIgnoreCase(sectionName)) {
                currentLine = line.copy();
                readerState = PositionInInputFile.START_OF_SECTION;
                this.currentSectionType = sectionTypeRead;
                if (sectionName.equals(DATABASE_SECTION_NAME)) {
                    this.currentSection = CurrentSectionType.DATABASE_SECTION;
                } else if (sectionName.equals(PLATFORM_SECTION_NAME)) {
                    this.currentSection = CurrentSectionType.PLATFORM_SECTION;
                } else if (sectionName.equals(SAMPLE_SECTION_NAME)) {
                    this.currentSection = CurrentSectionType.SAMPLE_SECTION;
                }
                return true;
            }
        }
        return false;
    }

    final MutableString DATABASE_SECTION_NAME = new MutableString("Database");

    /**
     * Skip to DATABASE section of file.
     *
     * @return True if the section was found, false otherwise.
     */
    public boolean skipToDatabaseSection() {

        return skipToSection(DATABASE_SECTION_NAME);
    }

    final MutableString SERIES_SECTION_NAME = new MutableString("Series");

    /**
     * Skip to SERIES section of file.
     *
     * @return True if the section was found, false otherwise.
     */
    public boolean skipToSeriesSection() {

        return skipToSection(SERIES_SECTION_NAME);
    }


    final MutableString PLATFORM_SECTION_NAME = new MutableString("Platform");

    /**
     * Skip to PLATFORM section of file.
     *
     * @return True if the section was found, false otherwise.
     */
    public boolean skipToPlatformSection() {

        return skipToSection(PLATFORM_SECTION_NAME);
    }

    final MutableString SAMPLE_SECTION_NAME = new MutableString("Sample");

    /**
     * Skip to next SAMPLE section of file.
     *
     * @return True if the section was found, false otherwise.
     */
    public boolean skipToSampleSection() {

        return skipToSection(SAMPLE_SECTION_NAME);
    }

    /**
     * Returns whether the end of file has been encountered.
     *
     * @return True if the reader has encountered the end of the input file.
     *         False if more content can be read.
     */
    public boolean isEOF() {
        return (!lineIterator.hasNext());
    }

    /**
     * Returns the attribute that follows the section delimiter. GEO soft files
     * have section delimiters of the form <PRE> ^DATABASE = GeoMiame </PRE>
     * This method returns the string "GeoMiame" for such a section. The section
     * attribute follows the equal sign. Spaces are trimmed.
     *
     * @return The section attribute.
     */
    public MutableString getSectionAttribute() {
        if (readerState == PositionInInputFile.START_OF_SECTION) {
            final int indexEqualCharacter = currentLine.indexOf('=');
            final MutableString result = currentLine.substring(indexEqualCharacter + 1, currentLine.length()).trim();
            currentLine = null;
            return result;
        } else {
            return null;
        }
    }

    /**
     * Parses property value pairs in a section. GEO section properties are
     * organized as follows: <PRE> !Database_name = Gene Expression Omnibus
     * (GEO) !Database_institute = NCBI NLM NIH !Database_web_link =
     * http://www.ncbi.nlm.nih.gov/projects/geo !Database_property_name = value
     * !Database_property_name = value </PRE> Property names can be repeated. In
     * this case, the value is a list where each element is a value.
     *
     * @return a map where keys are property names, and values the associated
     *         value.
     */
    public SectionProperties parseSectionProperties() {
        readerState = PositionInInputFile.AFTER_SECTION_PROPERTIES;
        final SectionProperties properties = new SectionProperties();

        final MutableString propertyPrefix = new MutableString();
        propertyPrefix.append("!");
        propertyPrefix.append(currentSectionType);
        propertyPrefix.append("_");
        propertyPrefix.toLowerCase();
        MutableString line;

        while (lineIterator.hasNext()) {
            line = lineIterator.next();
            currentLine = line;
            final int indexEqualCharacter;

            if (line.startsWith("!") &&
                    line.copy().toLowerCase().startsWith(propertyPrefix) &&
                    ((indexEqualCharacter = line.indexOf('=')) != -1)) {
                // property line detected.
                final int indexUnderscoreCharacter = line.indexOf('_');
                final MutableString propertyName = line.substring(indexUnderscoreCharacter + 1, indexEqualCharacter).trim().compact();
                final MutableString value = line.substring(indexEqualCharacter + 1, line.length()).trim();
                properties.put(propertyName, value);
            } else {
                break;
            }
        }
        return properties;
    }

    public GEOPlatformIndexed parsePlatform() {
        assert currentSection == CurrentSectionType.PLATFORM_SECTION : "parse platform must be called within the platform section of the file.";
        MutableString line;
        if (currentLine != null && !currentLine.startsWith("#ID")) {
            while (lineIterator.hasNext()) {
                line = lineIterator.next();

                if (line.startsWith("#ID")) {
                    break;
                }

            }
        }
        assert lineIterator.hasNext() : "#ID cannot be last line of file";
        line = lineIterator.next();
        int indexSpaceCharacter = line.indexOf(' ');
        final MutableString typeOfExternalIdentifier = line.substring(1, indexSpaceCharacter);
        final GEOPlatformIndexed platform = new GEOPlatformIndexed();
        platform.setExternalIdType(typeOfExternalIdentifier);
        while (lineIterator.hasNext()) {
            line = lineIterator.next();
            // skip other annotations until table begin
            if (line.startsWith("!platform_table_begin")) {
                break;
            }
        }
        while (lineIterator.hasNext()) {
            line = lineIterator.next();

            if (line.startsWith("!platform_table_end")) {
                break;
            }
            //  collect id -> external id mapping lines here.
            indexSpaceCharacter = line.indexOf('\t');
            int nextIndexSpaceCharacter = line.indexOf('\t', indexSpaceCharacter + 1);
            if (nextIndexSpaceCharacter == -1) {
                nextIndexSpaceCharacter = line.length();
            }

            if (nextIndexSpaceCharacter != -1) {
                final MutableString probeId = line.substring(0, indexSpaceCharacter).copy();
                final MutableString externalId = line.substring(indexSpaceCharacter + 1, nextIndexSpaceCharacter).copy().trim();
                platform.registerProbeId(probeId, externalId);
            }
        }

        return platform;
    }

    public void parseSampleData(final GEOPlatformIndexed platform, final SampleDataCallback callback) {
        assert currentSection == CurrentSectionType.SAMPLE_SECTION : "parseSampleData must be called within the sample section of the file.";
        MutableString line;
        while (lineIterator.hasNext()) {
            line = lineIterator.next();
            // skip other annotations until table begin
            if (line.startsWith("!sample_table_begin")) {
                break;
            }
        }
        assert lineIterator.hasNext();
        callback.setColumnNames(lineIterator.next());
        if (!callback.canParse()) {
            // stop immediately if columns cannot be interpreted. Non standard, something.
            return;
        }

        while (lineIterator.hasNext()) {
            line = lineIterator.next();
            // collect sample data here

            if (line.startsWith("!sample_table_end")) {
                break;
            }
            callback.parse(line);
        }
    }

}
