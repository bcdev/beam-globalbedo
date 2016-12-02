#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-sdr-tiles.sh

sdrPath=$1
sdrFile=$2
sdrTileDir=$3
sensor=$4
gaRootDir=$5
beamDir=$6

sdrBaseName=`basename $sdrFile .nc`

task="ga-l2-sdr-mapping-meris-to-modis-tiles"
jobname="${task}-${sensor}-${sdrBaseName}"
command="./bin/${task}-beam.sh ${sdrPath} ${sdrTileDir} ${sensor} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
