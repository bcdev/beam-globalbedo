#!/bin/bash

year=$1
doy=$2
snowMode=$3
gaRootDir=$4
beamRootDir=$5

scaling=1
deg=005
tileSize=1200
proj=PC

targetDir=$gaRootDir/Mosaic/Albedo_spectral/$snowMode/$year
if [ ! -d "$targetDir" ]
then
   mkdir -p $targetDir
fi
target=$targetDir/Qa4ecv.albedo.spectral.$snowMode.$deg.$year$doy.$proj.nc

echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo.spectral.qa4ecv -c 3000M -PsnowMode=$snowMode -Pscaling=$scaling -PinputProductTileSize=$tileSize -Preprojection=$proj -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-BEAM -t $target"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo.spectral.qa4ecv -c 3000M -PsnowMode=$snowMode -Pscaling=$scaling -PinputProductTileSize=$tileSize -Preprojection=$proj -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-BEAM -t $target

status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

echo `date`
