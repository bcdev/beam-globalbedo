#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-sdr-tiles.sh

sdrModisPath=$1
sdrModisFile=$2
sdrModisTileDir=$3
sensor=$4
gaRootDir=$5
beamDir=$6

sdrModisBaseName=`basename $sdrModisFile .nc`

task="ga-l2-sdr-modis-tiles-to-subtiles"
jobname="${task}-${sensor}-${sdrModisBaseName}"
command="./bin/${task}-beam.sh ${sdrModisPath} ${sdrModisTileDir} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
