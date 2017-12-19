#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs_priotest.sh

sensorID=$1
bandIndex=$2
tile=$3
year=$4
startdoy=$5
enddoy=$6
gaRootDir=$7
inversionRootDir=${8}
spectralDailyAccRootDir=${9}
beamDir=${10}


for doystring in $(seq -w $startdoy 32 $enddoy); do   # -w takes care for leading zeros

    # set up job with 32 days at once to make it longer (several minutes) for better scheduling
    iEndDoy=$((10#$doystring + 31))  # for step=32 which covers whole period 1..365

    intervalStartDoy=$doystring  # 031, 063, 095,...
    intervalEndDoy=`printf '%03d\n' "$(( $iEndDoy > 366 ? 366 : $iEndDoy))"`  # 032, 064, 096,...

    task="ga-l3-tile-inversion-spectral"
    jobname="${task}-${bandIndex}-${tile}-${year}-${intervalStartDoy}-${intervalEndDoy}"
    command="./bin/${task}-beam.sh ${sensorID} ${bandIndex} ${tile} ${year} ${intervalStartDoy} ${intervalEndDoy} ${gaRootDir} ${inversionRootDir} ${spectralDailyAccRootDir} ${beamDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs()..."
    read_task_jobs ${jobname}

    submit_job ${jobname} ${command}
done

