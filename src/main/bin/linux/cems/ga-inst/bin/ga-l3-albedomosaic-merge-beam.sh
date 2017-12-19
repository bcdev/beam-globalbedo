#!/bin/bash

year=$1
doy=$2
deg=$3
proj=$4
gaRootDir=$5
beamRootDir=$6

noSnowProduct=$gaRootDir/Mosaic/Albedo/NoSnow/avh_geo/$year/$deg/Qa4ecv.albedo.avh_geo.NoSnow.$deg.${year}${doy}.$proj.nc
snowProduct=$gaRootDir/Mosaic/Albedo/Snow/avh_geo/$year/$deg/Qa4ecv.albedo.avh_geo.Snow.$deg.${year}${doy}.$proj.nc

targetDir=$gaRootDir/Mosaic/Albedo/Merge/avh_geo/$year/$deg
if [ ! -d "$targetDir" ]
then
   mkdir -p $targetDir
fi
target=$targetDir/Qa4ecv.albedo.avh_geo.Merge.$deg.${year}${doy}.$proj.nc

if [ -f $noSnowProduct ] && [ -f $snowProduct ]
then
    echo "time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.mergealbedo -SnoSnowProduct=$noSnowProduct -SsnowProduct=$snowProduct -e -f NetCDF4-GA-ALBEDO -t $target"
    time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.mergealbedo -SnoSnowProduct=$noSnowProduct -SsnowProduct=$snowProduct -e -f NetCDF4-GA-ALBEDO -t $target
else
    if [ ! -f $noSnowProduct ]
    then
        echo "Input $noSnowProduct does not exist - exiting..."
    else
        echo "Input $snowProduct does not exist - exiting..."
    fi
    exit 1
fi

status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

echo `date`
