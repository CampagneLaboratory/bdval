#/bin/sh -x

#
# An example running the BDVAL prostate dataset on a cluster with PBS
#

# Determines the queue a job is submitted to
#PBS -q normal

# Combine PBS error and output files.
#PBS -j oe

# Number of nodes (exclusive access)
#PBS -l nodes=2#excl

# Requred files on each node
#PBS -W stagein=/tmp/foobar.jar@master01:/home/marko/bdval/bdval.jar

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
echo PBS: qsub is running on $PBS_O_HOST
echo PBS: originating queue is $PBS_O_QUEUE
echo PBS: executing queue is $PBS_QUEUE
echo PBS: working directory is $PBS_O_WORKDIR
echo PBS: execution mode is $PBS_ENVIRONMENT
echo PBS: job identifier is $PBS_JOBID
echo PBS: job name is $PBS_JOBNAME
echo PBS: node file is $PBS_NODEFILE
echo PBS: current home directory is $PBS_O_HOME
echo PBS: PATH = $PBS_O_PATH
echo ------------------------------------------------------

printenv | sort

#
# Start instances of Rserve on each node in the cluster
#
pbsdsh -v /home/marko/bdval/scripts/pbs/start-rserve.sh 6311
pbsdsh -v /home/marko/bdval/scripts/pbs/start-rserve.sh 6312

# Just double checking stuff here
for node in `sort $PBS_NODEFILE | uniq`
do
  echo $node
  echo "============="

  ls -l /tmp/foobar.jar

  ssh $node "java -jar /home/marko/RUtils/icb-rutils.jar --port 6311 --validate"
  ssh $node "java -jar /home/marko/RUtils/icb-rutils.jar --port 6312 --validate"

  ssh $node "ps -elf | grep $USER"
done

# TODO - launch bdval here
pbsdsh -v /home/marko/bdval/scripts/pbs/bdval-pbs.sh

# Just double checking stuff here
for node in `sort $PBS_NODEFILE | uniq`
do
  ssh $node "ls -R /tmp
done
