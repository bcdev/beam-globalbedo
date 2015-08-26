#!/bin/bash

# GA function definitions
# useage ${ga.home}/bin/ga-env.sh  (in xxx-start.sh and xxx-run.sh)

set -e

if [ -z "${GA_INST}" ]; then
    GA_INST=`pwd`
fi

GA_TASKS=${GA_INST}/tasks
GA_LOG=${GA_INST}/log

read_task_jobs() {
    echo "entered read_task_jobs()..."
    jobname=$1
    echo "jobname: $jobname"
    jobs=
    echo "GA_TASKS/jobname.tasks: ${GA_TASKS}/${jobname}.tasks"
    if [ -e ${GA_TASKS}/${jobname}.tasks ]
    then
        for logandid in `cat ${GA_TASKS}/${jobname}.tasks`
        do
	    echo "logandid: $logandid"
            job=`basename ${logandid}`
            log=`dirname ${logandid}`
	    echo "job: $job"
	    echo "log: $log"
            #if grep -qF 'Successfully completed.' ${log}
            if ! grep -qF 'Status: 1' ${log}
            then
                if [ "${jobs}" != "" ]
                then
                    jobs="${jobs}|${job}"
                else
                    jobs="${job}"
                fi
            fi
        done
    fi
}

wait_for_task_jobs_completion() {
    jobname=$1
    while true
    do
        sleep 10
        # Output of bjobs command (example from SST CCI):
        # JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME
        # 619450  rquast  RUN   lotus      lotus.jc.rl host042.jc. *r.n10-sub Aug 14 10:15
        # 619464  rquast  RUN   lotus      lotus.jc.rl host087.jc. *r.n11-sub Aug 14 10:15
        # 619457  rquast  RUN   lotus      lotus.jc.rl host209.jc. *r.n12-sub Aug 14 10:15
        # 619458  rquast  RUN   lotus      lotus.jc.rl host209.jc. *r.n11-sub Aug 14 10:15
        # 619452  rquast  RUN   lotus      lotus.jc.rl host043.jc. *r.n10-sub Aug 14 10:15
        if bjobs -P ga_qa4ecv | egrep -q "^$jobs\\>"
        then
            continue
        fi

        if [ -s ${GA_TASKS}/${jobname}.tasks ]
        then
            for logandid in `cat ${GA_TASKS}/${jobname}.tasks`
            do
                job=`basename ${logandid}`
                log=`dirname ${logandid}`

                if [ -s ${log} ]
                then
                    #if ! grep -qF 'Successfully completed.' ${log}
                    if grep -qF 'Status: 1' ${log} 
                    then
                        echo "tail -n10 ${log}"
                        tail -n10 ${log}
                        echo "`date -u +%Y%m%d-%H%M%S`: tasks for ${jobname} failed. Reason: see ${log}"
                        exit 1
                    else
                        echo "`date -u +%Y%m%d-%H%M%S`: tasks for ${jobname} done"
                        exit 0
                    fi
                else
                        echo "`date -u +%Y%m%d-%H%M%S`: logfile ${log} for job ${job} not found"
                fi
            done
        fi
    done
}

submit_job() {
    jobname=$1
    command=$2
    memory=$3

    #bsubmit="bsub -R rusage[mem=20480] -q lotus -n 1 -W 8:00 -P esacci_sst -cwd ${MMS_INST} -oo ${MMS_LOG}/${jobname}.out -eo ${MMS_LOG}/${jobname}.err -J ${jobname} ${mms.home}/bin/${command} ${@:3}"
    echo "GA_INST: ${GA_INST}"
    echo "GA_LOG : ${GA_LOG}"
    echo "jobname: ${jobname}"
    echo "command: ${command}"

    mem_to_use=8192
    if [ -n "$memory" ]
    then
        mem_to_use=$memory
    fi
    echo "mem_to_use: ${mem_to_use}"

    # for L1b, L2:
    #bsubmit="bsub -R rusage[mem=16384] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"
    # for mosaicing: TODO: make configurable
    bsubmit="bsub -R rusage[mem=65536] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"
    
    #bsubmit="bsub -R rusage[mem=${mem_to_use}] -P ga_qa4ecv -cwd ${GA_INST} -oo ${GA_LOG}/${jobname}.out -eo ${GA_LOG}/${jobname}.err -J ${jobname} ${GA_INST}/${command} ${@:3}"

    echo "bsubmit: $bsubmit"

    rm -f ${GA_LOG}/${jobname}.out
    rm -f ${GA_LOG}/${jobname}.err

#    echo "${bsubmit}"
#    line=`${bsubmit}`
#    if hostname | grep -qF 'cems-sci1.cems.rl.ac.uk'
#    if hostname | grep -qF 'jasmin-sci1.ceda.ac.uk'
    if hostname | grep -qF 'lotus.jc.rl.ac.uk'
    then
        echo "${bsubmit}"
        line=`${bsubmit}`
    else
        echo "ssh -A lotus.jc.rl.ac.uk ${bsubmit}"
#        echo "ssh -A cems-sci1.cems.rl.ac.uk ${bsubmit}"
#        echo "ssh -A jasmin-sci1.ceda.ac.uk ${bsubmit}"
        line=`ssh -A lotus.jc.rl.ac.uk ${bsubmit}`
#        line=`ssh -A cems-sci1.cems.rl.ac.uk ${bsubmit}`
#        line=`ssh -A jasmin-sci1.ceda.ac.uk ${bsubmit}`
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
