#!/bin/bash

volume=$1 # shall be vol1, vol2, vol3 or vol4

while true
do
  echo "`date` : `pan_df -h /group_workspaces/cems2/qa4ecv/${volume}`"
  sleep 600
done
