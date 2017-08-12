#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-preprocess-priors.sh

tile=$1
doy=$2
scaleFactor=$3
gaRootDir=$4
priorDir=$5
beamDir=$6

task="ga-l3-preprocess-priors-avhrrgeo"
jobname="${task}-${tile}-${doy}"
command="./bin/${task}-beam.sh ${tile} ${doy} ${scaleFactor} ${gaRootDir} ${priorDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
