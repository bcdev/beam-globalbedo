#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-avhrr-brf-tiles.sh

brfPath=$1
brfFile=$2
brfTileDir=$3
hStart=$4
hEnd=$5
gaRootDir=$6
beamDir=$7

brfBaseName=`basename $brfFile .nc`

task="ga-l2-avhrr-brf-tiles"
jobname="${task}-AVHRR-${hStart}-${hEnd}-${brfBaseName}"
command="./bin/${task}-beam.sh ${brfPath} ${brfTileDir} ${hStart} ${hEnd} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
