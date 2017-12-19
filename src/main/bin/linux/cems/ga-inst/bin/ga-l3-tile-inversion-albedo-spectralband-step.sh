#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo.sh

sensorID=$1
bandIndex=$2
tile=$3
year=$4
startdoy=$5
enddoy=$6
gaRootDir=$7
spectralSdrRootDir=$8
inversionRootDir=${9}
beamDir=${10}
albedoTargetDir=${11}  # remind the brackets if >= 10!!

# TODO: to be safe, add a wait loop (60 sec) here and check for completion of writing binary accumulator files in ../DailyAccs/$year/(No)Snow
# This is indicated by presence of marker files 'PROCESSED_ALL'
# continue when they are present or after 15min with a warning
# (assume that wing years are ready as they were processed before the center year and have less daily accs)

dailyAccNoSnowDir=$gaRootDir/SDR_spectral/band_${bandIndex}/DailyAcc/$year/$tile/NoSnow
dailyAccSnowDir=$gaRootDir/SDR_spectral/band_${bandIndex}/DailyAcc/$year/$tile/Snow

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
    task="ga-l3-tile-inversion-albedo-spectralband"
    jobname="${task}-${bandIndex}-${tile}-${year}-${iDoy}"
    command="./bin/${task}-beam.sh ${sensorID} ${bandIndex} ${tile} ${year} ${iDoy} ${gaRootDir} ${spectralSdrRootDir} ${inversionRootDir} ${beamDir} ${albedoTargetDir}"

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

