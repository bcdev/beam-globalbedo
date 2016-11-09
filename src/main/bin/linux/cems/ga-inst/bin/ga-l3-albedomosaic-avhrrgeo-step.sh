#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-albedomosaic-avhrrgeo.sh

echo "entered ga-l3-albedomosaic-step..."

year=$1
doy=$2
snowMode=$3
deg=$4
gaRootDir=$5
beamDir=$6

doy=`printf '%03d\n' "$((10#$doy))"`

task="ga-l3-albedomosaic"
jobname="${task}-${year}-${doy}-${snowMode}-${deg}"
command="./bin/${task}-beam.sh ${year} ${doy} ${snowMode} ${deg} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
