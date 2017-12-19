#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs.sh

sensorID=$1
year=$2
res=$3
snowMode=$4
startdoy=$5
enddoy=$6
albedoRootDir=$7
albedoTimedimDir=$8

if [ ! -e "$albedoTimedimDir" ]
then
    mkdir -p $albedoTimedimDir
fi

for iDoy in $(seq -w $startdoy $enddoy); do   # -w takes care for leading zeros
    task="ga-l3-albedo-mosaic-timedim"
    jobname="${task}-${year}-${iDoy}-${res}-${snowMode}"
    command="./bin/${task}-python.sh ${sensorID} ${year} ${iDoy} ${res} ${snowMode} ${albedoRootDir} ${albedoTimedimDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs()..."
    read_task_jobs ${jobname}

    submit_job ${jobname} ${command}
done
