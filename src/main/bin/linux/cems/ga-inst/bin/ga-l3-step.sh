#!/bin/bash

. ${GA_INST}/bin/ga-env.sh

tile=$1
year=$2
startdoy=$3
snow=$4
gaRootDir=$5
priorDir=$6
snowPriorDir=$7
beamDir=$8

enddoy=`printf '%03d\n' "$((10#$startdoy + 7))"`

task="ga-l3"
jobname="${task}-${tile}-${year}-${startdoy}-snow_${snow}"
command="./bin/${task}-beam.sh ${tile} ${year} ${startdoy} ${enddoy} ${snow} ${gaRootDir} ${priorDir} ${snowPriorDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
