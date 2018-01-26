#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-staging-browse2mov.sh

echo "entered ga-l3-staging-browse2mov-step..."
year=$1
band=$2
stagingBrowse2movInputDir=$3
stagingBrowse2movResultDir=$4

task="ga-l3-staging-browse2mov-spectral"
jobname="${task}-${year}-${band}"
command="./bin/${task}-ffmpeg.sh ${stagingBrowse2movInputDir} $year ${stagingBrowse2movResultDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
