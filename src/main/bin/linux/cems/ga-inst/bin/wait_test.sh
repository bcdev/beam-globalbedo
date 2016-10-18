#!/bin/bash

waitCount=0
while [  "$waitCount" -lt "10" ]; do
    markerNoSnow=bla
    markerSnow=blubb
    thedate=`date`
    echo "Waiting for $waitCount minutes for marker files... date: $thedate"
    if [ -f "$markerNoSnow" ] && [ -f "$markerSnow" ]; then
        echo "Daily acc completed - ready for inversion."
        break
    fi
    let waitCount=waitCount+1
    sleep 20
done
if [ "$waitCount" -ge 10 ]; then
    echo "WARNING: no marker files."
fi

echo "done"
