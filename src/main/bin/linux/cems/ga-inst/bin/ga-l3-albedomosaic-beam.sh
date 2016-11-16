#!/bin/bash

year=$1
doy=$2
snowMode=$3
deg=$4
gaRootDir=$5
beamRootDir=$6


#brdfSinMosaicProduct=$gaRootDir/Mosaic/brdf/$snowMode/$deg/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.SIN.nc
#sinTargetDir=$gaRootDir/Mosaic/albedo/$snowMode/$deg
#if [ ! -d "$sinTargetDir" ]
#then
#   mkdir -p $sinTargetDir
#fi
#sinTarget=$sinTargetDir/GlobAlbedo.albedo.$snowMode.$deg.${year}${doy}.SIN.nc 
#if [ -f $brdfSinMosaicProduct ]
#then
#    echo "time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.albedo -Pdoy=$doy -SbrdfMergedProduct=$brdfSinMosaicProduct -e -f NetCDF4-GA-ALBEDO -t $sinTarget"
#    time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.albedo -Pdoy=$doy -SbrdfMergedProduct=$brdfSinMosaicProduct -e -f NetCDF4-GA-ALBEDO -t $sinTarget
#else
#    echo "Input $brdfSinMosaicProduct does not exist - exiting..."
#    exit 1
#fi
#
#status=$?
#echo "Status: $status"
#if [ "$status" -ne 0 ]; then
#   echo "exiting..."
#   exit 1
#fi

brdfPcMosaicProduct=$gaRootDir/Mosaic/brdf/$snowMode/$deg/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.PC.nc
pcTargetDir=$gaRootDir/Mosaic/albedo/$snowMode/$deg
if [ ! -d "$pcTargetDir" ]
then
   mkdir -p $pcTargetDir
fi
pcTarget=$pcTargetDir/GlobAlbedo.albedo.$snowMode.$deg.${year}${doy}.PC.nc
if [ -f $brdfPcMosaicProduct ]
then
    echo "time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.albedo -Pdoy=$doy -SbrdfMergedProduct=$brdfPcMosaicProduct -e -f NetCDF4-GA-ALBEDO -t $pcTarget"
    time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.albedo -Pdoy=$doy -SbrdfMergedProduct=$brdfPcMosaicProduct -e -f NetCDF4-GA-ALBEDO -t $pcTarget
else
    echo "Input $brdfPcMosaicProduct does not exist - exiting..."
    exit 1
fi

status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

echo `date`
