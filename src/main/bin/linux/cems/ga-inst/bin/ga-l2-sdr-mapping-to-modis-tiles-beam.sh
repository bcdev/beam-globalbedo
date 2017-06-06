#!/bin/bash

allSdrPathsString=$1
sdrTileDir=$2
sensor=$3
gaRootDir=$4
beamRootDir=$5

echo "BEAM allSdrPathsString: $allSdrPathsString"
echo "BEAM sdrTileDir: $sdrTileDir"
echo "BEAM sensor: $sensor"

if [ ! -e "$sdrTileDir" ]
then
    mkdir -p $sdrTileDir
fi

echo "Create SDR tile products..."

#IFS=';' read -a allSdrPaths <<< "$allSdrPathsString"
IFS=',' read -a allSdrPaths <<< "$allSdrPathsString"

echo "BEAM allSdrPaths: ${allSdrPaths[@]}"

for sdrPath in "${allSdrPaths[@]}"
do
    srcFileName=`basename $sdrPath .nc`
    target=$sdrTileDir/${srcFileName}_mapped.nc 
    echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.modis.spectral -e -SsourceProduct=$sdrPath -Psensor=$sensor -PsingleBandIndex=3 -f NetCDF4-BEAM -t $target"
    time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.modis.spectral -e -SsourceProduct=$sdrPath -Psensor=$sensor -PsingleBandIndex=3 -f NetCDF4-BEAM -t $target
done

status=$?
echo "Status: $status"

echo `date`
