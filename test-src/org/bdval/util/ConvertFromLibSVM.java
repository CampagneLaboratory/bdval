
package org.bdval.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Convert a libSVM datafile to .Tmm and .cids formats.
 * Created by fac2003 on 5/27/15.
 */
public class ConvertFromLibSVM {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("You must enter one argument: the name of the input libSVM data file.");
            System.exit(1);
        }

        String inputFilename = args[0];
        String basename = FilenameUtils.getBaseName(inputFilename);
        ConvertFromLibSVM converter = new ConvertFromLibSVM();

        Dataset dataset = converter.scanFeatures(inputFilename);
        converter.writeTmm(dataset, inputFilename, basename + "-to-transpose.tmm");
        converter.writeCids(dataset, inputFilename, basename + ".cids");
        converter.writePlatform(dataset, inputFilename, basename + ".platform");
        System.out.println("done");
        System.exit(0);
    }

    private void writePlatform(Dataset dataset, String inputFilename, String outputFilename) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(outputFilename));
            writer.append("#ID\n" +
                    "#GB_LIST\n" +
                    "!platform_table_begin\n" +
                    "ID    GB_LIST\n");
            for (String featureName : dataset.featureNames) {
                               writer.append(featureName);
                writer.append("\n");
            }
            writer.append("!platform_table_end");
            writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            IOUtils.closeQuietly(writer);
        }

    }

    private void writeCids(Dataset dataset, String inputFilename, String outputFilename) {

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(outputFilename));
            writer.append("#label\tsampleId\n");
            int labelIndex = 0;
            for (String recordName : dataset.recordNames) {
                writer.append(Double.toString(dataset.labels.get(labelIndex++)));
                writer.append("\t");
                writer.append(recordName);
                writer.append("\n");
            }
            writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            IOUtils.closeQuietly(writer);
        }

    }

    private void writeTmm(Dataset dataset, String inputFilename, String outputFilename) {
        BufferedReader conditionReader = null;
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(outputFilename));
            conditionReader = new BufferedReader(new FileReader(inputFilename));
            int lineNumber = 0;
            String line;
            Object2DoubleAVLTreeMap lineFeatureValues = new Object2DoubleAVLTreeMap();
            Iterator<String> recordNameIterator = dataset.recordNames.iterator();
            writer.append("ID_REF");
            for (String featureId : dataset.featureNames) {
                writer.append("\t");
                writer.append(featureId);
            }
            writer.append("\n");
            while ((line = conditionReader.readLine()) != null) {
                String tokens[] = line.split("[\\s]+");
                lineFeatureValues.clear();
                String recordName = recordNameIterator.next();
                for (int i = 1; i < tokens.length; i++) {
                    int colonIndex = tokens[i].indexOf(':');
                    if (colonIndex != -1) {
                        String featureId = tokens[i].substring(0, colonIndex);
                        String featureToken = tokens[i].substring(colonIndex + 1, tokens[i].length());
                        double featureValue = Double.parseDouble(featureToken);
                        lineFeatureValues.put(featureId, featureValue);
                    }
                }
                StringBuffer lineValue = new StringBuffer();
                lineValue.append(recordName);
                lineValue.append("\t");
                for (String featureName : dataset.featureNames) {
                    lineValue.append(Double.toString(lineFeatureValues.getDouble(featureName)));
                    lineValue.append("\t");
                }
                writer.append(lineValue.subSequence(0, lineValue.length() - 1));
                writer.append("\n");
                writer.flush();
                lineNumber++;
            }
            writer.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(conditionReader);
            IOUtils.closeQuietly(writer);
        }
    }

    class Dataset {
        public Dataset() {
            featureNames = new ObjectAVLTreeSet<String>();
            recordNames = new ArrayList<String>();
            labels = new DoubleArrayList();
        }

        SortedSet<String> featureNames;
        DoubleList labels;
        List<String> recordNames;
    }

    private Dataset scanFeatures(String inputFilename) {
        BufferedReader conditionReader = null;
        Dataset set = new Dataset();

        try {
            conditionReader = new BufferedReader(new FileReader(inputFilename));
            int lineNumber = 0;
            String line;
            while ((line = conditionReader.readLine()) != null) {
                String tokens[] = line.split("[\\s]+");
                set.labels.add(Double.parseDouble(tokens[0]));
                for (int i = 1; i < tokens.length; i++) {
                    int colonIndex = tokens[i].indexOf(':');
                    if (colonIndex != -1) {
                        String featureId = tokens[i].substring(0, colonIndex);
                        set.featureNames.add(featureId);
                    }
                }
                set.recordNames.add(Integer.toString(lineNumber));
                lineNumber++;
            }
            return set;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(conditionReader);
        }
        return null;
    }
}
