#!/bin/bash

year=$1
doy=$2
snowMode=$3
deg=$4
tileSize=$5
gaRootDir=$6
beamRootDir=$7

#if [ "$deg" == "005" ]
#then
#    scaling=6
#elif [ "$deg" == "025" ]
#then
#    scaling=30
#else
#    scaling=60
#fi


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

#sinTargetDir=$gaRootDir/Mosaic/brdf/$snowMode/$deg
#if [ ! -d "$sinTargetDir" ]
#then
#   mkdir -p $sinTargetDir
#fi
#sinTarget=$sinTargetDir/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.SIN.nc
#echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PinputProductTileSize=$tileSize -PreprojectToPlateCarre=false -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-BRDF -t $sinTarget"
#time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -PinputProductTileSize=$tileSize -PreprojectToPlateCarre=false -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-BRDF -t $sinTarget
#
#
#status=$?
#echo "Status: $status"
#if [ "$status" -ne 0 ]; then
#   echo "exiting..."
#   exit 1
#fi

pcTargetDir=$gaRootDir/Mosaic/brdf/$snowMode/$deg
if [ ! -d "$pcTargetDir" ]
then
   mkdir -p $pcTargetDir
fi
pcTarget=$pcTargetDir/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.PC.nc
echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -PreducedOutput=true -Pscaling=$scaling -PinputProductTileSize=$tileSize -PreprojectToPlateCarre=true -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-BRDF -t $pcTarget"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -PreducedOutput=true -Pscaling=$scaling -PinputProductTileSize=$tileSize -PreprojectToPlateCarre=true -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-BRDF -t $pcTarget


echo "Status: $status"
status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

echo `date`
