#!/bin/sh -fx

if [ -z $1 ]; then
    echo "Job name is required"
    exit 1
fi

# Absolute path to this script.
SCRIPT=$(readlink -f $0)
# Absolute path this script is in.
SCRIPT_DIR=`dirname $SCRIPT`

BDVAL_DIR=${BDVAL_DIR:-$SCRIPT_DIR/../..}
BDVAL_CONFIG=${BDVAL_DIR}/config
BDVAL_DATA=${BDVAL_DIR}/data

JOB=$1
JOB_TAG=${JOB}-$$
JOB_DIR=$(readlink -f .)/${JOB_TAG}
JOB_CONFIG=${JOB_DIR}/config
JOB_DATA=${JOB_DIR}/data

#. ${JOB}-pbs-env.sh

/bin/mkdir -p ${JOB_DIR} ${JOB_CONFIG} ${JOB_DATA}
/bin/cp ${BDVAL_DIR}/bdval.jar ${JOB_DIR}
/bin/cp -r ${BDVAL_DIR}/buildsupport ${JOB_DIR}
/bin/cp ${SCRIPT_DIR}/start-rserve.sh ${JOB_DIR}

#
# Tell bdval not to try and compile
#
cat > ${JOB_DIR}/bdval.properties <<EOF
use-bdval-jar=true
nocompile=true
EOF

#
# Two instances of Rserve per node
#
cat > ${JOB_CONFIG}/RConnectionPool.xml <<EOF
<RConnectionPool>
   <RConfiguration>
      <RServer host="localhost" port="6311"/>
      <RServer host="localhost" port="6312"/>
   </RConfiguration>
</RConnectionPool>
EOF

#
# Two threads per node
#
cat > ${JOB_CONFIG}/${JOB}-local.properties <<EOF
eval-dataset-root=bdval/GSE8402
computer.type=server
server.thread-number=2
server.memory=-Xmx2000m
EOF

#
# Create the job submission script
#
cat > ${JOB_TAG}.qsub <<EOF
#/bin/sh -x

# Determines the queue a job is submitted to
#PBS -q normal

# Combine PBS error and output files.
#PBS -j oe

# Number of nodes (exclusive access)
#PBS -l nodes=2#excl

# Requred files on each node
#PBS -W stagein=/tmp/${JOB_TAG}@master01:${JOB_DIR}

# Mail job status at completion
#PBS -m ae

# Mail to user specified
#PBS -M mas2062@med.cornell.edu

#
# Output some useful job information
#
echo ------------------------------------------------------
echo Job is running on the following nodes:
cat $PBS_NODEFILE
echo ------------------------------------------------------
echo PBS: qsub is running on \$PBS_O_HOST
echo PBS: originating queue is \$PBS_O_QUEUE
echo PBS: executing queue is \$PBS_QUEUE
echo PBS: working directory is \$PBS_O_WORKDIR
echo PBS: execution mode is \$PBS_ENVIRONMENT
echo PBS: job identifier is \$PBS_JOBID
echo PBS: job name is \$PBS_JOBNAME
echo PBS: node file is \$PBS_NODEFILE
echo PBS: current home directory is \$PBS_O_HOME
echo PBS: PATH = \$PBS_O_PATH
echo ------------------------------------------------------

printenv | sort

#
# Start instances of Rserve on each node in the cluster
#
pbsdsh -v /tmp/${JOB_TAG}/start-rserve.sh 6311
pbsdsh -v /tmp/${JOB_TAG}/start-rserve.sh 6312

# Just double checking stuff here
for node in \`sort \$PBS_NODEFILE | uniq\`
do
  echo \$node
  echo "============="

  ls -lR /tmp

  ssh \$node "java -jar /home/marko/RUtils/icb-rutils.jar --port 6311 --validate"
  ssh \$node "java -jar /home/marko/RUtils/icb-rutils.jar --port 6312 --validate"

  ssh \$node "ps -elf | grep \$USER"
done

# TODO - launch bdval here
pbsdsh -v /home/marko/bdval/scripts/pbs/bdval-pbs.sh

# Just double checking stuff here
for node in \`sort \$PBS_NODEFILE | uniq\`
do
  ssh \$node "ls -R /tmp"
done

EOF

echo "Job tag is $JOB_TAG"