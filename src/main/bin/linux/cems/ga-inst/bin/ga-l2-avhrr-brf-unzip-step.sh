#!/bin/bash

. ${GA_INST}/bin/ga_env/ga-env-l2-avhrr-brf-unzip.sh

bbdrPath=$1
gaRootDir=$2

# unpack bz2
# e.g. AVH_19980701_001D_900S900N1800W1800E_0005D_BRDF_N14.NC.bz2
bbdrNcFileName=`basename $bbdrPath .NC.bz2`
bbdrInputProduct=$gaRootDir/tmp/${bbdrNcFileName}.nc
#chmod 644 $bbdrInputProduct

task="ga-l2-avhrr-brf-unzip"
jobname="${task}-${bbdrNcFileName}"
command="mybzip2 -dc $bbdrPath > $bbdrInputProduct"

echo "jobname: $jobname"
echo "command: $command"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for task ${task}"

echo "calling read_task_jobs()..."
read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname} 
