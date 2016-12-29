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

srcFileName=`basename $sdrPath .nc`
# SDR orbits --> tiles:
target=$sdrTileDir/${srcFileName}_mapped.nc 
echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.modis.spectral -e -SsourceProduct=$sdrPath -Psensor=$sensor -f NetCDF4-BEAM -t $target"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.modis.spectral -e -SsourceProduct=$sdrPath -Psensor=$sensor -f NetCDF4-BEAM -t $target

status=$?
echo "Status: $status"

echo `date`