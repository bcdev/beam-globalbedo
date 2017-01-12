#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-cleanup.sh

tile=$1
year=$2
gaRootDir=$3

task="ga-l3-tile-inversion-cleanup"
jobname="${task}-${tile}-${year}"
command="./bin/${task}-bash.sh ${tile} ${year} ${gaRootDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}
