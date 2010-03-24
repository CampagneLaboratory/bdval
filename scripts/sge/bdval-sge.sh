#!/bin/sh

. /etc/profile

# Run bdval job
cd $TMPDIR/@JOB-NAME@/data

SGE_ANT_PROPERTY_FILE=../@JOB-NAME@-${SGE_TASK_ID}.properties
ant -propertyfile $SGE_ANT_PROPERTY_FILE -Dsave-data-tag=@TAG@ -Dtag-description="@TAG-DESCRIPTION@" -f @PROJECT@.xml @TARGET@

# Copy results back to the master node when we are done
JOB_RESULTS_DIR=@JOB-DIR@-results
/usr/bin/ssh @MASTER-NODE@ "/bin/mkdir -p ${JOB_RESULTS_DIR}/logs"
/usr/bin/scp @TAG@.zip @@MASTER-NODE@:${JOB_RESULTS_DIR}/@TAG@-${SGE_TASK_ID}.zip
/usr/bin/scp -r logs @@MASTER-NODE@:${JOB_RESULTS_DIR}/logs/@TAG@-${SGE_TASK_ID}
