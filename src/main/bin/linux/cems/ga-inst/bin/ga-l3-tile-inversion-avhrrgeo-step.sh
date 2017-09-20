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
dailyAccRootDir=$7
inversionRootDir=$8
usePrior=$9
priorDir=${10}
beamDir=${11}
modisTileScaleFactor=${12}  # remind the brackets if >= 10!!

dailyAccNoSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow

for doystring in $(seq -w $startdoy 64 $enddoy); do   # -w takes care for leading zeros
    
    # set up job with 64 days at once to make it longer (several minutes) for better scheduling
    iEndDoy=$((10#$doystring + 63))  # for step=64 which covers whole period 1..365

    intervalStartDoy=$doystring  # 001, 063, 127,...
    intervalEndDoy=`printf '%03d\n' "$(( $iEndDoy > 366 ? 366 : $iEndDoy))"`  # 064, 129, 193,...
    #intervalEndDoy=`printf '%03d\n' "$(( $iEndDoy > 121 ? 121 : $iEndDoy))"`  # test!!

    task="ga-l3-tile-inversion-avhrrgeo"
    jobname="${task}-${tile}-${year}-${intervalStartDoy}-${intervalEndDoy}"
    command="./bin/${task}-beam.sh ${sensorID} ${tile} ${year} ${intervalStartDoy} ${intervalEndDoy} ${gaRootDir} ${dailyAccRootDir} ${inversionRootDir} ${usePrior} ${priorDir} ${beamDir} ${modisTileScaleFactor}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs()..."
    read_task_jobs ${jobname}

    submit_job ${jobname} ${command}
done
