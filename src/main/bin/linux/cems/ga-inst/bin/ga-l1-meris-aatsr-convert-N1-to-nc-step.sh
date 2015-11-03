#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l1-meris-aatsr-convert-N1-to-nc.sh

l1bN1SourcePath=$1
l1bN1SourceFile=$2
l1bNcTargetPath=$3
year=$4
month=$5
day=$6
gaRootDir=$7
beamDir=$8
l1bNcTargetDir=$9
upperLat=${10}
lowerLat=${11}

l1bSensor=${l1bN1SourceFile:0:3}
l1bDate=${l1bN1SourceFile:14:15}

task="ga-l1-meris-aatsr-convert-N1-to-nc"
jobname="${task}-${year}-${month}-${day}-${l1bSensor}-${l1bDate}"
command="./bin/${task}-beam.sh ${year} ${month} ${day} ${gaRootDir} ${beamDir} ${l1bN1SourcePath} ${l1bNcTargetPath} ${l1bNcTargetDir} ${upperLat} ${lowerLat}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
