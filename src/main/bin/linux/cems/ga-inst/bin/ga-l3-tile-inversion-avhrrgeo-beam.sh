#!/bin/bash

echo "entering 'ga-l3-tile-inversion-avhrrgeo-beam.sh' ..."

### get input parameters
sensorID=$1
tile=$2
year=$3
start=$4
end=$5
gaRootDir=$6
dailyAccRootDir=$7
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

mkdir -p $inversionNosnowTargetDir
mkdir -p $inversionSnowTargetDir

albedoNosnowTargetDir=$gaRootDir/Albedo/NoSnow/$sensorID/$year/$tile
albedoSnowTargetDir=$gaRootDir/Albedo/Snow/$sensorID/$year/$tile

mkdir -p $albedoNosnowTargetDir
mkdir -p $albedoSnowTargetDir

#end=$(( $end > 366 ? 366 : end))

### now start inversion... ###

#echo "sensorID: $sensorID"
#echo "tile: $tile"
#echo "year: $year"
#echo "start: $start"
#echo "end: $end"
#echo "gaRootDir: $gaRootDir"
#echo "dailyAccRootDir: dailyAccRootDir"
#echo "inversionRootDir: $inversionRootDir"
#echo "usePrior: $usePrior"
#echo "priorRootDir: $priorRootDir"
echo "beamRootDir: $beamRootDir"
echo "modisTileScaleFactor: $modisTileScaleFactor"

if [ -d "$priorRootDir/$tile" ] 
then
    for doy in $(seq -w $start $end); do   # -w takes care for leading zeros

        echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $doy ..."
        
        echo "Compute NOSNOW BRDF for tile $tile, year $year, DoY $doy, ..."
        TARGET=${inversionNosnowTargetDir}/Qa4ecv.avhrrgeo.brdf.$year$doy.$tile.NoSnow.nc
        echo "time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PdailyAccRootDir=$dailyAccRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET"
        time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PdailyAccRootDir=$dailyAccRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET
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
        echo "time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PdailyAccRootDir=$dailyAccRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET"
        time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PdailyAccRootDir=$dailyAccRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET
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
