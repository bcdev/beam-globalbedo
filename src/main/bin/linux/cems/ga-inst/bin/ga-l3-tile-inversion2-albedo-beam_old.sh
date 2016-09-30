#!/bin/bash

### get input parameters
tile=$1
year=$2
start=$3
end=$4
gaRootDir=$5
priorRootDir=$6
beamRootDir=$7
albedoTargetDir=${8}  # remind the brackets if >= 10!!

### set GPT
gpt=$beamRootDir/bin/gpt-d-l2.sh

### set up required directories...
bbdrRootDir=$gaRootDir/BBDR
noSnowPriorDir=$priorRootDir/NoSnow
snowPriorDir=$priorRootDir/Snow

inversionNosnowTargetDir=$gaRootDir/Inversion/NoSnow/$year/$tile
inversionSnowTargetDir=$gaRootDir/Inversion/Snow/$year/$tile
inversionMergeTargetDir=$gaRootDir/Inversion/Merge/$year/$tile
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


if [ "$start" -le "0" ]; then
    start=1
fi
if [ "$end" -ge "366" ]; then
    end=365
fi

echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $start ..."
if [ -d "$snowPriorDir/$tile" ] 
then

    doy=$start
    echo "Compute NOSNOW BRDF for tile $tile, year $year, DoY $doy, ..."
    TARGET=${inversionNosnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.NoSnow.nc
    echo "time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PgaRootDir=$gaRootDir -PpriorRootDir=$noSnowPriorDir -e -f NetCDF4-GA-BRDF -t $TARGET"
    time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PgaRootDir=$gaRootDir -PpriorRootDir=$noSnowPriorDir -e -f NetCDF4-GA-BRDF -t $TARGET
    status=$?
    echo "Status: $status"

    if [ "$status" -eq 0 ]; then
        doy=$start
        echo "Compute SNOW BRDF for tile $tile, year $year, DoY $doy, ..."
        TARGET=${inversionSnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.Snow.nc
        echo "time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -f NetCDF4-GA-BRDF -t $TARGET"
        time $gpt ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -f NetCDF4-GA-BRDF -t $TARGET
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        echo "Compute MERGED BRDF for tile $tile, year $year, DoY $doy ..."
        TARGET=${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.nc
        echo "time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -f NetCDF4-GA-BRDF -t $TARGET"
        time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -f NetCDF4-GA-BRDF -t $TARGET
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        echo "Compute ALBEDO for tile $tile, year $year, DoY $doy ..."
        TARGET=$albedoTargetDir/GlobAlbedo.albedo.$year$doy.$tile.nc
        echo "time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -f NetCDF4-GA-ALBEDO -t $TARGET"
        time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -f NetCDF4-GA-ALBEDO -t $TARGET
        status=$?
        echo "Status: $status"
    fi

else
    echo "Directory '$priorRootDir/$tile' does not exist - no BRDF computed for tile $tile, year $year, DoY $doy."
fi

echo `date`
