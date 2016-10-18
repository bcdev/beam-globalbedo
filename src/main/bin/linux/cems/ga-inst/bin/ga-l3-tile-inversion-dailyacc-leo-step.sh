#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc.sh

tile=$1
year=$2
startdoy=$3
modisTileScaleFactor=$4
gaRootDir=$5
bbdrRootDir=$6
beamDir=$7

enddoy=`printf '%03d\n' "$((10#$startdoy + 7))"`

task="ga-l3-tile-inversion-dailyacc-leo"
jobname="${task}-${tile}-${year}-${startdoy}-dailyacc-leo"
command="./bin/${task}-beam.sh ${tile} ${year} ${startdoy} ${enddoy} ${modisTileScaleFactor} ${gaRootDir} ${bbdrRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    echo "calling submit_job..."
    submit_job ${jobname} ${command}
fi

echo "calling wait_for_task_jobs_completion..." 
wait_for_task_jobs_completion ${jobname}

echo "all calls done from ga-env-l3-tile-inversion-dailyacc-step.sh." 
