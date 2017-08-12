#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-meteosat-brf-tiles.sh

year=$1
bbdrPath=$2
bbdrFile=$3
bbdrTileDir=$4
diskId=$5
hIndex=$6
sensor=$7
gaRootDir=$8
beamDir=$9

bbdrBaseName=`basename $bbdrFile .nc`

task="ga-l2-meteosat-bbdr-tiles"
jobname="${task}-${sensor}-${diskId}-${year}-${hIndex}-${bbdrBaseName}"
command="./bin/${task}-beam.sh ${year} ${bbdrPath} ${bbdrTileDir} ${diskId} ${hIndex} ${sensor} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
