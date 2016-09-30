#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-avhrr-brf-tiles.sh

brfPath=$1
brfFile=$2
bbdrDir=$3
sensor=$4
gaRootDir=$5
beamDir=$6

brfBaseName=`basename $brfFile .nc`

task="ga-l2-meteosat-brf-to-bbdr"
jobname="${task}-${sensor}-${brfBaseName}"
command="./bin/${task}-beam.sh ${brfPath} ${brfBaseName} ${bbdrDir} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
