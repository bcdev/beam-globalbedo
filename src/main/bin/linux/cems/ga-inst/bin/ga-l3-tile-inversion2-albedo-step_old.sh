#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo.sh

tile=$1
year=$2
startdoy=$3
gaRootDir=$4
priorDir=$5
beamDir=$6
albedoTargetDir=$7

enddoy=`printf '%03d\n' "$((10#$startdoy + 7))"`

task="ga-l3-tile-inversion-albedo"
jobname="${task}-${tile}-${year}-${startdoy}"
command="./bin/${task}-beam.sh ${tile} ${year} ${startdoy} ${enddoy} ${gaRootDir} ${priorDir} ${beamDir} ${albedoTargetDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
