#!/bin/bash

year=$1
doy=$2
gaRootDir=$3
albedodiffRootDir=$4
beamRootDir=$5

res=005

sinTargetDir=$gaRootDir/Mosaic/Albedo/diff_old_new
if [ ! -d "$sinTargetDir" ]
then
   mkdir -p $sinTargetDir
fi
sinTarget=$sinTargetDir/GlobAlbedo.albedo.diffoldnew.$res.$year$doy.SIN.nc
echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo -c 3000M -PinputFormat=NETCDF -Pscaling=6 -PreprojectToPlateCarre=false -Pyear=$year -Pdoy=$doy -PgaRootDir=$albedodiffRootDir -e -f NetCDF4-BEAM -t $sinTarget"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo -c 3000M -PinputFormat=NETCDF -Pscaling=6 -PreprojectToPlateCarre=false -Pyear=$year -Pdoy=$doy -PgaRootDir=$albedodiffRootDir -e -f NetCDF4-BEAM -t $sinTarget


status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

#pcTargetDir=$gaRootDir/Mosaic/Albedo/diff_old_new
#if [ ! -d "$pcTargetDir" ]
#then
#   mkdir -p $pcTargetDir
#fi
#pcTarget=$pcTargetDir/GlobAlbedo.albedo.diffoldnew.$res.$year$doy.PC.nc
#echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo -c 3000M -PinputFormat=NETCDF -Pscaling=6 -PreprojectToPlateCarre=true -Pyear=$year -Pdoy=$doy -PgaRootDir=$albedodiffRootDir -e -f NetCDF4-BEAM -t $pcTarget"
#time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo -c 3000M -PinputFormat=NETCDF -Pscaling=6 -PreprojectToPlateCarre=true -Pyear=$year -Pdoy=$doy -PgaRootDir=$albedodiffRootDir -e -f NetCDF4-BEAM -t $pcTarget
#
#echo "Status: $status"
#status=$?
#echo "Status: $status"
#if [ "$status" -ne 0 ]; then
#   echo "exiting..."
#   exit 1
#fi

echo `date`
