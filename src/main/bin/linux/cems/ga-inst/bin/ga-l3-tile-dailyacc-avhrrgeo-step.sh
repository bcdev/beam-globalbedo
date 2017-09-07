#!/bin/bash

#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc_nologs.sh
. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc_nologs_priotest.sh

# script to set up one LSF job per daily accumulation for 24-day-interval accumulation, invoked
# from just one PMonitor execution for whole time window (i.e. one year and the wings) instead of
# one PMonitor execution per single day.

tile=$1
year=$2
startdoy=$3
enddoy=$4
modisTileScaleFactor=$5
gaRootDir=$6
bbdrRootDir=$7
dailyAccRootDir=$8
beamDir=$9

#step=24
step=96

# e.g. we have startdoy='000', enddoy='361'. Doy interval is always 8.
# we want to submit one job for each doy

for doystring in $(seq -w $startdoy 96 $enddoy); do   # -w takes care for leading zeros

    # set up job with 64 days at once to make it longer (several minutes) for better scheduling
    iEndDoy=$((10#$doystring + 95))  # for step=96 which covers whole period 1..365

    intervalStartDoy=$doystring  # 001, 095, 191,...
    intervalEndDoy=`printf '%03d\n' "$(( $iEndDoy > 366 ? 366 : $iEndDoy))"`  # 096, 192,..., 366

    task="ga-l3-tile-dailyacc-avhrrgeo"
    jobname="${task}-${tile}-${year}-${intervalStartDoy}-${intervalEndDoy}"
    command="./bin/${task}-beam.sh ${tile} ${year} ${intervalStartDoy} ${intervalEndDoy} ${modisTileScaleFactor} ${gaRootDir} ${bbdrRootDir} ${dailyAccRootDir} ${beamDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs..."
    read_task_jobs ${jobname}

    echo "calling submit_job..."
    submit_job ${jobname} ${command}
done

echo "all calls done from ga-env-l3-tile-inversion-dailyacc.sh" 
