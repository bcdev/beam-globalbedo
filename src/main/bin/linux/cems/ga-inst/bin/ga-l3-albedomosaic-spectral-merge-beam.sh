#!/bin/bash

year=$1
doy=$2
gaRootDir=$3
beamRootDir=$4

deg=005
proj=PC

noSnowProduct=$gaRootDir/Mosaic/Albedo_spectral/NoSnow/$year/Qa4ecv.albedo.spectral.NoSnow.$deg.${year}${doy}.$proj.nc
snowProduct=$gaRootDir/Mosaic/Albedo_spectral/Snow/$year/Qa4ecv.albedo.spectral.Snow.$deg.${year}${doy}.$proj.nc

targetDir=$gaRootDir/Mosaic/Albedo_spectral/Merge/$year
if [ ! -d "$targetDir" ]
then
   mkdir -p $targetDir
fi
target=$targetDir/Qa4ecv.albedo.spectral.Merge.$deg.${year}${doy}.$proj.nc

if [ -f $noSnowProduct ] && [ -f $snowProduct ]
then
    echo "time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.mergealbedo.spectral -SnoSnowProduct=$noSnowProduct -SsnowProduct=$snowProduct -e -f NetCDF4-BEAM -t $target"
    time $beamRootDir/bin/gpt-d-l3.sh ga.albedo.mergealbedo.spectral -SnoSnowProduct=$noSnowProduct -SsnowProduct=$snowProduct -e -f NetCDF4-BEAM -t $target
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
