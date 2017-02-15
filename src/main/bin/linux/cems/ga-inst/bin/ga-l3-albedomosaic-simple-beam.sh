#!/bin/bash

sensorID=$1
year=$2
doy=$3
snowMode=$4
deg=$5
proj=$6
tileSize=$7
gaRootDir=$8
beamRootDir=$9

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

albedoSrcSubdirName=Albedo/$sensorID

#targetDir=$gaRootDir/Mosaic/albedo/$snowMode/$year/$deg
targetDir=$gaRootDir/Mosaic/Albedo/$sensorID/$snowMode/$year/$deg
if [ ! -d "$targetDir" ]
then
   mkdir -p $targetDir
fi
#target=$targetDir/Qa4ecv.albedo.avhrrgeo.$snowMode.$deg.$year$doy.$proj.nc
if [ "$sensorID"="/" ]
then
   sensorID="avh_geo" # the default
fi
target=$targetDir/Qa4ecv.albedo.$sensorID.$snowMode.$deg.$year$doy.$proj.nc
#echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo.qa4ecv -c 3000M -PbandsToWrite="BHR_SW","Weighted_Number_of_Samples" -PinputFormat=NETCDF -Pscaling=$scaling -PinputProductTileSize=$tileSize -Preprojection=$proj -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-ALBEDO -t $target"
#time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo.qa4ecv -c 3000M -PbandsToWrite="BHR_SW","Weighted_Number_of_Samples" -PinputFormat=NETCDF -Pscaling=$scaling -PinputProductTileSize=$tileSize -Preprojection=$proj -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-ALBEDO -t $target

echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo.qa4ecv -c 3000M -PinputFormat=NETCDF -PalbedoSubdirName=$albedoSrcSubdirName -Pscaling=$scaling -PinputProductTileSize=$tileSize -Preprojection=$proj -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-ALBEDO -t $target"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo.qa4ecv -c 3000M -PinputFormat=NETCDF -PalbedoSubdirName=$albedoSrcSubdirName -Pscaling=$scaling -PinputProductTileSize=$tileSize -Preprojection=$proj -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -f NetCDF4-GA-ALBEDO -t $target

status=$?
echo "Status: $status"
if [ "$status" -ne 0 ]; then
   echo "exiting..."
   exit 1
fi

echo `date`
