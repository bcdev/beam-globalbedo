#!/bin/bash

l1bPath=$1
l1bBaseName=$2
sdrL2Dir=$3
sdrTileDir=$4
sensor=$5
year=$6
month=$7
gaRootDir=$8
beamRootDir=$9

sdrFile=$sdrL2Dir/${l1bBaseName}_SDR.nc

if [ ! -e "$sdrL2Dir" ]
then
    mkdir -p $sdrL2Dir
fi
if [ ! -e "$sdrTileDir" ]
then
    mkdir -p $sdrTileDir
fi

echo "time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.l2 -e -c 8000M -q 24 -Psensor=$sensor -PcomputeSdr=true -f NetCDF4-BEAM -t $sdrFile $l1bPath"
time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.l2 -e -c 8000M -q 24 -Psensor=$sensor -PcomputeSdr=true -f NetCDF4-BEAM -t $sdrFile $l1bPath

status=$?
echo "Status: $status"

#if [ $status = 0 ] 
# als check for existence of SDR orbit file (i.e. may not exist in case of time limit exceeded):
if [ $status = 0 ] && [ -e "$sdrFile" ]
then
    echo "SDR orbit product created."
    echo "Now create SDR tile products..."

    echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile -e -c 8000M -q 8 -PbbdrDir=$sdrTileDir -PsdrOnly=true $sdrFile"
    time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile -e -c 8000M -q 8 -PbbdrDir=$sdrTileDir -PsdrOnly=true $sdrFile
    status=$?
    echo "Status: $status"
    echo "SDR tile products created."
else
    echo "No SDR products created."
fi

echo "Removing SDR intermediate product..."
#if [ -e "${sdrL2Dir}/${l1bBaseName}_SDR.nc" ] 
#then
#    rm -f ${sdrL2Dir}/${l1bBaseName}_SDR.nc
#fi

#if [ $sensor = "VGT" ] 
#then
#    echo "Removing tmp products..."
#    rm -rf /tmp/*
#    rm -f $l1bRootDir/$year/$l1bBaseName
#fi

echo `date`
