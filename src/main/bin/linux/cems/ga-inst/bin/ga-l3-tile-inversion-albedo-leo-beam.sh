#!/bin/bash

### get input parameters
tile=$1
year=$2
doy=$3
gaRootDir=$4
bbdrRootDir=$5
inversionRootDir=$6
usePrior=$7
priorRootDir=$8
beamRootDir=$9
modisTileScaleFactor=${10}
albedoTargetDir=${11}  # remind the brackets if >= 10!!

### set GPT
gpt=$beamRootDir/bin/gpt-d-l2.sh

inversionNosnowTargetDir=$inversionRootDir/NoSnow/$year/$tile
inversionSnowTargetDir=$inversionRootDir/Snow/$year/$tile
inversionMergeTargetDir=$inversionRootDir/Merge/$year/$tile
if [ ! -d "$inversionNosnowTargetDir" ]
then
   mkdir -p $inversionNosnowTargetDir
fi
if [ ! -d "$inversionSnowTargetDir" ]
then
   mkdir -p $inversionSnowTargetDir
fi
if [ ! -d "$inversionMergeTargetDir" ]
then
   mkdir -p $inversionMergeTargetDir
fi

if [ ! -d "$albedoTargetDir" ]
then
   mkdir -p $albedoTargetDir
fi

echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $doy ..."
if [ -d "$priorRootDir/$tile" ] 
then
    echo "Compute NOSNOW BRDF for tile $tile, year $year, DoY $doy, ..."
    TARGET=${inversionNosnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.NoSnow.nc
    echo "time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET"
    time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET
    status=$?
    echo "Status: $status"

    if [ "$status" -eq 0 ]; then
        echo "Compute SNOW BRDF for tile $tile, year $year, DoY $doy, ..."
        TARGET=${inversionSnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.Snow.nc
        echo "time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET"
        time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        echo "Compute MERGED BRDF for tile $tile, year $year, DoY $doy ..."
        TARGET=${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.nc
        echo "time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PinversionRootDir=$inversionRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET"
        time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PinversionRootDir=$inversionRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        echo "Compute ALBEDO for tile $tile, year $year, DoY $doy ..."
        TARGET=$albedoTargetDir/GlobAlbedo.albedo.$year$doy.$tile.nc
        echo "time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$inversionRootDir -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-ALBEDO -t $TARGET"
        time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$inversionRootDir -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-ALBEDO -t $TARGET
        status=$?
        echo "Status: $status"
    fi

    # create marker file that albedo for given tile/DoY was processed
    touch $albedoTargetDir/PROCESSED_ALL_$doy

    # count existing marker files and create final marker file if we are done for all DoYs of given year (we assume daily processing):
    numAlbedoFiles=`ls -1 $albedoTargetDir/PROCESSED_ALL_* |wc -l`
    echo "numAlbedoFiles: $numAlbedoFiles"
    if [ $numAlbedoFiles -eq 365 ]
    then
        echo "All albedo products for year $year, tile $tile done."
        touch $albedoTargetDir/PROCESSED_ALL
    fi

else
    echo "Directory '$priorRootDir/$tile' does not exist - no BRDF computed for tile $tile, year $year, DoY $doy."
    echo "Status: -1"
fi

echo `date`
