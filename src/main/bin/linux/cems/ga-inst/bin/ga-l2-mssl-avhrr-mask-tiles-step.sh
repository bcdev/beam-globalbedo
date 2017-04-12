#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-avhrr-brf-tiles.sh

msslMaskPath=$1
msslMaskFile=$2
msslMaskTileDir=$3
gaRootDir=$4
beamDir=$5

msslMaskBaseName=`basename $msslMaskFile .nc`

task="ga-l2-mssl-avhrr-mask-tiles"
jobname="${task}-AVHRR-${msslMaskBaseName}"
command="./bin/${task}-beam.sh ${msslMaskPath} ${msslMaskTileDir} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
