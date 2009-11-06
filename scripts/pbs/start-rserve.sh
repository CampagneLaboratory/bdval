#/bin/sh -x

. /etc/profile

RSERVE_LIB=${RSERVE_LIB:-"~/R/x86_64-unknown-linux-gnu-library/2.9/Rserve/libs/Rserve.so"}

PORT=${1:-"6311"}

echo "Starting Rserve process on port $PORT"
R CMD ${RSERVE_LIB} --RS-port $PORT --no-save
