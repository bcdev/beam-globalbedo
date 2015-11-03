#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-meris-aatsr-coreg.sh

aatsrL1bDir=$1
aatsrL1bFile=$2
merisL1bDir=$3
merisL1bFile=$4
year=$5
month=$6
day=$7
gaRootDir=$8

aatsrDate=${aatsrL1bFile:14:15}
merisDate=${merisL1bFile:14:15}

task="ga-l2-meris-aatsr-coreg"
jobname="${task}-${year}-${month}-${day}-ATS_${aatsrDate}-MER_${merisDate}"
command="./bin/${task}-python.sh ${aatsrL1bDir} ${aatsrL1bFile} ${merisL1bDir} ${merisL1bFile} ${year} ${month} ${day} ${gaRootDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
