#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc_nologs.sh
#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc_nologs_priotest.sh

# script to set up one LSF job per daily accumulation for 32-day-interval accumulation, invoked
# from just one PMonitor execution for whole time window (i.e. one year and the wings) instead of
# one PMonitor execution per single day.

bandIndex=$1
tile=$2
year=$3
startdoy=$4
enddoy=$5
gaRootDir=$6
spectralSdrRootDir=$7
spectralDailyAccRootDir=$8
beamDir=${9}

# e.g. we have startdoy='000', enddoy='361'. Doy interval is always 32.
# we want to submit one job for each doy

for doystring in $(seq -w $startdoy 32 $enddoy); do   # -w takes care for leading zeros

    # set up job with 32 days at once to make it longer (several minutes) for better scheduling
    iEndDoy=$((10#$doystring + 31))  # for step=32 which covers whole period 1..365

    intervalStartDoy=$doystring  # 001, 031, 063,...
    intervalEndDoy=`printf '%03d\n' "$(( $iEndDoy > 366 ? 366 : $iEndDoy))"`  # 032, 064,..., 366

    task="ga-l3-tile-dailyacc-spectral"
    jobname="${task}-${bandIndex}-${tile}-${year}-${intervalStartDoy}-${intervalEndDoy}"
    command="./bin/${task}-beam.sh ${bandIndex} ${tile} ${year} ${intervalStartDoy} ${intervalEndDoy} ${gaRootDir} ${spectralSdrRootDir} ${spectralDailyAccRootDir} ${beamDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs..."
    read_task_jobs ${jobname}

    echo "calling submit_job..."
    submit_job ${jobname} ${command}
done

echo "all calls done from ga-l3-tile-dailyacc-spectral-step.sh." 
