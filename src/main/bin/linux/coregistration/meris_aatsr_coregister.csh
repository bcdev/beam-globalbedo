#!/bin/tcsh

# wrapper script fpr python coregistration call 

echo "time python ./python_scripts/AatsrMerisCoregisterNc4.py $1 $2 $3 $4 $5 $6"
time python ./python_scripts/AatsrMerisCoregisterNc4.py $1 $2 $3 $4 $5 $6
