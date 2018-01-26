#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-albedomosaic-merge.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-albedomosaic-merge_nologs.sh

echo "entered ga-l3-albedomosaic-merge-step..."

year=$1
doy=$2
gaRootDir=$3
beamDir=$4

doy=`printf '%03d\n' "$((10#$doy))"`

task="ga-l3-albedomosaic-spectral-merge"
jobname="${task}-${year}-${doy}"
command="./bin/${task}-beam.sh ${year} ${doy} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

# comment if no logs shall be written...
wait_for_task_jobs_completion ${jobname}
