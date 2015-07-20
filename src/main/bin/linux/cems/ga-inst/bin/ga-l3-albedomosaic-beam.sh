#!/bin/bash

year=$1
doy=$2
snowMode=$3
deg=$4
gaRootDir=$5
beamRootDir=$6


brdfSinMosaicProduct=$gaRootDir/Mosaic/brdf/$snowMode/$deg/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.SIN.dim
sinTarget=$gaRootDir/Mosaic/albedo/$snowMode/$deg/GlobAlbedo.albedo.$snowMode.$deg.${year}${doy}.SIN.dim 
if [ -f $brdfSinMosaicProduct ]
then
    echo "time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.brdfmosaic.albedomosaic -Pdoy=$doy -SbrdfMosaicProduct=$brdfSinMosaicProduct -e -t $sinTarget"
    time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.brdfmosaic.albedomosaic -Pdoy=$doy -SbrdfMosaicProduct=$brdfSinMosaicProduct -e -t $sinTarget
else
    echo "Input $brdfSinMosaicProduct does not exist - exiting..."
    exit 1
fi

status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

brdfPcMosaicProduct=$gaRootDir/Mosaic/brdf/$snowMode/$deg/GlobAlbedo.brdf.$snowMode.$deg.$year$doy.PC.dim
pcTarget=$gaRootDir/Mosaic/albedo/$snowMode/$deg/GlobAlbedo.albedo.$snowMode.$deg.${year}${doy}.PC.dim
if [ -f $brdfPcMosaicProduct ]
then
    echo "time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.brdfmosaic.albedomosaic -Pdoy=$doy -SbrdfMosaicProduct=$brdfPcMosaicProduct -e -t $pcTarget"
    time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.brdfmosaic.albedomosaic -Pdoy=$doy -SbrdfMosaicProduct=$brdfPcMosaicProduct -e -t $pcTarget
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
