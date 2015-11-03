#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-staging-nc2browse-albedodiff.sh

echo "entered ga-l3-staging-ncbrowse-albedodiff-step..."
year=$1
res=$2
proj=$3
stagingNc2browseListFile=$4
stagingNc2browseResultDir=$5

task="ga-l3-staging-nc2browse-albedodiff"
jobname="${task}-${year}-${res}-${proj}"
command="./bin/${task}-python.sh ${stagingNc2browseListFile} ${stagingNc2browseResultDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
