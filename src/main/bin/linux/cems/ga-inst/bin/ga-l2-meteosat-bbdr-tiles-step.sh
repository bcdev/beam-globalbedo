#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-meteosat-brf-tiles.sh

bbdrPath=$1
bbdrFile=$2
bbdrTileDir=$3
diskId=$4
hIndex=$5
sensor=$6
gaRootDir=$7
beamDir=$8

bbdrBaseName=`basename $bbdrFile .nc`

task="ga-l2-meteosat-bbdr-tiles"
jobname="${task}-${sensor}-${diskId}-${hIndex}-${bbdrBaseName}"
command="./bin/${task}-beam.sh ${bbdrPath} ${bbdrTileDir} ${diskId} ${hIndex} ${sensor} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
