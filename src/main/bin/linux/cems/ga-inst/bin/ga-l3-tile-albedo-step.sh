#!/bin/bash

. ${GA_INST}/bin/ga-env.sh

tile=$1
year=$2
doy=$3
gaRootDir=$4
priorDir=$5
snowPriorDir=$6
beamDir=$7
albedoTargetDir=$8

task="ga-l3-tile-albedo"
jobname="${task}-${tile}-${year}-${doy}"
command="./bin/${task}-beam.sh ${tile} ${year} ${doy} ${gaRootDir} ${priorDir} ${snowPriorDir} ${beamDir} ${albedoTargetDir}"
memory=16384

echo "jobname: $jobname"
echo "command: $command"
echo "memory: ${memory}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command} ${memory}
fi

wait_for_task_jobs_completion ${jobname} 
