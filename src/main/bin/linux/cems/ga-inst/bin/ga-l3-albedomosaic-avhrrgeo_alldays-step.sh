#!/bin/bash

#. ${GA_INST}/bin/ga_env/ga-env-l3-albedomosaic-avhrrgeo.sh
. ${GA_INST}/bin/ga_env/ga-env-l3-albedomosaic-avhrrgeo_nologs.sh

echo "entered ga-l3-albedomosaic-avhrrgeo_alldays-step..."

year=$1
startdoy=$2
enddoy=$3
snowMode=$4
deg=$5
proj=$6
tileSize=$7
gaRootDir=$8
beamDir=$9

#doy=`printf '%03d\n' "$((10#$doy))"`


for iDoy in $(seq -w $startdoy $enddoy); do   # -w takes care for leading zeros
    task="ga-l3-albedomosaic-simple"
    jobname="${task}-${year}-${iDoy}-${snowMode}-${deg}-${proj}-${tileSize}"
    command="./bin/${task}-beam.sh ${year} ${iDoy} ${snowMode} ${deg} ${proj} ${tileSize} ${gaRootDir} ${beamDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs()..."
    read_task_jobs ${jobname}

    submit_job ${jobname} ${command}
done
