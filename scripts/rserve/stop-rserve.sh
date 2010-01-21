#/bin/sh -x

. /etc/profile

PORT=${1:-"6311"}
echo "Stopping Rserve process on port $PORT"

# Assume bdval resides in the same directory as this script
cd $TMPDIR/@JOB-NAME@
java -cp bdval.jar edu.cornell.med.icb.R.RUtils --port $PORT --shutdown
