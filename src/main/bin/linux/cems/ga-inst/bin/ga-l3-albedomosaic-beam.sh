#!/bin/bash

year=$1
doy=$2
snowMode=$3
deg=$4
proj=$5
gaRootDir=$6
beamRootDir=$7

brdfMosaicProduct=$gaRootDir/Mosaic/brdf/$snowMode/$deg/Qa4ecv.brdf.$snowMode.$deg.$year$doy.$proj.nc
targetDir=$gaRootDir/Mosaic/albedo/$snowMode/$year/$deg
if [ ! -d "$targetDir" ]
then
   mkdir -p $targetDir
fi
target=$targetDir/Qa4ecv.albedo.$snowMode.$deg.${year}${doy}.$proj.nc
if [ -f $brdfMosaicProduct ]
then
    echo "time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.albedo -Pdoy=$doy -SbrdfMergedProduct=$brdfMosaicProduct -e -f NetCDF4-GA-ALBEDO -t $target"
    time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.albedo -Pdoy=$doy -SbrdfMergedProduct=$brdfMosaicProduct -e -f NetCDF4-GA-ALBEDO -t $target
else
    echo "Input $brdfMosaicProduct does not exist - exiting..."
    exit 1
fi

status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

echo `date`
