#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-staging-browse2mov.sh

echo "entered ga-l3-staging-browse2mov-albedodiff-step..."
year=$1
res=$2
proj=$3
band=$4
stagingBrowse2movInputDir=$5
stagingBrowse2movResultDir=$6

task="ga-l3-staging-browse2mov"
jobname="${task}-${year}-${res}-${proj}-${band}"
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
