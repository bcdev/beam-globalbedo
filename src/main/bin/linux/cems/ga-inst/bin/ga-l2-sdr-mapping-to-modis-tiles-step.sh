#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-sdr-mapping.sh

allSdrPaths=$1
sdrTileDir=$2
tile=$3
interval=$4
sensor=$5
gaRootDir=$6
beamDir=$7

echo "STEP allSdrPaths: $allSdrPaths"
echo "STEP sdrTileDir : $sdrTileDir"
echo "STEP tile       : $tile"
echo "STEP interval   : $interval"
echo "STEP sensor     : $sensor"
echo "STEP gaRootDir  : $gaRootDir"
echo "STEP beamDir    : $beamDir"

task="ga-l2-sdr-mapping-to-modis-tiles"
jobname="${task}-${tile}-${interval}-${sensor}"
command="./bin/${task}-beam.sh ${allSdrPaths} ${sdrTileDir} ${sensor} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
