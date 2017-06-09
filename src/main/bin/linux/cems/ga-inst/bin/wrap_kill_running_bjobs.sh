
#!/bin/bash

while true
do

  echo "Killing pending bjobs ..."
  BJOBS_PENDING=`bjobs -aw | grep odanne |grep PEND |grep 2005 | awk -F" " '{print $1}'`
  bkill $BJOBS_PENDING
  echo "Pending bjobs killed."

  echo "Killing running bjobs ..."
  BJOBS_RUNNING=`bjobs -r | grep odanne | awk -F" " '{print $1}'`
  bkill $BJOBS_RUNNING
  echo "Running bjobs killed."
  sleep 30

done
