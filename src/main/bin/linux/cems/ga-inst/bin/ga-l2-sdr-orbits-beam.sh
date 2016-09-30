#!/bin/bash

l1bPath=$1
l1bBaseName=$2
sdrL2Dir=$3
sensor=$4
year=$5
month=$6
gaRootDir=$7
beamRootDir=$8

sdrFile=$sdrL2Dir/${l1bBaseName}_SDR.nc

if [ ! -e "$sdrL2Dir" ]
then
    mkdir -p $sdrL2Dir
fi

echo "time $beamRootDir/bin/gpt-d-l1b-bbdr-orbits.sh ga.l2 -e -c 8000M -Psensor=$sensor -PcomputeSdr=true -f NetCDF4-BEAM -t $sdrFile $l1bPath"
time $beamRootDir/bin/gpt-d-l1b-bbdr-orbits.sh ga.l2 -e -c 8000M -Psensor=$sensor -PcomputeSdr=true -f NetCDF4-BEAM -t $sdrFile $l1bPath

status=$?
echo "Status: $status"

#if [ $status = 0 ] 
# also check for existence of SDR orbit file (i.e. may not exist in case of time limit exceeded):
if [ $status = 0 ] && [ -e "$sdrFile" ]
then
    echo "SDR orbit product created."
    gzip $sdrFile
else
    echo "An error occured - no SDR orbit product created."
fi

echo `date`
