#!/bin/bash

year=$1
doy=$2
snowMode=$3
deg=$4
proj=$5
tileSize=$6
gaRootDir=$7
beamRootDir=$8

if [ "$deg" == "005" ]
then
    if [ "$tileSize" == "200" ]
    then
        scaling=1
    else
        scaling=6
    fi
else
    if [ "$tileSize" == "200" ]
    then
        scaling=10
    else
        scaling=60
    fi
fi

targetDir=$gaRootDir/Mosaic/brdf/$snowMode/$deg
if [ ! -d "$targetDir" ]
then
   mkdir -p $targetDir
fi
target=$targetDir/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.$proj.nc
echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputType=$snowMode -PinputFormat=NETCDF -Pscaling=$scaling -PinputProductTileSize=$tileSize -Preprojection=$proj -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-BRDF -t $target"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputType=$snowMode -PinputFormat=NETCDF -Pscaling=$scaling -PinputProductTileSize=$tileSize -Preprojection=$proj -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-BRDF -t $target


echo "Status: $status"
status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

echo `date`
