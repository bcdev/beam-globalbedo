#!/bin/bash

brfPath=$1
baseName=$2
bbdrDir=$3
gaRootDir=$4
beamRootDir=$5

if [ ! -e "$bbdrDir" ]
then
    mkdir -p $bbdrDir
fi

echo "Create Meteosat MVIRI/SEVIRI BBDR from BRF tile products..."

target=${bbdrDir}/${baseName}_BBDR.nc

echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.meteosat -e -c 3000M -SsourceProduct=$brfPath -f NetCDF4-BEAM -t $target"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.meteosat -e -c 3000M -SsourceProduct=$brfPath -f NetCDF4-BEAM -t $target
status=$?
echo "Status: $status"

echo `date`
