#!/bin/bash

l1bPath=$1
l1bBaseName=$2
bbdrL2Dir=$3
bbdrTileDir=$4
sensor=$5
year=$6
month=$7
gaRootDir=$8
beamRootDir=$9

bbdrFile=$bbdrL2Dir/${l1bBaseName}_BBDR_${sensor}.nc

if [ ! -e "$bbdrL2Dir" ]
then
    mkdir -p $bbdrL2Dir
fi
if [ ! -e "$bbdrTileDir" ]
then
    mkdir -p $bbdrTileDir
fi

#echo "time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.l2 -e -c 8000M -q 24 -Psensor=$sensor -PsubsetAatsr=true -f NetCDF4-BEAM -t $bbdrFile $l1bPath"
#time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.l2 -e -c 8000M -q 24 -Psensor=$sensor -PsubsetAatsr=true -f NetCDF4-BEAM -t $bbdrFile $l1bPath
echo "time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.l2 -e -c 8000M -q 24 -Psensor=$sensor -f NetCDF4-BEAM -t $bbdrFile $l1bPath"
time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.l2 -e -c 8000M -q 24 -Psensor=$sensor -f NetCDF4-BEAM -t $bbdrFile $l1bPath


status=$?
echo "Status: $status"
if [ $status = 0 ] 
then
    echo "BBDR intermediate product created."
    echo "Now create BBDR tile products..."

    time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile -e -c 8000M -q 8 -PbbdrDir=$bbdrTileDir  $bbdrFile
    status=$?
    echo "Status: $status"
    echo "BBDR tile products created."
else
    echo "No BBDR products created."
    echo "Status: -1"
fi

#echo "Removing BBDR intermediate product..."
#if [ -e "${bbdrL2Dir}/${l1bBaseName}_BBDR.nc" ] 
#then
#    rm -f ${bbdrL2Dir}/${l1bBaseName}_BBDR.nc
#    #rm -f ${bbdrL2Dir}/${l1bBaseName}_BBDR.dim
#    #rm -rf ${bbdrL2Dir}/${l1bBaseName}_BBDR.data
#fi

#if [ $sensor = "VGT" ] 
#then
#    echo "Removing tmp products..."
#    rm -rf /tmp/*
#    rm -f $l1bRootDir/$year/$l1bBaseName
#fi

echo `date`
