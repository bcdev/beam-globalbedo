#!/bin/bash
echo "Killing pending bjobs ..."
BJOBS_PENDING=`bjobs -a | grep odanne |grep PEND | awk -F" " '{print $1}'`
bkill $BJOBS_PENDING
echo "Pending bjobs killed."

echo "Killing running bjobs ..."
BJOBS_RUNNING=`bjobs -r | grep odanne | awk -F" " '{print $1}'`
bkill $BJOBS_RUNNING
echo "Running bjobs killed."
