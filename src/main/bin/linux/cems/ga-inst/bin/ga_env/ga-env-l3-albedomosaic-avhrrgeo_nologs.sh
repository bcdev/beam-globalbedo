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

    # L3 albedo mosaics:
    # for AVHRRGEO (200x200 tiles):
    #bsubmit="bsub -W 120 -R rusage[mem=24000] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"
    # NEW queue, 201611:
    #bsubmit="bsub -q short-serial -W 120 -R rusage[mem=24000] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"
    # hopefully enough time and memory if we have just 2 test output bands (BHR_SW, WNS):
    #bsubmit="bsub -q short-serial -W 120 -R rusage[mem=16000] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"

    # stress test:
    #bsubmit="bsub -q short-serial -W 180 -R rusage[mem=40000] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"
    # for PC, we are fine with 24GB
    #bsubmit="bsub -q short-serial -W 180 -R rusage[mem=24000] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"
    # add NEW -M option (helpdesk advice, 20170424)!!
    #bsubmit="bsub -U root#14 -q short-serial -W 180 -R rusage[mem=32000] -M 32000000 -P ga_qa4ecv -cwd ${GA_INST} -J ${jobname} ${GA_INST}/${command} ${@:3}"
    # no longer priority, 201711:
    bsubmit="bsub -q short-serial -W 180 -R rusage[mem=32000] -M 32000000 -P ga_qa4ecv -cwd ${GA_INST} -J ${jobname} ${GA_INST}/${command} ${@:3}"

    #### THIS IS FOR PRIORITY TEST 20170421! REMOVE AFTER THAT DAY!
    #bsubmit="bsub -U QA4ECV001 -q short-serial -W 180 -R rusage[mem=24000] -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"
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
        echo "${GA_LOG}/${jobname}.out/${jobs}" > ${GA_TASKS}/${jobname}.tasks
	echo "jobs: $jobs"
    else
        echo "`date -u +%Y%m%d-%H%M%S`: tasks for ${jobname} failed. Reason: was not submitted."
        exit 1
    fi
}
