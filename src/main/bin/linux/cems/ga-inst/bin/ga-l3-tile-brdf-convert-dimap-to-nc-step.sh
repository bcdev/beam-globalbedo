#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-brdf-convert-dimap-to-nc.sh

tile=$1
year=$2
doy=$3
gaRootDir=$4
beamDir=$5
brdfTargetDir=$6

task="ga-l3-tile-brdf-convert-dimap-to-nc"
jobname="${task}-${tile}-${year}-${doy}"
command="./bin/${task}-beam.sh ${tile} ${year} ${doy} ${gaRootDir} ${beamDir} ${brdfTargetDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
