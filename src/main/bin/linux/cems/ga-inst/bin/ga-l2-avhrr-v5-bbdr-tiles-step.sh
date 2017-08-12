#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-avhrr-brf-tiles.sh

year=$1
month=$2
bbdrPath=$3
bbdrFile=$4
bbdrTileDir=$5
hStart=$6
hEnd=$7
gaRootDir=$8
beamDir=$9

# old products:
# bbdrBaseName=`basename $bbdrFile .nc`
# new products:
#bbdrBaseName=`basename $bbdrFile .NC.bz2`
# new products v5 201707:
bbdrBaseName=`basename $bbdrFile .zip`

task="ga-l2-avhrr-v5-bbdr-tiles"
jobname="${task}-AVHRR-${year}-${month}-${hStart}-${hEnd}-${bbdrBaseName}"
command="./bin/${task}-beam.sh ${year} ${bbdrPath} ${bbdrTileDir} ${hStart} ${hEnd} ${gaRootDir} ${beamDir}"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
