#!/bin/sh
. /etc/profile

# Run bdval job
cd $TMPDIR/@JOB-NAME@/data

# see if the job is running as an array job submission
if [ -z "$PBS_ARRAY_INDEX" ]; then
    PBS_ANT_PROPERTY_FILE=../@JOB-NAME@-1.properties
else
    PBS_ANT_PROPERTY_FILE=../@JOB-NAME@-$PBS_ARRAY_INDEX.properties
fi

ant -propertyfile $PBS_ANT_PROPERTY_FILE -Dsave-data-tag=@TAG@ -Dtag-description="@TAG-DESCRIPTION@" -f @PROJECT@.xml @TARGET@

# Copy results back to the master node when we are done
JOB_RESULTS_DIR=@JOB-DIR@-results/@PROJECT@-$PBS_ARRAY_INDEX
ssh @MASTER-NODE@ "/bin/mkdir -p ${JOB_RESULTS_DIR}"
scp -r *.zip logs @@MASTER-NODE@:${JOB_RESULTS_DIR}
