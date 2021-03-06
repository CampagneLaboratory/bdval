#!/bin/bash

#
# This script will combine the output from a BDVAL SGE array job into a form
# that can be used directly by the "generate final models" targets,  This is
# needed since the SGE sub-jobs all run independently of one another and only
# the zipped results are output
#
# The only input required is the output destination for the results
#

# Absolute path to this script.
SCRIPT="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
# Absolute path this script is in.
SCRIPT_DIR=`dirname $SCRIPT`

if [ "$1" = "" ]; then
    echo "No output directory specified!"
    echo "Usage: $0 output-directory"
    exit 1
fi

# Base name of the BDVAL project file without the ".xml" (i.e., prostate-example, maqcii-c, etc.)
ANT_PROJECT_NAME=@PROJECT@
SGE_JOB_NAME=@JOB-NAME@

# directories to extract results from
BASE_DIR=${SCRIPT_DIR}/../${SGE_JOB_NAME}
CONFIG_DIR=${BASE_DIR}/config
DATA_DIR=${BASE_DIR}/data
RESULTS_DIR=${BASE_DIR}/../${SGE_JOB_NAME}-results

# corresponding directories to write results to
OUTPUT_BASE_DIR=$1
OUTPUT_CONFIG_DIR=${OUTPUT_BASE_DIR}/config
OUTPUT_DATA_DIR=${OUTPUT_BASE_DIR}/data
OUTPUT_RESULTS_DIR=${OUTPUT_DATA_DIR}/results

/bin/mkdir -p ${OUTPUT_BASE_DIR} ${OUTPUT_DATA_DIR} ${OUTPUT_RESULTS_DIR} ${OUTPUT_CONFIG_DIR}

# Copy the files used to execute the runs
/bin/cp -r ${BASE_DIR}/bdval.jar ${BASE_DIR}/buildsupport ${OUTPUT_BASE_DIR}
/bin/cp ${CONFIG_DIR}/${ANT_PROJECT_NAME}-local.properties ${CONFIG_DIR}/log4j.properties ${CONFIG_DIR}/RConnectionPool.xml ${OUTPUT_CONFIG_DIR}
/bin/cp -r ${DATA_DIR}/* ${OUTPUT_DATA_DIR}

# extract all the features, models and predictions along with the model conditions
/bin/mkdir -p ${OUTPUT_RESULTS_DIR}/features ${OUTPUT_RESULTS_DIR}/models ${OUTPUT_RESULTS_DIR}/predictions

function createCombinedFile {
NAME=$1
if [ -z "$COMBINED_MAQC_STATS_OUTPUT" ]; then
  COMBINED_MAQC_STATS_OUTPUT=${OUTPUT_BASE_DIR}/${NAME}-all-maqc-stats.tsv
  echo >${COMBINED_MAQC_STATS_OUTPUT} "OrganizationCode	DatasetCode	EndpointCode	ExcelColumnHeader	MCC	Accuracy	Sensitivity	Specificity	AUC	RMSE	MCC_StdDev	Accuracy_StdDev	Sensitivity_StdDev	Specificity_StdDev	AUC_StdDev	RMSE_StdDev	SummaryNormalization	FeatureSelectionMethod	NumberOfFeatureUsed	ClassificationAlgorithm	BatchEffectRemovalMethod	InternalValidation	ValidationIterations	ModelId	Model-Series-Id	Label	combinedPerformance	prec	prec_StdDev	rec	rec_StdDev	F-1	F-1_StdDev	MCC	MCC_StdDev	binary-auc	binary-auc_StdDev"
fi
}

for ZIPFILE in ${RESULTS_DIR}/*.zip; do
    echo Processing ${ZIPFILE}

    # get the file name without the .zip part
    FILE=`basename ${ZIPFILE} .zip`

    # get the basedir of the zip contents (everything before the last "-")
    NAME=${FILE%-*}

    createCombinedFile $NAME

    # get the run number (everything after the last "-")
    RUN=${FILE##*-}

    # copy the property file used for the run
    /bin/cp ${SGE_JOB_NAME}-${RUN}.properties ${OUTPUT_RESULTS_DIR}/${NAME}-${RUN}.properties

    # test for existence of model conditions file
    /usr/bin/unzip -t -qq ${ZIPFILE} ${NAME}/model-conditions.txt &> /dev/null
    # proceed only if model-conditions.txt exists in zip file
    if [ $? -eq 0 ]; then
        # extract all the features, models and predictions
        /usr/bin/unzip -q ${ZIPFILE} "${NAME}/consensus-features/*" "${NAME}/features/*" \
            "${NAME}/final-models/*" "${NAME}/models/*" "${NAME}/predictions/*" \
            -d ${OUTPUT_RESULTS_DIR}

        # move files around so they are all together
        # source files: ${OUTPUT_RESULTS_DIR}/${NAME}/${RESULT}/*
        # destination: ${OUTPUT_RESULTS_DIR}/${RESULT}/
        for RESULT in consensus-features features final-models models predictions; do
            for ENDPOINT_DIR in ${OUTPUT_RESULTS_DIR}/${NAME}/${RESULT}/*; do
                ENDPOINT=`basename ${ENDPOINT_DIR}`
                /bin/mkdir -p ${OUTPUT_RESULTS_DIR}/${RESULT}/${ENDPOINT}
                /usr/bin/find ${ENDPOINT_DIR} -mindepth 1 -maxdepth 1 -exec /bin/mv {} ${OUTPUT_RESULTS_DIR}/${RESULT}/${ENDPOINT} \;

                # if the rmdir fails we know that not everything was processed
                /bin/rmdir ${ENDPOINT_DIR}
            done
            /bin/rmdir ${OUTPUT_RESULTS_DIR}/${NAME}/${RESULT}
        done
        /bin/rmdir ${OUTPUT_RESULTS_DIR}/${NAME}

        # Extract the maqc stats file
        /usr/bin/unzip -p ${ZIPFILE} ${NAME}/${NAME}-all-maqcii-submission.txt > ${OUTPUT_RESULTS_DIR}/${NAME}-${RUN}-maqcii-submission.txt
        grep -v OrganizationCode ${OUTPUT_RESULTS_DIR}/${NAME}-${RUN}-maqcii-submission.txt >> ${COMBINED_MAQC_STATS_OUTPUT}

        # extract the model conditions (replacing the temporary paths used for SGE)
        /usr/bin/unzip -p ${ZIPFILE} ${NAME}/${NAME}-README.txt > ${OUTPUT_RESULTS_DIR}/${NAME}-${RUN}-README.txt
        /usr/bin/unzip -p ${ZIPFILE} ${NAME}/model-conditions.txt \
            | sed -e "s|properties=.*/${ANT_PROJECT_NAME}.properties|properties=${ANT_PROJECT_NAME}.properties|g" \
            | sed -e "s|sequence-file=.*/sequences|sequence-file=sequences|g" \
                >> ${OUTPUT_DATA_DIR}/model-conditions.txt
    else
        echo Skipped ${ZIPFILE} since no model-conditions file was found
    fi
done
