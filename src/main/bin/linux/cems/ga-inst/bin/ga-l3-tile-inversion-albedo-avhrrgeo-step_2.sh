#!/bin/bash

#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs.sh
. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs_priotest.sh

sensorID=$1
tile=$2
year=$3
startdoy=$4
enddoy=$5
gaRootDir=$6
bbdrRootDir=$7
inversionRootDir=$8
usePrior=$9
priorDir=${10}
beamDir=${11}
modisTileScaleFactor=${12}  # remind the brackets if >= 10!!

# TODO: to be safe, add a wait loop (60 sec) here and check for completion of writing binary accumulator files in ../DailyAccs/$year/(No)Snow
# This is indicated by presence of marker files 'PROCESSED_ALL'
# continue when they are present or after 30min with a warning
# (assume that wing years are ready as they were processed before the center year and have less daily accs)

dailyAccNoSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow

waitCount=0
while [  "$waitCount" -lt "30" ]; do
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
if [ "$waitCount" -ge "30" ]; then
    echo "WARNING: Daily accs not complete but starting inversion anyway."
fi

#for iStartDoy in $(seq -w $startdoy 4 $enddoy); do   # -w takes care for leading zeros
for iStartDoy in $(seq -w $startdoy 28 $enddoy); do   # -w takes care for leading zeros
    
    # set up job with 4 days at once to make it longer (several minutes) for better scheduling
    # iEndDoy=`printf '%03d\n' "$((10#$iStartDoy + 3))"`  # for step=4

    # set up job with 28 days at once to make it longer (several minutes) for better scheduling
    iEndDoy=`printf '%03d\n' "$((10#$iStartDoy + 27))"`  # for step=28 which covers whole period 1..365

    task="ga-l3-tile-inversion-albedo-avhrrgeo"
    jobname="${task}-${tile}-${year}-${iStartDoy}"
    command="./bin/${task}-beam_2.sh ${sensorID} ${tile} ${year} ${iStartDoy} ${iEndDoy} ${gaRootDir} ${bbdrRootDir} ${inversionRootDir} ${usePrior} ${priorDir} ${beamDir} ${modisTileScaleFactor}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs()..."
    read_task_jobs ${jobname}

    submit_job ${jobname} ${command}
done
