#!/bin/bash

#. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc.sh
. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc_nologs.sh

# test script to set up one LSF job per single 8-day-interval accumulation, but invoked
# from just one PMonitor execution for whole time window (i.e. one year or the wings) instead of
# one PMonitor execution per single 8-day-interval (old setup).
# --> many bsubs are supervised by one PMonitor
# --> should allow to feed many more jobs into the LSF queue for same PMonitor limit (e.g. 192)

tile=$1
year=$2
startdoy=$3
enddoy=$4
step=$5
modisTileScaleFactor=$6
gaRootDir=$7
bbdrRootDir=$8
beamDir=$9


# e.g. we have startdoy='000', enddoy='361'. Doy interval is always 8.
# we want to submit one job for each doy

for iStartDoy in $(seq -w $startdoy $step $enddoy); do   # -w takes care for leading zeros
    iEndDoy=`printf '%03d\n' "$((10#$iStartDoy + 7))"`

    task="ga-l3-tile-inversion-dailyacc-avhrrgeo"
    jobname="${task}-${tile}-${year}-${iStartDoy}-dailyacc"
    command="./bin/${task}-beam.sh ${tile} ${year} ${iStartDoy} ${iEndDoy} ${modisTileScaleFactor} ${gaRootDir} ${bbdrRootDir} ${beamDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs..."
    read_task_jobs ${jobname}

    #if [ -z ${jobs} ]; then
    #    echo "calling submit_job..."
    #    submit_job ${jobname} ${command}
    #fi
    echo "calling submit_job..."
    submit_job ${jobname} ${command}

done

echo "all calls done from ga-env-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh" 
