#!/bin/bash

year=$1
doy=$2
snowMode=$3
deg=$4
scaling=$5
gaRootDir=$6
beamRootDir=$7

sinTargetDir=$gaRootDir/Mosaic/brdf/$snowMode/$deg
if [ ! -d "$sinTargetDir" ]
then
   mkdir -p $sinTargetDir
fi
#sinTarget=$sinTargetDir/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.SIN.nc
#echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PreprojectToPlateCarre=false -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-BEAM -t $sinTarget"
#time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PreprojectToPlateCarre=false -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-BEAM -t $sinTarget
sinTarget=$sinTargetDir/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.SIN.dim
echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PreprojectToPlateCarre=false -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -t $sinTarget"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PreprojectToPlateCarre=false -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -t $sinTarget


status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

pcTargetDir=$gaRootDir/Mosaic/brdf/$snowMode/$deg
if [ ! -d "$pcTargetDir" ]
then
   mkdir -p $pcTargetDir
fi
#pcTarget=$pcTargetDir/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.PC.nc
#echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PreprojectToPlateCarre=true -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-BEAM -t $pcTarget"
#time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PreprojectToPlateCarre=true -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-BEAM -t $pcTarget
pcTarget=$pcTargetDir/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.PC.dim
echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PreprojectToPlateCarre=true -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -t $pcTarget"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PreprojectToPlateCarre=true -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -t $pcTarget

echo "Status: $status"
status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

echo `date`
