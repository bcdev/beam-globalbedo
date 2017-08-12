#!/bin/bash

### get input parameters
sensorID=$1
tile=$2
year=$3
start=$4
end=$5
gaRootDir=$6
bbdrRootDir=$7
inversionRootDir=$8
usePrior=$9
priorRootDir=${10}
beamRootDir=${11}
modisTileScaleFactor=${12}  # remind the brackets if >= 10!!

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


### now start inversion... ###

if [ -d "$priorRootDir/$tile" ] 
then
    for doy in $(seq -w $start $end); do   # -w takes care for leading zeros

        echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $doy ..."
        
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
    done
else
    echo "Directory '$priorRootDir/$tile' does not exist - no BRDF computed for tile $tile, year $year, DoY $doy."
    echo "Status: -1"
fi

echo `date`
