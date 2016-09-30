#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-meris-aatsr-coreg.sh

aatsrPath=$1
aatsrFile=$2
merisPath=$3
merisFile=$4
gaRootDir=$5

aatsrDate=${aatsrFile:14:15}
merisDate=${merisFile:14:15}

task="ga-l2-aatsr-coregister"
jobname="${task}-ATS_${aatsrDate}-MER_${merisDate}"
command="./bin/${task}-python.sh ${aatsrPath} ${merisPath} ${merisFile} ${gaRootDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
