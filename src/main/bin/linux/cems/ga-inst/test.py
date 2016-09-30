__author__ = 'olafd'

# This is a script to clean for all lakes per year all results but not the shallow results

# example call: python clean_alllakes_per_year.py 2005

import os
import sys
import subprocess
import commands
import time

#if len(sys.argv) != 2:
#    print 'Usage:  python clean_alllakes_per_year.py 2005 <year>'
#    print 'example call:  python clean_alllakes_per_year.py 2005'
#    sys.exit(-1)

#root_folder = '/calvalus/projects/diversity/prototype/'
#year = sys.argv[1]

#regions = ['Lake-Victoria']

print 'starting...'


num_jobs_i = 100000
i = 1
while num_jobs_i > 5: 
    #bjobs_exit_cmd = "bjobs -a | grep odanne |grep DONE |wc | awk -F" " '{print $1}'"
    bjobs_exit_cmd = "ps -ef | grep odanne |wc | awk -F' ' '{print $1}'"
    print bjobs_exit_cmd
    status, num_jobs = commands.getstatusoutput(bjobs_exit_cmd)
    i += 1
    num_jobs_i = int(num_jobs) - i
    print 'num_jobs: ', num_jobs, ', ', num_jobs_i
    time.sleep(1.0)


bjobs_run_cmd = "bjobs -a | grep odanne |grep RUNNING |wc | awk -F' ' '{print $1}'"
status, run = commands.getstatusoutput(bjobs_run_cmd)
num_bjobs_run = int(run)
bjobs_pend_cmd = "bjobs -a | grep odanne |grep DONE |wc | awk -F' ' '{print $1}'"
status, pend = commands.getstatusoutput(bjobs_run_cmd)
num_bjobs_pend = int(pend)

while num_bjobs_run > 200 or num_bjobs_pend > 500:
    bjobs_run_cmd = "bjobs -a | grep odanne |grep RUNNING |wc | awk -F' ' '{print $1}'"
    status, run = commands.getstatusoutput(bjobs_run_cmd)
    num_bjobs_run = int(run)
    bjobs_pend_cmd = "bjobs -a | grep odanne |grep DONE |wc | awk -F' ' '{print $1}'"
    status, pend = commands.getstatusoutput(bjobs_run_cmd)
    num_bjobs_pend = int(pend)

    time.sleep(1.0)

print 'done'

