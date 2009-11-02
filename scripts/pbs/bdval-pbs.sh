#!/bin/sh
. /etc/profile

# Absolute path to this script.
SCRIPT=$(readlink -f $0)
# Absolute path this script is in.
SCRIPT_DIR=`dirname $SCRIPT`

# Run bdval job
cd $SCRIPT_DIR/data

ant -propertyfile=@PROJECT@-$PBS_ARRAY_INDEX.properties -Dsave-data-tag=@TAG@ -Dtag-description="@TAG-DESCRIPTION@" -f @PROJECT@.xml @TARGET@

# Copy results back to the master node when we are done
JOB_RESULTS_DIR=@JOB-DIR@-results/${HOSTNAME}
ssh @MASTER-NODE@ "/bin/mkdir -p ${JOB_RESULTS_DIR}"
scp -r *.zip logs @@MASTER-NODE@:${JOB_RESULTS_DIR}
