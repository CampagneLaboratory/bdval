#!/bin/sh
. /etc/profile

# Absolute path to this script.
SCRIPT=$(readlink -f $0)
# Absolute path this script is in.
SCRIPT_DIR=`dirname $SCRIPT`

# Run bdval job
cd $SCRIPT_DIR/data
ant -Dsave-data-tag=@TAG@ -Dtag-description="@TAG-DESCRIPTION@" -f @PROJECT@.xml @TARGET@

# Copy results back to the master node when we are done
JOB_RESULTS_DIR=@JOB-DIR@-results/${HOSTNAME}
ssh master01 "/bin/mkdir -p ${JOB_RESULTS_DIR}"
scp -r *.zip logs @master01:${JOB_RESULTS_DIR}
