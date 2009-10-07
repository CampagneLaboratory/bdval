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

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import edu.cornell.med.icb.geo.tools.FileGeneList;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.learning.tools.svmlight.EvaluationMeasure;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 * Helper class to format submissions for MAQCII.
 *
 * @author Kevin C. Dorff
 * @author Fabien Campagne (extracted from CrossValidationMode)
 */
public class MaqciiHelper {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(MaqciiHelper.class);
    private String featureSelectionMethod;
    private String label;
    private boolean outputBinaryMeasures = true;
    private String[] otherMeasureNames;
    private String modelId;
    private String endpointCode;

    public void setupSubmissionFile(final JSAPResult result, final DAVOptions options,
                                    final String label) {
        outputBinaryMeasures = result.getBoolean("binary");

        options.submissionFilename = result.getString("submission-file");
        if (StringUtils.isBlank(options.submissionFilename)
                || options.submissionFilename.equals("-")) {
            options.submissionFilename = null;
            options.submissionOutput = null;
        }
        if (options.submissionFilename != null) {
            try {
                final File submissionFile = new File(options.submissionFilename);
                options.submissionFilePreexist = submissionFile.exists();
                options.submissionOutput = new PrintWriter(
                        new FileWriter(options.submissionFilename, true));
            } catch (IOException e) {
                LOG.error("Could not create MAQC-II submission file named "
                        + options.submissionFilename + " - submission file will not be written.");
                options.submissionOutput = null;
            }
        }
        this.label = label;
        this.featureSelectionMethod = getFeatureSelectionCode(label);
        final String otherOptions = result.getString("other-measures");
        final ObjectSet<String> otherMeasures = new ObjectArraySet<String>();

        for (final String name : otherOptions.split("[,]")) {
            if (name.trim().length() > 0) {
                otherMeasures.add(name.trim());
                LOG.info("Will evaluate additional performance measure: " + name.trim());
            }

        }
        this.otherMeasureNames = otherMeasures.toArray(new String[otherMeasures.size()]);
    }

    public void defineSubmissionFileOption(final JSAP jsap) throws JSAPException {
        final Parameter submissionFileParam =
                new FlaggedOption("submission-file")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setDefault("-")
                        .setRequired(false)
                        .setLongFlag("submission-file")
                        .setHelp("The MAQC-II submission file to create. Please note that "
                                + "this file lacks some columns required for MAQCII submission. "
                                + "These columns must be created manually in excel.");
        jsap.registerParameter(submissionFileParam);


        final Parameter otherMeasureNamesOption = new FlaggedOption("other-measures")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault("")
                .setRequired(false)
                .setLongFlag("other-measures")
                .setHelp("A list of performance measures to evaluate and report in the "
                        + "maqcii file. These measures will be appended at the end of the "
                        + "columns, after the official maqcii submission columns.");
        jsap.registerParameter(otherMeasureNamesOption);

        final Parameter labelOption = new FlaggedOption("label")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(JSAP.NO_DEFAULT)
                .setRequired(true)
                .setLongFlag("label")
                .setHelp("A string that the type of model construction process used to generate "
                        + "the models.");
        jsap.registerParameter(labelOption);


        final Parameter binaryOption = new FlaggedOption("binary")
                .setStringParser(JSAP.BOOLEAN_PARSER)
                .setDefault("false")
                .setRequired(false)
                .setLongFlag("binary")
                .setHelp("Indicates that binary decision values (-1/+1) should be used "
                        + "to evaluate the binary flavor of evaluation measures in "
                        + "addition to the traditional evaluation measures.");
        jsap.registerParameter(binaryOption);
    }

    public void setFeatureSelectionMethod(final String featureSelectionMethod) {
        this.featureSelectionMethod = featureSelectionMethod;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public void setModelId(final String modelId) {
        this.modelId = modelId;
    }

    public void setEndpointCode(final String endpointCodeVal) {
        this.endpointCode = endpointCodeVal;
    }

    public enum MaqciiDataSetDetails {
        HamnerWithControl(1, "Hamner", "A", "Class_NT_NLT", "C"),
        Iconix(2, "Iconix", "B", "Class", "B"),
        NIEHS(3, "NIEHS", "C", "Class", "C"),
        MDACC_PCR(4, "BR", "D", "pCR", "O"),
        MDACC_ERPOS(5, "BR", "E", "erpos", "H"),
        UAMS_OS_MO(6, "MM", "F", "OS_MO", "AB"),
        UAMS_EFS_MO(7, "MM", "G", "EFS_MO", "AA"),
        UAMS_CPS1(8, "MM", "H", "CPS1", "S"),
        UAMS_CPR1(9, "MM", "I", "CPR1", "T"),
        Cologne_OS_MO(10, "NB", "J", "OS_MO", "AM"),
        Cologne_EFS_MO(11, "NB", "K", "EFS_MO", "AL"),
        Cologne_NEP_S(12, "NB", "L", "NEP_S", "AN"),
        Cologne_NEP_R(13, "NB", "M", "NEP_R", "AO"),
        CologneTraining(14, "NB", "N", "COLOGNE_TRAINING", "AO"),
        UAMS_CPR1Training(15, "MM", "I", "CPR1", "T"),
        NIEHSTraining(16, "NIEHS", "C", "Class", "C"),

        IconixSwap(17, "IconixSwap", "B", "Class", "B"),
        NIEHSSwap(18, "NIEHSSwap", "C", "Class", "C"),
        MDACC_PCRSwap(19, "MDACC_PCRSwap", "D", "pCR", "O"),
        MDACC_ERPOSSwap(20, "MDACC_ERPOSSwap", "E", "erpos", "H"),
        UAMS_OS_MOSwap(21, "UAMS_OS_MOSwap", "F", "OS_MO", "AB"),
        UAMS_EFS_MOSwap(22, "UAMS_EFS_MOSwap", "G", "EFS_MO", "AA"),
        UAMS_CPS1Swap(23, "UAMS_CPS1Swap", "H", "CPS1", "S"),
        UAMS_CPR1Swap(24, "UAMS_CPR1Swap", "I", "CPR1", "T"),
        Cologne_OS_MOSwap(25, "Cologne_OS_MOSwap", "J", "OS_MO", "AM"),
        Cologne_EFS_MOSwap(26, "Cologne_EFS_MOSwap", "K", "EFS_MO", "AL"),
        Cologne_NEP_SSwap(27, "Cologne_NEP_SSwap", "L", "NEP_S", "AN"),
        Cologne_NEP_RSwap(28, "Cologne_NEP_RSwap", "M", "NEP_R", "AO"),
        CologneTrainingSwap(29, "CologneTrainingSwap", "N", "COLOGNE_TRAINING", "AO"),
        UAMS_CPR1TrainingSwap(30, "UAMS_CPR1TrainingSwap", "I", "CPR1", "T"),
        NIEHSTrainingSwap(31, "NIEHSTrainingSwap", "C", "Class", "C"),
        HamnerWithControlSwap(32, "HamnerWithControlSwap", "A", "Class_NT_NLT", "C"),

        Unknown(32, "UNKNOWN", "Z", "UNKNOWN", "XX");

        public final int number;
        public final String dataSetCode;
        public final String endpointCode;
        public final String excelColumnHeader;
        public final String excelColumn;
        private final String nameLowercase;

        MaqciiDataSetDetails(
                final int number,
                final String dataSetCode,
                final String endpointCode,
                final String excelColumnHeader,
                final String excelColumn) {

            this.number = number;
            this.dataSetCode = dataSetCode;
            this.endpointCode = endpointCode;
            this.excelColumnHeader = excelColumnHeader;
            this.excelColumn = excelColumn;
            nameLowercase = name().toLowerCase();
        }

        public static MaqciiDataSetDetails getByName(final String datasetName) {
            if (StringUtils.isBlank(datasetName)) {
                return Unknown;
            }
            String requestLc = datasetName.toLowerCase();
            if (requestLc.endsWith("-short")) {
                requestLc = requestLc.substring(0, requestLc.length() - "-short".length());
            }
            for (final MaqciiDataSetDetails item : MaqciiDataSetDetails.values()) {
                if (item.nameLowercase.equals(requestLc)) {
                    return item;
                }
            }
            return Unknown;
        }

        public static MaqciiDataSetDetails getByEndpointCode(final String endpointCodeVal) {
            if (StringUtils.isBlank(endpointCodeVal)) {
                return Unknown;
            }
            for (final MaqciiDataSetDetails item : MaqciiDataSetDetails.values()) {
                if (item.endpointCode.equals(endpointCodeVal)) {
                    return item;
                }
            }
            return Unknown;
        }
    }

    public void printSubmissionHeaders(final DAVOptions options) {
        printSubmissionHeaders(options, false);
    }

    public void printSubmissionHeaders(final DAVOptions options, final Boolean printSurvival) {

        if (options.submissionFilePreexist || options.submissionOutput == null) {
            return;
        }

        options.submissionOutput.print("OrganizationCode");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("DatasetCode");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("EndpointCode");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("ExcelColumnHeader");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("MCC");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("Accuracy");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("Sensitivity");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("Specificity");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("AUC");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("RMSE");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("MCC_StdDev");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("Accuracy_StdDev");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("Sensitivity_StdDev");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("Specificity_StdDev");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("AUC_StdDev");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("RMSE_StdDev");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("SummaryNormalization");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("FeatureSelectionMethod");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("NumberOfFeatureUsed");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("ClassificationAlgorithm");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("BatchEffectRemovalMethod");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("InternalValidation");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("ValidationIterations");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("ModelId");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("Label");
        options.submissionOutput.print("\t");
        options.submissionOutput.print("combinedPerformance"); // MCC+ AUC - MCC_std - AUC_std

        if (outputBinaryMeasures) {
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-MCC");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-Accuracy");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-Sensitivity");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-Specificity");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-AUC");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-RMSE");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-MCC_StdDev");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-Accuracy_StdDev");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-Sensitivity_StdDev");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-Specificity_StdDev");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-AUC_StdDev");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("binary-RMSE_StdDev");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("MCC-thresholdIndependent");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("MCC-thresholdIndependent_StdDev");
        }
        for (final String otherMeasureName : otherMeasureNames) {
            options.submissionOutput.print("\t");
            options.submissionOutput.print(otherMeasureName);
            options.submissionOutput.print("\t");
            options.submissionOutput.print(otherMeasureName + "_StdDev");
        }
        if (printSurvival) {
            options.submissionOutput.print("\t");
            options.submissionOutput.print("survival_coxP");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("survival_hazardRatio");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("survival_lowCI");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("survival_upCI");
            options.submissionOutput.print("\t");
            options.submissionOutput.print("survival_logRankP");
        }
        options.submissionOutput.print("\n");
        options.submissionOutput.flush();
        options.submissionFilePreexist = new File(options.submissionFilename).exists();
                
    }

    public void printSubmissionHeaders(boolean submissionFilePreexist,
                                       boolean outputBinaryMeasures,
                                       PrintWriter submissionOutput,
                                       final Boolean printSurvival) {
        if (submissionFilePreexist || submissionOutput == null) {
            return;
        }
        submissionOutput.print("OrganizationCode");
        submissionOutput.print("\t");
        submissionOutput.print("DatasetCode");
        submissionOutput.print("\t");
        submissionOutput.print("EndpointCode");
        submissionOutput.print("\t");
        submissionOutput.print("ExcelColumnHeader");
        submissionOutput.print("\t");
        submissionOutput.print("MCC");
        submissionOutput.print("\t");
        submissionOutput.print("Accuracy");
        submissionOutput.print("\t");
        submissionOutput.print("Sensitivity");
        submissionOutput.print("\t");
        submissionOutput.print("Specificity");
        submissionOutput.print("\t");
        submissionOutput.print("AUC");
        submissionOutput.print("\t");
        submissionOutput.print("RMSE");
        submissionOutput.print("\t");
        submissionOutput.print("MCC_StdDev");
        submissionOutput.print("\t");
        submissionOutput.print("Accuracy_StdDev");
        submissionOutput.print("\t");
        submissionOutput.print("Sensitivity_StdDev");
        submissionOutput.print("\t");
        submissionOutput.print("Specificity_StdDev");
        submissionOutput.print("\t");
        submissionOutput.print("AUC_StdDev");
        submissionOutput.print("\t");
        submissionOutput.print("RMSE_StdDev");
        submissionOutput.print("\t");
        submissionOutput.print("SummaryNormalization");
        submissionOutput.print("\t");
        submissionOutput.print("FeatureSelectionMethod");
        submissionOutput.print("\t");
        submissionOutput.print("NumberOfFeatureUsed");
        submissionOutput.print("\t");
        submissionOutput.print("ClassificationAlgorithm");
        submissionOutput.print("\t");
        submissionOutput.print("BatchEffectRemovalMethod");
        submissionOutput.print("\t");
        submissionOutput.print("InternalValidation");
        submissionOutput.print("\t");
        submissionOutput.print("ValidationIterations");
        submissionOutput.print("\t");
        submissionOutput.print("ModelId");
        submissionOutput.print("\t");
        submissionOutput.print("Label");
        submissionOutput.print("\t");
        submissionOutput.print("combinedPerformance"); // MCC+ AUC - MCC_std - AUC_std

        if (outputBinaryMeasures) {
            submissionOutput.print("\t");
            submissionOutput.print("binary-MCC");
            submissionOutput.print("\t");
            submissionOutput.print("binary-Accuracy");
            submissionOutput.print("\t");
            submissionOutput.print("binary-Sensitivity");
            submissionOutput.print("\t");
            submissionOutput.print("binary-Specificity");
            submissionOutput.print("\t");
            submissionOutput.print("binary-AUC");
            submissionOutput.print("\t");
            submissionOutput.print("binary-RMSE");
            submissionOutput.print("\t");
            submissionOutput.print("binary-MCC_StdDev");
            submissionOutput.print("\t");
            submissionOutput.print("binary-Accuracy_StdDev");
            submissionOutput.print("\t");
            submissionOutput.print("binary-Sensitivity_StdDev");
            submissionOutput.print("\t");
            submissionOutput.print("binary-Specificity_StdDev");
            submissionOutput.print("\t");
            submissionOutput.print("binary-AUC_StdDev");
            submissionOutput.print("\t");
            submissionOutput.print("binary-RMSE_StdDev");
            submissionOutput.print("\t");
            submissionOutput.print("MCC-thresholdIndependent");
            submissionOutput.print("\t");
            submissionOutput.print("MCC-thresholdIndependent_StdDev");
        }
        for (final String otherMeasureName : otherMeasureNames) {
            submissionOutput.print("\t");
            submissionOutput.print(otherMeasureName);
            submissionOutput.print("\t");
            submissionOutput.print(otherMeasureName + "_StdDev");
        }
        if (printSurvival) {
            submissionOutput.print("\t");
            submissionOutput.print("survival_coxP");
            submissionOutput.print("\t");
            submissionOutput.print("survival_hazardRatio");
            submissionOutput.print("\t");
            submissionOutput.print("survival_lowCI");
            submissionOutput.print("\t");
            submissionOutput.print("survival_upCI");
            submissionOutput.print("\t");
            submissionOutput.print("survival_logRankP");
        }
        submissionOutput.print("\n");
        submissionOutput.flush();
    }

    /**
     * Get the feature selection method for the current options.
     * This will be the filename for the current genelist, ignoring
     * stuff before the first "-" and ignoring stuff after the last
     * "-", keeping everything in the middle.
     *
     * @param options
     * @return
     */
    private String getFeatureSelectionMethod(final DAVOptions options) {
        if (this.featureSelectionMethod != null) {
            return featureSelectionMethod;
        }
        String featureSelectionMethod = "UNKNOWN";
        if (options.geneLists.length > 0) {
            final GeneList currentGeneList = options.geneLists[0];
            if (currentGeneList instanceof FileGeneList) {
                final FileGeneList fileGeneList = (FileGeneList) currentGeneList;
                final String geneFilename = FilenameUtils.getBaseName(fileGeneList.getFilename());
                final String[] parts = StringUtils.split(geneFilename, "-");
                if (parts.length >= 3) {
                    featureSelectionMethod = "";
                    for (int pos = 1; pos < parts.length - 1; pos++) {
                        final String curPart = parts[pos];
                        if (pos != 1) {
                            featureSelectionMethod += "-";
                        }
                        featureSelectionMethod += curPart;
                    }
                }
            }
        }
        return featureSelectionMethod;
    }

    public void printSubmissionResults(final DAVOptions options,
                                       final EvaluationMeasure measure,
                                       final int numberOfFeatures, final int numRepeats) {
        printSubmissionResults(options, measure, numberOfFeatures, numRepeats, null);

    }

    public void printSubmissionResults(final DAVOptions options,
                                       final EvaluationMeasure measure,
                                       final int numberOfFeatures, final int numRepeats, final List<SurvivalMeasures> survivalMeasuresList) {
        if (options.submissionOutput == null) {
            LOG.info("Omitting submission output, submission-file not specified.");
            return;
        }

        MaqciiDataSetDetails details =
                MaqciiHelper.MaqciiDataSetDetails.getByName(options.datasetName);
        if (details == MaqciiHelper.MaqciiDataSetDetails.Unknown && endpointCode != null) {
            details = MaqciiHelper.MaqciiDataSetDetails.getByEndpointCode(endpointCode);
        }

        final String featureSelectionMethod = getFeatureSelectionMethod(options);
        LOG.debug(String.format("Submission line for dataset=%s, featureSelection=%s",
                options.datasetName, featureSelectionMethod));

        options.submissionOutput.print("Cornell");    //OrganizationCode
        options.submissionOutput.print("\t");
        options.submissionOutput.print(((details == MaqciiDataSetDetails.Unknown) ? options.datasetName :
                details.dataSetCode));    //DatasetCode
        options.submissionOutput.print("\t");
        options.submissionOutput.print(details.endpointCode);    //EndpointCode
        options.submissionOutput.print("\t");
        options.submissionOutput.print(details.excelColumnHeader);    //ExcelColumnHeader
        options.submissionOutput.print("\t");
        options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("mat")));    //MCC
        options.submissionOutput.print("\t");
        options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("acc")));    //Accuracy
        options.submissionOutput.print("\t");
        options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("sens")));    //Sensitivity
        options.submissionOutput.print("\t");
        options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("spec")));    //Specificity
        options.submissionOutput.print("\t");
        options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("auc")));    //AUC
        options.submissionOutput.print("\t");
        options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("rmse")));    //RMSE
        options.submissionOutput.print("\t");
        options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("mat")));    //MCC_StdDev
        options.submissionOutput.print("\t");
        options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("acc")));    //Accuracy_StdDev
        options.submissionOutput.print("\t");
        options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("sens")));    //Sensitivity_StdDev
        options.submissionOutput.print("\t");
        options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("spec")));    //Specificity_StdDev
        options.submissionOutput.print("\t");
        options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("auc")));    //AUC_StdDev
        options.submissionOutput.print("\t");
        options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("rmse")));    //RMSE_StdDev
        options.submissionOutput.print("\t");
        options.submissionOutput.print("N/A");    //SummaryNormalization
        options.submissionOutput.print("\t");
        options.submissionOutput.print(featureSelectionMethod);    //FeatureSelectionMethod
        options.submissionOutput.print("\t");
        options.submissionOutput.print(Integer.toString(numberOfFeatures));    //NumberOfFeatureUsed
        options.submissionOutput.print("\t");
        options.submissionOutput.print("SVM");    //ClassificationAlgorithm
        options.submissionOutput.print("\t");
        options.submissionOutput.print("N/A");    //BatchEffectRemovalMethod
        options.submissionOutput.print("\t");
        options.submissionOutput.print(Integer.toString(options.crossValidationFoldNumber) + "-CV");    //InternalValidation
        options.submissionOutput.print("\t");
        options.submissionOutput.print(Integer.toString(numRepeats));    //ValidationIteration
        options.submissionOutput.print("\t");
        if (this.modelId != null) {
            LOG.trace("Using helper's modelId");
            options.submissionOutput.print(this.modelId);    //ModelID
        } else {
            LOG.trace("Using option's modelId");
            options.submissionOutput.print(options.modelId);    //ModelID
        }
        options.submissionOutput.print("\t");
        options.submissionOutput.print(label);    //Label
        options.submissionOutput.print("\t");
        options.submissionOutput.print(valueToString(getCompositeMeasure(measure), 4));    // auc+mcc -auc_std -mcc_std
        if (outputBinaryMeasures) {
            options.submissionOutput.print("\t");
            options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("binary-mat")));    //MCC
            options.submissionOutput.print("\t");
            options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("binary-acc")));    //Accuracy
            options.submissionOutput.print("\t");
            options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("binary-sens")));    //Sensitivity
            options.submissionOutput.print("\t");
            options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("binary-spec")));    //Specificity
            options.submissionOutput.print("\t");
            options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("binary-auc")));    //AUC
            options.submissionOutput.print("\t");
            options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("binary-rmse")));    //RMSE
            options.submissionOutput.print("\t");
            options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("binary-mat")));    //MCC_StdDev
            options.submissionOutput.print("\t");
            options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("binary-acc")));    //Accuracy_StdDev
            options.submissionOutput.print("\t");
            options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("binary-sens")));    //Sensitivity_StdDev
            options.submissionOutput.print("\t");
            options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("binary-spec")));    //Specificity_StdDev
            options.submissionOutput.print("\t");
            options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("binary-auc")));    //AUC_StdDev
            options.submissionOutput.print("\t");
            options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("binary-rmse")));    //RMSE_StdDev
            options.submissionOutput.print("\t");
            options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage("MCC")));    //MCC-threshold independent
            options.submissionOutput.print("\t");
            options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd("MCC")));    //MCC-threshold independent_StdDev

        }

        for (final String otherMeasureName : otherMeasureNames) {
            options.submissionOutput.print("\t");
            options.submissionOutput.print(valueToString(measure.getPerformanceValueAverage(otherMeasureName)));
            options.submissionOutput.print("\t");
            options.submissionOutput.print(stdDevToString(measure.getPerformanceValueStd(otherMeasureName)));
        }

        if (survivalMeasuresList != null && survivalMeasuresList.size() != 0) {
            // index is the covariate; decision score is the last one, which is at filterSamples() in SurvivalMeasures.java
            final int scoreIndex = survivalMeasuresList.get(0).coxP.length - 1;
            final SurvivalMeasureResult survivalMeasureResult =
                    averageSurvivalMeasuresList(survivalMeasuresList, scoreIndex);

            final DecimalFormat decimalFormat = new DecimalFormat("##.###");
            final DecimalFormat scientificFormat = new DecimalFormat("0.##E0");
            options.submissionOutput.print("\t");
            options.submissionOutput.print(scientificFormat.format(survivalMeasureResult.coxP));
            options.submissionOutput.print("\t");
            options.submissionOutput.print(decimalFormat.format(survivalMeasureResult.hazardRatio));//the last hr value is the score
            options.submissionOutput.print("\t");
            options.submissionOutput.print(decimalFormat.format(survivalMeasureResult.lowCI));//the last lowCi value is the score
            options.submissionOutput.print("\t");
            options.submissionOutput.print(decimalFormat.format(survivalMeasureResult.upCI));//the last up value is the score
            options.submissionOutput.print("\t");
            options.submissionOutput.print(scientificFormat.format(survivalMeasureResult.logRankP));//the last up value is the score

        }
        options.submissionOutput.print("\n");
        options.submissionOutput.flush();
    }

    private double stouffer(final double[] pval) {
        double rval = 1.0;
        final NormalDistribution normd = new NormalDistributionImpl();
        normd.setMean(0);
        normd.setStandardDeviation(1);
        final int size = pval.length;
        final double[] z = new double[size];
        double zCombine = 0;

        for (int i = 0; i < size; i++) {
            try {
                z[i] = normd.inverseCumulativeProbability(pval[i]);
            } catch (MathException e) {
                LOG.warn("Error calculating probability", e);
            }
            zCombine += z[i];
        }
        zCombine /= Math.sqrt(size);

        try {
            rval = normd.cumulativeProbability(zCombine);
        } catch (MathException e) {
        }

        return rval;
    }

    //index is the covariate; decision score is the last one
    private SurvivalMeasureResult averageSurvivalMeasuresList(
            final List<SurvivalMeasures> survivalMeasuresList, final int index) {
        final SurvivalMeasureResult survivalMeasureResult = new SurvivalMeasureResult();

        double h = 0;
        double lo = 0;
        double up = 0;
        final int numRepeat = survivalMeasuresList.size(); //number of repeat of CV in the list
        final double[] coxpVector = new double[numRepeat];
        final double[] logpVector = new double[numRepeat];
        for (int i = 0; i < numRepeat; i++) {
            logpVector[i] = survivalMeasuresList.get(i).logRankP;
            coxpVector[i] = survivalMeasuresList.get(i).coxP[index];
            h += survivalMeasuresList.get(i).hazardRatio[index];
            lo += survivalMeasuresList.get(i).lowCI[index];
            up += survivalMeasuresList.get(i).upCI[index];
        }

        final double stoufferCoxP = stouffer(coxpVector);
        final double stoufferLogRankP = stouffer(logpVector);
        survivalMeasureResult.assignValue(stoufferCoxP, h / numRepeat, lo / numRepeat,
                up / numRepeat, stoufferLogRankP);
        return survivalMeasureResult;
    }

    private double getCompositeMeasure(final EvaluationMeasure measure) {
        final double mcc = measure.getPerformanceValueAverage("mat");
        final double mccStd = measure.getPerformanceValueStd("mat");
        final double auc = measure.getPerformanceValueAverage("auc");
        final double aucStd = measure.getPerformanceValueStd("auc");
        return mcc + auc - mccStd - aucStd;
    }

    static final NumberFormat formatter = new DecimalFormat();

    private String valueToString(final double value) {
        return valueToString(value, 2);
    }

    private String stdDevToString(final double value) {
        return valueToString(value, 4);
    }

    private synchronized String valueToString(final double value, final int numberOfDigits) {
        if (Double.isNaN(value)) {
            return "NaN";
        } else {
            formatter.setMaximumFractionDigits(numberOfDigits);
            return formatter.format(value);
        }
    }

    /**
     * Delete the submission file if specified and it exists.
     *
     * @param result the parsed command line options
     */
    public void deletePreExistingSubmissionFile(final JSAPResult result) {
        final String submissionFile = result.getString("submission-file");
        if (submissionFile != null) {
            final File submissionFileFile = new File(submissionFile);
            if (submissionFileFile.exists()) {
                submissionFileFile.delete();
            }
        }
    }

    public void setupSubmissionFile(final JSAPResult result, final DAVOptions options) {
        setupSubmissionFile(result, options, result.getString("label"));
    }


    private String getFeatureSelectionCode(final String label) {
        if (label.contains("genelists")) {
            return "Genelists";
        }
        if (label.contains("genetic-algorithm")) {
            return "GeneticAlgorithm";
        }
        if (label.contains("iterative")) {
            return "RFE";
        }
        if (label.contains("global-svm-weights")) {
            return "SVM-Weights";
        }
        if (label.contains("pathways")) {
            return "Pathways";
        }
        if (label.contains("ttest")) {
            return "T-Test";
        }
        if (label.contains("foldchange")) {
            return "FC";
        }
        if (label.contains("minmax")) {
            return "MinMax";
        } else {
            return "None";
        }
    }
}
