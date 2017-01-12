#!/bin/bash

sdrPath=$1
sdrTileDir=$2
sensor=$3
gaRootDir=$4
beamRootDir=$5

# todo 20160528: configure if we want to process SDR or BBDR!

if [ ! -e "$sdrTileDir" ]
then
    mkdir -p $sdrTileDir
fi

echo "Create SDR tile products..."

# SDR orbits --> tiles:
#echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile -e -c 8000M -q 8 -PbbdrDir=$sdrTileDir -PsdrOnly=true $sdrPath"
#time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile -e -c 8000M -q 8 -PbbdrDir=$sdrTileDir -PsdrOnly=true $sdrPath

# BBDR orbits --> tiles:
echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile -e -c 8000M -q 8 -PbbdrDir=$sdrTileDir -PsdrOnly=false $sdrPath"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile -e -c 8000M -q 8 -PbbdrDir=$sdrTileDir -PsdrOnly=false $sdrPath

status=$?
echo "Status: $status"

#echo "Removing SDR intermediate product..."
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
