#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-albedomosaic-avhrrgeo.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-albedomosaic-avhrrgeo_nologs.sh

echo "entered ga-l3-albedomosaic-simple-step..."

sensorID=$1
year=$2
doy=$3
snowMode=$4
deg=$5
proj=$6
tileSize=$7
gaRootDir=$8
beamDir=$9

doy=`printf '%03d\n' "$((10#$doy))"`

task="ga-l3-albedomosaic-simple"
jobname="${task}-${year}-${doy}-${snowMode}-${deg}-${proj}-${tileSize}"
command="./bin/${task}-beam.sh ${sensorID} ${year} ${doy} ${snowMode} ${deg} ${proj} ${tileSize} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
