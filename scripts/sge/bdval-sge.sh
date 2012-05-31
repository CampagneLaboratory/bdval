#!/bin/sh

. /etc/profile

# Run bdval job
TMDIR=/scratchLocal/campagne
cd $TMPDIR/@JOB-NAME@/data

SGE_ANT_PROPERTY_FILE=../@JOB-NAME@-${SGE_TASK_ID}.properties
ant -propertyfile $SGE_ANT_PROPERTY_FILE \
    -Dsave-data-tag="@TAG@" \
    -Dtag-description="@TAG-DESCRIPTION@" \
    -Dresults-directory="@TAG@" \
    -Dcache-dir-location="@CACHE-DIR-LOCATION@" \
    -Dmodel-conditions="model-conditions.txt" \
    -f @PROJECT@.xml @TARGET@

# Copy results back to the master node when we are done
JOB_RESULTS_DIR=@JOB-DIR@-results
/bin/mkdir -p ${JOB_RESULTS_DIR}/logs
/bin/cp @TAG@.zip ${JOB_RESULTS_DIR}/@TAG@-${SGE_TASK_ID}.zip
/bin/cp -r logs ${JOB_RESULTS_DIR}/logs/@TAG@-${SGE_TASK_ID}
