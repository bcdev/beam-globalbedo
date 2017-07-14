#!/bin/bash

# wrapper script for python albedo mosaic timedim call 

sensorId=$1
year=$2
doy=$3
res=$4
snowMode=$5
albedoSourceDir=$6
albedoTimedimDir=$7

echo "time python2.7 $GA_INST/bin/ga-l3-albedo-mosaic-timedim-python.py $sensorId $year $doy $res $snowMode $albedoSourceDir $albedoTimedimDir"
time python2.7 $GA_INST/bin/ga-l3-albedo-mosaic-timedim-python.py $sensorId $year $doy $res $snowMode $albedoSourceDir $albedoTimedimDir
