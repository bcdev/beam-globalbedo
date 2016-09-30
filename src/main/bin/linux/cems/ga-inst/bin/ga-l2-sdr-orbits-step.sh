#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-sdr-orbits.sh

l1bPath=$1
l1bFile=$2
sdrL2Dir=$3
year=$4
month=$5
sensor=$6
gaRootDir=$7
beamDir=$8

if [ $sensor == "VGT" ]
then
    l1bBaseName=`basename $l1bFile .ZIP`
else
    l1bBaseName=`basename $l1bFile .N1`
fi
echo "l1bBaseName: $l1bBaseName"

task="ga-l2-sdr-orbits"
jobname="${task}-${sensor}-${year}-${month}-${l1bBaseName}"
command="./bin/${task}-beam.sh ${l1bPath} ${l1bBaseName} ${sdrL2Dir} ${sensor} ${year} ${month} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
