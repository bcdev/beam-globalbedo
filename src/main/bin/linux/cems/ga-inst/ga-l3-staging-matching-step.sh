#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-staging-matching.sh

echo "entered ga-l3-mosaic-step..."

tile=$1
stagingMatchingResultDir=$2

task="ga-l3-staging-matching"
jobname="${task}-${tile}"
command="./bin/${task}-java.sh ${tile} ${stagingMatchingResultDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
