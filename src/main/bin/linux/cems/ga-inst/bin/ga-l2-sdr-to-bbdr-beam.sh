#!/bin/bash

sdrPath=$1
sdrBaseName=$2
bbdrDir=$3
sensor=$4
gaRootDir=$5
beamRootDir=$6

bbdrFile=$bbdrDir/${sdrBaseName}_BBDR.nc

if [ ! -e "$bbdrDir" ]
then
    mkdir -p $bbdrDir
fi

echo "time $beamRootDir/bin/gpt-d-l1b-bbdr-orbits.sh ga.l2 -e -SsourceProduct=$sdrPath -Psensor=$sensor -PcomputeBbdrFromSdr=true -f NetCDF4-BEAM -t $bbdrFile"
time $beamRootDir/bin/gpt-d-l1b-bbdr-orbits.sh ga.l2 -e -SsourceProduct=$sdrPath -Psensor=$sensor -PcomputeBbdrFromSdr=true -f NetCDF4-BEAM -t $bbdrFile

status=$?
echo "Status: $status"

# also check for existence of BBDR file (i.e. may not exist in case of time limit exceeded):
if [ $status = 0 ] && [ -e "$bbdrFile" ]
then
    echo "BBDR product created from SDR input."
    gzip $bbdrFile
else
    echo "An error occured - no BBDR product created."
    echo "Status: 1"
fi

echo `date`
