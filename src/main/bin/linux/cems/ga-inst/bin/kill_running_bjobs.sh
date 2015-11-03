#!/bin/bash
echo "Killing running bjobs ..."
BJOBS_RUNNING=`bjobs -r | grep odanne | awk -F" " '{print $1}'`
bkill $BJOBS_RUNNING
echo "Running bjobs killed."
