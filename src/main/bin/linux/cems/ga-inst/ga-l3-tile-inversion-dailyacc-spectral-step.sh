#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l3-tile-inversion-dailyacc.sh

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
subStartX=$6
subStartY=$7
gaRootDir=$8
spectralSdrRootDir=$9
beamDir=${10}

# e.g. we have startdoy='000', enddoy='361'. Doy interval is always 8.
# we want to submit one job for each doy

for iStartDoy in $(seq -w $startdoy $step $enddoy); do   # -w takes care for leading zeros
    iEndDoy=`printf '%03d\n' "$((10#$iStartDoy + 7))"`

    task="ga-l3-tile-inversion-dailyacc-spectral"
    jobname="${task}-${tile}-${year}-${iStartDoy}-${subStartX}-${subStartY}-dailyacc"
    command="./bin/${task}-beam.sh ${tile} ${year} ${iStartDoy} ${iEndDoy} ${modisTileScaleFactor} ${subStartX} ${subStartY} ${gaRootDir} ${spectralSdrRootDir} ${beamDir}"

    echo "jobname: $jobname"
    echo "command: $command"

    echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

    echo "calling read_task_jobs..."
    read_task_jobs ${jobname}

    if [ -z ${jobs} ]; then
        echo "calling submit_job..."
        submit_job ${jobname} ${command}
    fi

done

for iStartDoy in $(seq -w $startdoy $step $enddoy); do
    task="ga-l3-tile-inversion-dailyacc-spectral"
    jobname="${task}-${tile}-${year}-${iStartDoy}-${subStartX}-${subStartY}-dailyacc"
    
    echo "calling wait_for_task_jobs_completion: $jobname"
    wait_for_task_jobs_completion ${jobname}
done

echo "all calls done from ga-env-l3-tile-inversion-dailyacc-spectral-step.sh." 
