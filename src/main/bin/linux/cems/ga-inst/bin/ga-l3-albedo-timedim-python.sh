#!/bin/bash

# wrapper script fpr python albedo timedim call 

sensorId=$1
tile=$2
year=$3
doy=$4
albedoSourceDir=$5
albedoTimedimDir=$6

echo "time python2.7 $GA_INST/bin/ga-l3-albedo-timedim-python.py $sensorId $tile $year $doy $albedoSourceDir $albedoTimedimDir"
time python2.7 $GA_INST/bin/ga-l3-albedo-timedim-python.py $sensorId $tile $year $doy $albedoSourceDir $albedoTimedimDir
