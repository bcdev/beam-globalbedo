#!/bin/bash

# GA function definitions

set -e

if [ -z "${GA_INST}" ]; then
    GA_INST=`pwd`
fi

GA_TASKS=${GA_INST}/tasks
GA_LOG=${GA_INST}/log

read_task_jobs() {
    echo "entered read_task_jobs()..."
}

submit_job() {
    jobname=$1
    command=$2
    echo "GA_INST: ${GA_INST}"
    echo "GA_LOG : ${GA_LOG}"
    echo "jobname: ${jobname}"
    echo "command: ${command}"

    # L3 daily acc:
    #bsubmit="bsub -W 180 -R rusage[mem=16000] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"
    #bsubmit="bsub -q short-serial -R rusage[mem=8000] -P ga_qa4ecv -cwd ${GA_INST} -J ${jobname} ${GA_INST}/${command} ${@:3}"
    #bsubmit="bsub -q short-serial -P ga_qa4ecv -cwd ${GA_INST} -J ${jobname} ${GA_INST}/${command} ${@:3}"

    #### THIS IS FOR THE PRIORITY TEST 20170103! REMOVE AFTER PROCESSING:
    #bsubmit="bsub -U root#2 -q short-serial -P ga_qa4ecv -cwd ${GA_INST} -J ${jobname} ${GA_INST}/${command} ${@:3}"
    ## RETRY 20170112:
    #bsubmit="bsub -U root#6 -q short-serial -cwd ${GA_INST} -J ${jobname} ${GA_INST}/${command} ${@:3}"
    ## RETRY 20170120:
    bsubmit="bsub -U root#8 -q short-serial -cwd ${GA_INST} -J ${jobname} ${GA_INST}/${command} ${@:3}"

    ####

    echo "bsubmit: $bsubmit"

    #if hostname | grep -qF 'lotus.jc.rl.ac.uk'
    #then
    #    echo "${bsubmit}"
    #    line=`${bsubmit}`
    #else
    #    echo "ssh -A lotus.jc.rl.ac.uk ${bsubmit}"
    #    line=`ssh -A lotus.jc.rl.ac.uk ${bsubmit}`
    #fi

    if hostname | grep -qF 'cems-sci1.cems.rl.ac.uk'
    then
        echo "${bsubmit}"
        line=`${bsubmit}`
    else
        echo "ssh -A cems-sci1.cems.rl.ac.uk ${bsubmit}"
        line=`ssh -A cems-sci1.cems.rl.ac.uk ${bsubmit}`
    fi

    echo ${line}
    if echo ${line} | grep -qF 'is submitted'
    then
        jobs=`echo ${line} | awk '{ print substr($2,2,length($2)-2) }'`
        #echo "${GA_LOG}/${jobname}.out/${jobs}" > ${GA_TASKS}/${jobname}.tasks
	echo "jobs: $jobs"
    else
        echo "`date -u +%Y%m%d-%H%M%S`: tasks for ${jobname} failed. Reason: was not submitted."
        exit 1
    fi
}