#!/bin/bash

. ${GA_INST}/bin/ga-env.sh

echo "entered ga-l3-mosaic-step..."

year=$1
doy=$2
snowMode=$3
deg=$4
gaRootDir=$5
beamDir=$6

doy=`printf '%03d\n' "$((10#$doy))"`

if [ $deg -eq "005" ]
then
    scaling=6
elif [ $deg -eq "025" ]
then
    scaling=30
else
    scaling=60
fi

task="ga-l3-brdfmosaic"
jobname="${task}-${year}-${doy}-${snowMode}-${deg}"
command="./bin/${task}-beam.sh ${year} ${doy} ${snowMode} ${deg} ${scaling} ${gaRootDir} ${beamDir}"
memory="65536"

echo "jobname: $jobname"
echo "command: $command"
echo "memory: ${memory}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
