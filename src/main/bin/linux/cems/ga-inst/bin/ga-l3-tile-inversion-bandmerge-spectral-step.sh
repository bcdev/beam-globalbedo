#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-albedo_nologs_priotest.sh

tile=$1
year=$2
startdoy=$3
enddoy=$4
gaRootDir=$5
inversionRootDir=${6}
beamDir=${7}


for doystring in $(seq -w $startdoy 64 $enddoy); do   # -w takes care for leading zeros

    # set up job with 64 days at once to make it longer (several minutes) for better scheduling
    iEndDoy=$((10#$doystring + 63))  # for step=64 which covers whole period 1..365

    intervalStartDoy=$doystring  # 001, 063, 127,...
    intervalEndDoy=`printf '%03d\n' "$(( $iEndDoy > 366 ? 366 : $iEndDoy))"`  # 064, 129, 193,...

    task="ga-l3-tile-inversion-bandmerge-spectral"
    jobname="${task}-${tile}-${year}-${intervalStartDoy}-${intervalEndDoy}"
    command="./bin/${task}-beam.sh ${tile} ${year} ${intervalStartDoy} ${intervalEndDoy} ${gaRootDir} ${inversionRootDir} ${beamDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs()..."
    read_task_jobs ${jobname}

    submit_job ${jobname} ${command}
done

