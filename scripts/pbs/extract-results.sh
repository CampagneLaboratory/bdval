#!/bin/sh

# Absolute path to this script.
SCRIPT=$(readlink -f $0)
# Absolute path this script is in.
SCRIPT_DIR=`dirname $SCRIPT`

# Base name of the BDVAL project file without the ".xml" (i.e., prostate-example, maqcii-c, etc.)
ANT_PROJECT_NAME=prostate-example
PBS_JOB_NAME=prostate-pbs
BASE_DIR=${SCRIPT_DIR}
EXECUTION_DIR=${BASE_DIR}/${PBS_JOB_NAME}
RESULTS_DIR=${BASE_DIR}/${PBS_JOB_NAME}-results

OUTPUT_BASE=/home/marko/foobar
DATA_DIR=${OUTPUT_BASE}/data
CONFIG_DIR=${OUTPUT_BASE}/config
OUTPUT_DIR=${DATA_DIR}/${PBS_JOB_NAME}-results

/bin/mkdir -p ${OUTPUT_DIR} ${CONFIG_DIR} ${OUTPUT_DIR}

# Copy the files used to exectute the runs
/bin/cp ${EXECUTION_DIR}/bdval.jar -r ${EXECUTION_DIR}/buildsupport ${OUTPUT_BASE}
/bin/cp ${EXECUTION_DIR}/config/${ANT_PROJECT_NAME}-local.properties ${EXECUTION_DIR}/config/log4j.properties ${CONFIG_DIR}

/bin/cp -r ${EXECUTION_DIR}/data/* ${OUTPUT_BASE}/data
/bin/cp ${EXECUTION_DIR}/*.properties ${OUTPUT_DIR}

# Copy the log files
/bin/mkdir -p ${OUTPUT_DIR}/logs
/bin/cp ${BASE_DIR}/*.log ${OUTPUT_DIR}/logs

/bin/mkdir ${OUTPUT_DIR}/features ${OUTPUT_DIR}/models ${OUTPUT_DIR}/predictions

# extract all the features, models and predictions along with the model conditions
for ZIPFILE in ${RESULTS_DIR}/*.zip
do
  echo Processing $ZIPFILE

  # get the file name without the .zip part
  FILE=`basename $ZIPFILE .zip`

  # get the basedir of the zip contents (everything before the last "-")
  NAME=${FILE%-*}

  # get the run number (everything after the last "-")
  RUN=${FILE##*-}

  # copy the property file used for the run
  /bin/cp ${PBS_JOB_NAME}/${PBS_JOB_NAME}-${RUN}.properties ${OUTPUT_DIR}/${NAME}-${RUN}.properties

  # extract all the features, models and predictions
  /usr/bin/unzip $ZIPFILE "${NAME}/features/*" "${NAME}/models/*" "${NAME}/predictions/*" -d ${OUTPUT_DIR}

  # move files around so they are all together
  for RESULT in features models predictions
  do
    for ENDPOINT_DIR in ${OUTPUT_DIR}/${NAME}/${RESULT}/*
    do
      ENDPOINT=`basename ${ENDPOINT_DIR}`
      /bin/mkdir -p ${OUTPUT_DIR}/${RESULT}/${ENDPOINT}
      /bin/mv ${ENDPOINT_DIR}/* ${OUTPUT_DIR}/${RESULT}/${ENDPOINT}
      /bin/rmdir ${ENDPOINT_DIR}
    done
    /bin/rmdir ${OUTPUT_DIR}/${NAME}/${RESULT}
  done
  /bin/rmdir ${OUTPUT_DIR}/${NAME}

  # extract the model conditions
  /usr/bin/unzip -p $ZIPFILE ${NAME}/${NAME}-README.txt > ${OUTPUT_DIR}/${NAME}-$RUN-README.txt
  /usr/bin/unzip -p $ZIPFILE ${NAME}/model-conditions.txt \
      | sed -e "s|properties=.*/${ANT_PROJECT_NAME}.properties|properties=${DATA_DIR}/${ANT_PROJECT_NAME}.properties|g" \
      >> ${DATA_DIR}/model-conditions.txt
done
