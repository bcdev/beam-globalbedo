#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-wget-probav-synthesis-from-vito.sh

echo "entered wget-probav-synthesis-from-vito-step.sh..."

dayDir=$1
year=$2
month=$3
month02=$4
day=$5
day02=$6
version=$7

task="wget-probav-synthesis-from-vito"
jobname="${task}-${year}-${month}-${day}"
command="./bin/${task}-wget.sh ${dayDir} ${year} ${month} ${month02} ${day} ${day02} ${version}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
