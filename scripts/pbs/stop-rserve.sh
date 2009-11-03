#/bin/sh -x

. /etc/profile

# Absolute path to this script.
SCRIPT=$(readlink -f $0)
# Absolute path this script is in.
SCRIPT_DIR=`dirname $SCRIPT`

PORT=${1:-"6311"}
echo "Stopping Rserve process on port $PORT"

# Assume bdval resides in the same directory as this script
cd $SCRIPT_DIR
java -cp bdval.jar edu.cornell.med.icb.R.RUtils --port $PORT --shutdown
