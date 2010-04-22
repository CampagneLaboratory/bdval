#!/bin/sh

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
BASE_DIR=${SCRIPT_DIR}/..
EXECUTION_DIR=${BASE_DIR}/${SGE_JOB_NAME}
RESULTS_DIR=${BASE_DIR}/${SGE_JOB_NAME}-results

OUTPUT_BASE=$1
DATA_DIR=${OUTPUT_BASE}/data
CONFIG_DIR=${OUTPUT_BASE}/config
OUTPUT_DIR=${DATA_DIR}/${SGE_JOB_NAME}-results

/bin/mkdir -p ${OUTPUT_DIR} ${CONFIG_DIR} ${OUTPUT_DIR}

# Copy the files used to exectute the runs
/bin/cp -r ${EXECUTION_DIR}/bdval.jar ${EXECUTION_DIR}/buildsupport ${OUTPUT_BASE}
/bin/cp ${EXECUTION_DIR}/config/${ANT_PROJECT_NAME}-local.properties ${EXECUTION_DIR}/config/log4j.properties ${CONFIG_DIR}
/bin/cp -r ${EXECUTION_DIR}/data/* ${OUTPUT_BASE}/data

# extract all the features, models and predictions along with the model conditions
/bin/mkdir -p ${OUTPUT_DIR}/features ${OUTPUT_DIR}/models ${OUTPUT_DIR}/predictions
for ZIPFILE in ${RESULTS_DIR}/*.zip; do
    echo Processing $ZIPFILE

    # get the file name without the .zip part
    FILE=`basename $ZIPFILE .zip`

    # get the basedir of the zip contents (everything before the last "-")
    NAME=${FILE%-*}

    # get the run number (everything after the last "-")
    RUN=${FILE##*-}

    # copy the property file used for the run
    /bin/cp ${SGE_JOB_NAME}-${RUN}.properties ${OUTPUT_DIR}/${NAME}-${RUN}.properties

    # test for existence of model conditions file
    /usr/bin/unzip -l -qq $ZIPFILE ${NAME}/model-conditions.txt
    # proceed only if model-conditions.txt exists in zip file
    if [ $? -eq 0 ]; then
        # extract all the features, models and predictions
        /usr/bin/unzip -q $ZIPFILE "${NAME}/features/*" "${NAME}/models/*" "${NAME}/predictions/*" -d ${OUTPUT_DIR}

        # move files around so they are all together
        # source files: ${OUTPUT_DIR}/${NAME}/${RESULT}/*
        # destination: ${OUTPUT_DIR}/${RESULT}/
        for RESULT in features models predictions; do
            for ENDPOINT_DIR in ${OUTPUT_DIR}/${NAME}/${RESULT}/*; do
                ENDPOINT=`basename ${ENDPOINT_DIR}`
                /bin/mkdir -p ${OUTPUT_DIR}/${RESULT}/${ENDPOINT}
                # TODO: handle case where there are too many files to move
                /bin/mv ${ENDPOINT_DIR}/* ${OUTPUT_DIR}/${RESULT}/${ENDPOINT}
                /bin/rmdir ${ENDPOINT_DIR}
            done
            /bin/rmdir ${OUTPUT_DIR}/${NAME}/${RESULT}
        done
        /bin/rmdir ${OUTPUT_DIR}/${NAME}

        # extract the model conditions (replacing the temporary paths used for SGE)
        /usr/bin/unzip -p $ZIPFILE ${NAME}/${NAME}-README.txt > ${OUTPUT_DIR}/${NAME}-$RUN-README.txt
        /usr/bin/unzip -p $ZIPFILE ${NAME}/model-conditions.txt \
            | sed -e "s|properties=.*/${ANT_PROJECT_NAME}.properties|properties=${ANT_PROJECT_NAME}.properties|g" \
            | sed -e "s|sequence-file=.*/sequences|sequence-file=sequences|g" \
                >> ${DATA_DIR}/model-conditions.txt
    else
        echo Skipped $ZIPFILE since no model-conditions file was found
    fi
done
