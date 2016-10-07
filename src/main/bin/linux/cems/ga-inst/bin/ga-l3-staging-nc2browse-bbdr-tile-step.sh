#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-staging-nc2browse.sh

echo "entered ga-l3-staging-ncbrowse-bbdr-tile-step..."
datestring=$1
tile=$2
sensor=$3
lon_geo=$4
band=$5
plot_min=$6
plot_max=$7
stagingNc2browseFile=$8
stagingNc2browseResultDir=$9
gaRootDir=${10}

task="ga-l3-staging-nc2browse-bbdr-tile"
jobname="${task}-${datestring}-${tile}-${sensor}${lon_geo}-${band}"
#command="${pythonCmd} ${stagingNc2browseFile} ${stagingNc2browseResultDir} ${datestring} ${band} ${MINMAX} ${LUT} ${SIZE} ${COLORTXT}"
command="./bin/${task}-python.sh ${stagingNc2browseFile} ${stagingNc2browseResultDir} ${gaRootDir} ${datestring} ${band} ${plot_min} ${plot_max}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
