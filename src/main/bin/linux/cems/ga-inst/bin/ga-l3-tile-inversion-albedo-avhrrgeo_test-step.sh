#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs.sh

tile=$1
year=$2
startdoy=$3
enddoy=$4
gaRootDir=$5
bbdrRootDir=$6
inversionRootDir=$7
usePrior=$8
priorDir=$9
beamDir=${10}
modisTileScaleFactor=${11}
albedoTargetDir=${12}  # remind the brackets if >= 10!!

# TODO: to be safe, add a wait loop (60 sec) here and check for completion of writing binary accumulator files in ../DailyAccs/$year/(No)Snow
# This is indicated by presence of marker files 'PROCESSED_ALL'
# continue when they are present or after 15min with a warning
# (assume that wing years are ready as they were processed before the center year and have less daily accs)

dailyAccNoSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow

waitCount=0
while [  "$waitCount" -lt "15" ]; do
    markerNoSnow=$dailyAccNoSnowDir/PROCESSED_ALL
    markerSnow=$dailyAccSnowDir/PROCESSED_ALL
    thedate=`date`
    echo "Waiting for $waitCount minutes for completion of daily accs... $thedate"
    if [ -f "$markerNoSnow" ] && [ -f "$markerSnow" ]; then
        echo "Daily acc completed - ready for inversion."
        break
    fi
    let waitCount=waitCount+1 
    sleep 60
done
if [ "$waitCount" -ge "15" ]; then
    echo "WARNING: Daily accs not complete but starting inversion anyway."
fi


for iDoy in $(seq -w $startdoy $enddoy); do   # -w takes care for leading zeros
    task="ga-l3-tile-inversion-albedo-avhrrgeo"
    jobname="${task}-${tile}-${year}-${iDoy}"
    command="./bin/${task}-beam.sh ${tile} ${year} ${iDoy} ${gaRootDir} ${bbdrRootDir} ${inversionRootDir} ${usePrior} ${priorDir} ${beamDir} ${modisTileScaleFactor} ${albedoTargetDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs()..."
    read_task_jobs ${jobname}

    #if [ -z ${jobs} ]; then
    #    submit_job ${jobname} ${command}
    #fi
    submit_job ${jobname} ${command}
done

#for iDoy in $(seq -w $startdoy $enddoy); do   # -w takes care for leading zeros
#    task="ga-l3-tile-inversion-albedo-avhrrgeo"
#    jobname="${task}-${tile}-${year}-${iDoy}"
#    echo "calling wait_for_task_jobs_completion: $jobname"
#    wait_for_task_jobs_completion ${jobname} 
#done
