#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-staging-nc2browse.sh

echo "entered ga-l3-staging-ncbrowse-step..."
year=$1
doy=$2
snowMode=$3
res=$4
proj=$5
stagingNc2browseFile=$6
stagingNc2browseResultDir=$7

task="ga-l3-staging-nc2browse"
jobname="${task}-${year}-${doy}-${snowMode}-${res}-${proj}"
#command="./bin/${task}-python.sh ${stagingNc2browseFile} ${stagingNc2browseResultDir}"
command="./bin/${task}-python_only2bands.sh ${stagingNc2browseFile} ${stagingNc2browseResultDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
