#!/bin/bash
KILLME=$1
echo "Killing pending and running jobs containing '$KILLME'..."
echo "Killing pending bjobs ..."
BJOBS_PENDING=`bjobs -aw | grep odanne |grep PEND |grep $KILLME | awk -F" " '{print $1}'`
bkill $BJOBS_PENDING
echo "Pending bjobs killed."

echo "Killing running bjobs ..."
BJOBS_RUNNING=`bjobs -rw | grep odanne |grep $KILLME | awk -F" " '{print $1}'`
bkill $BJOBS_RUNNING
echo "Running bjobs killed."
