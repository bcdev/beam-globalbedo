#!/bin/bash

#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo.sh
. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs.sh

sensorID=$1
tile=$2
year=$3
startdoy=$4
enddoy=$5
albedoRootDir=$6
albedoTimedimDir=$7

if [ ! -e "$albedoTimedimDir" ]
then
    mkdir -p $albedoTimedimDir
fi

for iDoy in $(seq -w $startdoy $enddoy); do   # -w takes care for leading zeros
    task="ga-l3-albedo-timedim"
    jobname="${task}-${tile}-${year}-${iDoy}"
    command="./bin/${task}-python.sh ${sensorID} ${tile} ${year} ${iDoy} ${albedoRootDir} ${albedoTimedimDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs()..."
    read_task_jobs ${jobname}

    submit_job ${jobname} ${command}
done
