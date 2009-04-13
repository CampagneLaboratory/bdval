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

package org.bdval.signalquality;

import edu.cornell.med.icb.io.TsvToFromMap;
import edu.cornell.med.icb.iterators.TextFileLineIterator;
import edu.cornell.med.icb.maps.LinkedHashToMultiTypeMap;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test the SimpleSignalQualityCalculator (specifically testing the R-Scripts).
 *
 * @author Kevin Dorff
 */
public class TestSimpleSignalQualityCalculator {

    /** Test data directory. */
    private static final String INPUT_ROOT = "test-data/quality/";

    /** Raw data read from input file. */
    private static List<LinkedHashToMultiTypeMap<String>> rawData;

    /** Raw data read from input file. */
    private static List<LinkedHashToMultiTypeMap<String>> ratioData;

    /** The signal quality calculator to use. */
    private static BaseSignalQualityCalculator calculator;

    /**
     * Verify that the data -> pvalues calculator is working correctly
     * using the data and values provided by Rikke.
     */
    @Test
    public void testDataToPValues() {
        final String[] allClasses = new String[] {"0"};
        final Map<String, Map<String, double[]>> classToDataMapMap =
                    new Object2ObjectOpenHashMap<String, Map<String, double[]>>();
        int lineNo = 1;
        for (final LinkedHashToMultiTypeMap<String> line : rawData) {
            classToDataMapMap.clear();
            final Map<String, double[]> trainingData =
                    new Object2ObjectLinkedOpenHashMap<String, double[]>();
            trainingData.put("0", line.getDoubleArray("training-data"));
            classToDataMapMap.put("0-training", trainingData);

            final Map<String, double[]> validationData =
                    new Object2ObjectLinkedOpenHashMap<String, double[]>();
            validationData.put("0", line.getDoubleArray("validation-data"));
            classToDataMapMap.put("0-validation", validationData);

            final double expectedPValue =  line.getDouble("p-value");
            final double expectedTestStat =  line.getDouble("test-statistics");
            final double expectedT1 =  line.getDouble("t1");
            final double expectedT2 =  line.getDouble("t2");
            calculator.calculatePValues(null, "test-model", allClasses, classToDataMapMap);

            final LinkedHashToMultiTypeMap<String> receivedData = calculator.getLastData();

            assertEquals("pvalue for line " + lineNo, expectedPValue,
                    receivedData.getDouble("p-value[A]"), 0.001);
            assertEquals("test-statistics for line " + lineNo, expectedTestStat,
                    receivedData.getDouble("test-statistics[A]"), 0.001);
            assertEquals("t1 for line " + lineNo, expectedT1,
                    receivedData.getDouble("t1[A]"), 0.001);
            assertEquals("t2 for line " + lineNo, expectedT2,
                    receivedData.getDouble("t2[A]"), 0.001);
            lineNo++;
        }
        assertEquals(20, lineNo - 1);
    }

    /**
     * Verify that the data -> pvalues calculator is working correctly
     * using the data and values provided by Rikke.
     */
    @Test
    public void testPValuesToQuality() {
        final String[] allClasses = new String[] {"0"};
        final List<String> featureList = new ArrayList<String>();
        final DoubleList pValuesList = new DoubleArrayList();
        final DoubleList t1sList = new DoubleArrayList();
        final DoubleList t2sList = new DoubleArrayList();
        final Map<String, DoubleList> data = new Object2ObjectOpenHashMap<String, DoubleList>();
        data.put("0-p-value", pValuesList);
        data.put("0-t1", t1sList);
        data.put("0-t2", t2sList);
        int lineNo = 1;
        for (final LinkedHashToMultiTypeMap<String> line : ratioData) {
            int count = line.getInt("count");
            final double expectedStoufferZ =  line.getDouble("Stouffer z");
            final double expectedStoufferPZ =  line.getDouble("Stouffer pz");
            final double expectedRatioRank =  line.getDouble("ratio_rank");

            featureList.clear();
            pValuesList.clear();
            t1sList.clear();
            t2sList.clear();
            for (int i = 0; i < count; i++) {
                featureList.add("feature" + i);
                pValuesList.add(rawData.get(i).getDouble("p-value"));
                t1sList.add(rawData.get(i).getDouble("t1"));
                t2sList.add(rawData.get(i).getDouble("t2"));
            }

            calculator.calculateSignalQuality("test-model", allClasses, featureList, data);
            final LinkedHashToMultiTypeMap<String> receivedData = calculator.getLastData();

            assertEquals("stouffer z for line " + lineNo, expectedStoufferZ,
                    receivedData.getDouble("stouffer z[0]"), 0.001);
            assertEquals("stouffer pz for line " + lineNo, expectedStoufferPZ,
                    receivedData.getDouble("stouffer pz[0]"), 0.001);
            assertEquals("test-statistics for line " + lineNo, expectedRatioRank,
                    receivedData.getDouble("ratio_rank[0]"), 0.001);

            lineNo++;
        }
        assertEquals(3, lineNo - 1);
    }

    /**
     * Load the data from data-to-pvalues.txt.
     * @throws IOException error reading data
     */
    @BeforeClass
    public static void loadData() throws IOException {
        rawData = new ArrayList<LinkedHashToMultiTypeMap<String>>();
        final File inputFile = new File(INPUT_ROOT + "quality-pvalues.tsv");
        TsvToFromMap tsvReader = TsvToFromMap.createFromTsvFile(inputFile);
        int lineNo = 0;
        for (final String fileLine : new TextFileLineIterator(inputFile)) {
            if (lineNo == 0 || fileLine.startsWith("#") || StringUtils.isBlank(fileLine)) {
                lineNo++;
                continue;
            }
            final LinkedHashToMultiTypeMap<String> line = tsvReader.readDataToMap(fileLine);
            if (line == null) {
                continue;
            }
            line.put("training-data", FileUtils.readFileToString(new File(
                    String.format("%sx%d.csv", INPUT_ROOT, lineNo))).trim());
            line.put("validation-data", FileUtils.readFileToString(new File(
                    String.format("%sy%d.csv", INPUT_ROOT, lineNo))).trim());
            rawData.add(line);
            lineNo++;
        }
    }

    /**
     * Load the data from ratio.txt.
     * @throws IOException error reading data
     */
    @BeforeClass
    public static void loadRatioData() throws IOException {
        ratioData = new ArrayList<LinkedHashToMultiTypeMap<String>>();
        final File inputFile = new File(INPUT_ROOT + "ratio.tsv");
        TsvToFromMap tsvReader = TsvToFromMap.createFromTsvFile(inputFile);
        int lineNo = 0;
        for (final String fileLine : new TextFileLineIterator(inputFile)) {
            if (lineNo == 0 || fileLine.startsWith("#") || StringUtils.isBlank(fileLine)) {
                lineNo++;
                continue;
            }
            final LinkedHashToMultiTypeMap<String> line = tsvReader.readDataToMap(fileLine);
            if (line == null) {
                continue;
            }
            ratioData.add(line);
            lineNo++;
        }
    }

    /**
     * Create the calculator.
     * @throws IOException error creating the calculator
     */
    @BeforeClass
    public static void createCalculator() throws IOException {
        final Properties config =  new Properties();
        config.addProperty("output", "null");
        calculator = new SimpleSignalQualityCalculator();
        calculator.configure(
                config, BaseSignalQualityCalculator.OutputFileHeader.P_VALUES, false, 1);
    }

}
