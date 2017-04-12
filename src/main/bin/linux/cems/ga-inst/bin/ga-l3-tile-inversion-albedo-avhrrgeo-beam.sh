#!/bin/bash

### get input parameters
sensorID=$1
tile=$2
year=$3
doy=$4
gaRootDir=$5
bbdrRootDir=$6
inversionRootDir=$7
usePrior=$8
priorRootDir=$9
beamRootDir=${10}
modisTileScaleFactor=${11}  # remind the brackets if >= 10!!

### set GPT
gpt=$beamRootDir/bin/gpt-d-l2.sh

inversionNosnowTargetDir=$inversionRootDir/NoSnow/$year/$tile
inversionSnowTargetDir=$inversionRootDir/Snow/$year/$tile
#inversionMergeTargetDir=$inversionRootDir/Merge/$year/$tile

if [ ! -d "$inversionNosnowTargetDir" ]
then
   mkdir -p $inversionNosnowTargetDir
fi
if [ ! -d "$inversionSnowTargetDir" ]
then
   mkdir -p $inversionSnowTargetDir
fi

#if [ ! -d "$inversionMergeTargetDir" ]
#then
#   mkdir -p $inversionMergeTargetDir
#fi

albedoNosnowTargetDir=$gaRootDir/Albedo/NoSnow/$sensorID/$year/$tile
albedoSnowTargetDir=$gaRootDir/Albedo/Snow/$sensorID/$year/$tile

if [ ! -d "$albedoNosnowTargetDir" ]
then
   mkdir -p $albedoNosnowTargetDir
fi
if [ ! -d "$albedoSnowTargetDir" ]
then
   mkdir -p $albedoSnowTargetDir
fi


echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $doy ..."
if [ -d "$priorRootDir/$tile" ] 
then
    echo "Compute NOSNOW BRDF for tile $tile, year $year, DoY $doy, ..."
    TARGET=${inversionNosnowTargetDir}/Qa4ecv.avhrrgeo.brdf.$year$doy.$tile.NoSnow.nc
    echo "time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET"
    time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET
    status=$?
    echo "Status: $status"

    if [ "$status" -eq 0 ]; then
        echo "Compute NOSNOW ALBEDO for tile $tile, year $year, DoY $doy ..."
        TARGET=$albedoNosnowTargetDir/Qa4ecv.albedo.${sensorID}.$year$doy.$tile.NoSnow.nc
        echo "time $gpt ga.l3.albedo -PprocessingMode=AVHRRGEO -PcomputeSnow=false -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$inversionRootDir -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-ALBEDO -t $TARGET"
        time $gpt ga.l3.albedo -PprocessingMode=AVHRRGEO -PcomputeSnow=false -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$inversionRootDir -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-ALBEDO -t $TARGET
        status=$?
        echo "Status: $status"
    fi

    echo "Compute SNOW BRDF for tile $tile, year $year, DoY $doy, ..."
    TARGET=${inversionSnowTargetDir}/Qa4ecv.avhrrgeo.brdf.$year$doy.$tile.Snow.nc
    echo "time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET"
    time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET
    status=$?
    echo "Status: $status"

    if [ "$status" -eq 0 ]; then
        echo "Compute SNOW ALBEDO for tile $tile, year $year, DoY $doy ..."
        TARGET=$albedoSnowTargetDir/Qa4ecv.albedo.${sensorID}.$year$doy.$tile.Snow.nc
        echo "time $gpt ga.l3.albedo -PprocessingMode=AVHRRGEO -PcomputeSnow=true -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$inversionRootDir -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-ALBEDO -t $TARGET"
        time $gpt ga.l3.albedo -PprocessingMode=AVHRRGEO -PcomputeSnow=true -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$inversionRootDir -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-ALBEDO -t $TARGET
        status=$?
        echo "Status: $status"
    fi

    # create marker file that albedo for given tile/DoY was processed
    touch $albedoNosnowTargetDir/PROCESSED_ALL_$doy

    # count existing marker files and create final marker file if we are done for all DoYs of given year (we assume daily processing):
    numAlbedoFiles=`ls -1 $albedoNosnowTargetDir/PROCESSED_ALL_* |wc -l`
    echo "numAlbedoFiles nosnow: $numAlbedoFiles"
    if [ $numAlbedoFiles -eq 365 ]
    then
	echo "All albedo nosnow products for year $year, tile $tile done."
	touch $albedoNosnowTargetDir/PROCESSED_ALL
    fi

    touch $albedoNosnowTargetDir/PROCESSED_ALL_$doy

    # count existing marker files and create final marker file if we are done for all DoYs of given year (we assume daily processing):
    numAlbedoFiles=`ls -1 $albedoSnowTargetDir/PROCESSED_ALL_* |wc -l`
    echo "numAlbedoFiles snow: $numAlbedoFiles"
    if [ $numAlbedoFiles -eq 365 ]
    then
        echo "All albedo snow products for year $year, tile $tile done."
        touch $albedoSnowTargetDir/PROCESSED_ALL
    fi


else
    echo "Directory '$priorRootDir/$tile' does not exist - no BRDF computed for tile $tile, year $year, DoY $doy."
    echo "Status: -1"
fi

echo `date`
