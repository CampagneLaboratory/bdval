# Environment variables used for a bdval pbs job submission

#
# Name of the PBS queue to use
#
PBS_QUEUE="normal"

#
# The number of nodes to request
#
PBS_NODES=1

#
# Tag for the job submission (no spaces)
#
SAVE_DATA_TAG="mytag"

#
# Description of the job (should be in quotes)
#
TAG_DESCRIPTION="Testing the PBS submission"

#
# Full path to datasets used for this job
#
DATASET_ROOT=/home/${USER}/bdval/data/bdval/GSE8402

#
# Ant target to run
#
ANT_TARGET=evaluate

#
# email address
#
MAILTO=
