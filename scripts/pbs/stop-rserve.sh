#/bin/sh -x

. /etc/profile

PORT=${1:-"6311"}

echo "Stopping Rserve process on port $PORT"
java -cp bdval.jar edu.cornell.med.icb.R.RUtils --port $PORT --shutdown
exec R CMD ${RSERVE_LIB} --RS-port $PORT --no-save
