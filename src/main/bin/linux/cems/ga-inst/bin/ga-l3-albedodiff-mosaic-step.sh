#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-albedodiff-mosaic.sh

echo "entered ga-l3-mosaic-step..."

year=$1
doy=$2
gaRootDir=$3
albedodiffRootDir=$4
beamDir=$5

doy=`printf '%03d\n' "$((10#$doy))"`

task="ga-l3-albedodiff-mosaic"
jobname="${task}-${year}-${doy}"
command="./bin/${task}-beam.sh ${year} ${doy} ${scaling} ${gaRootDir} ${albedodiffRootDir} ${beamDir}"
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
