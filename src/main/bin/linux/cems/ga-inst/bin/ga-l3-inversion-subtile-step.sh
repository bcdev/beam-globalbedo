#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-subtile.sh

tile=$1
year=$2
doy=$3
startX=$4
startY=$5
endX=$6
endY=$7
gaRootDir=$8
bbdrRootDir=$9
priorDir=${10}
beamDir=${11}
inversionTargetDir=${12}

task="ga-l3-inversion-subtile"
jobname="${task}-${tile}-${year}-${doy}-${startX}-${startY}-${endX}-${endY}"
command="./bin/${task}-beam.sh ${tile} ${year} ${doy} ${startX} ${startY} ${endX} ${endY} ${gaRootDir} ${bbdrRootDir} ${priorDir} ${beamDir} ${inversionTargetDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
