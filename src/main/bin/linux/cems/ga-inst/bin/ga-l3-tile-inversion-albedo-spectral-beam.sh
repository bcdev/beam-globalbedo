#!/bin/bash

### get input parameters
tile=$1
year=$2
doy=$3
subStartX=$4
subStartY=$5
gaRootDir=$6
spectralSdrRootDir=$7
spectralInversionRootDir=$8
beamRootDir=$9
spectralAlbedoTargetDir=${10}  # remind the brackets if argument index >= 10!!

### set GPT
gpt=$beamRootDir/bin/gpt-d-l2.sh

spectralInversionNosnowTargetDir=$spectralInversionRootDir/NoSnow/$year/$tile/SUB_${subStartX}_${subStartY}
spectralInversionSnowTargetDir=$spectralInversionRootDir/Snow/$year/$tile/SUB_${subStartX}_${subStartY}
spectralInversionMergeTargetDir=$spectralInversionRootDir/Merge/$year/$tile/SUB_${subStartX}_${subStartY}
if [ ! -d "$spectralInversionNosnowTargetDir" ]
then
   mkdir -p $spectralInversionNosnowTargetDir
fi
if [ ! -d "$spectralInversionSnowTargetDir" ]
then
   mkdir -p $spectralInversionSnowTargetDir
fi
if [ ! -d "$spectralInversionMergeTargetDir" ]
then
   mkdir -p $spectralInversionMergeTargetDir
fi

if [ ! -d "$spectralAlbedoTargetDir" ]
then
   mkdir -p $spectralAlbedoTargetDir
fi

echo "BRDF computation for tile: '$tile' , year $year, DoY $doy ..."

echo "Compute NOSNOW BRDF for tile $tile, x=$subStartX, y=$subStartY, year $year, DoY $doy, ..."
TARGET=${spectralInversionNosnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.${subStartX}_${subStartY}.NoSnow.nc
echo "time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -PsubStartX=$subStartX -PsubStartY=$subStartY -Pdoy=$doy -PcomputeSnow=false -PsdrRootDir=$spectralSdrRootDir -e -f NetCDF4-BEAM -t $TARGET"
time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -PsubStartX=$subStartX -PsubStartY=$subStartY -Pdoy=$doy -PcomputeSnow=false -PsdrRootDir=$spectralSdrRootDir -e -f NetCDF4-BEAM -t $TARGET
status=$?
echo "Status: $status"

if [ "$status" -eq 0 ]; then
    echo "Compute SNOW BRDF for tile $tile, x=$subStartX, y=$subStartY, year $year, DoY $doy, ..."
    TARGET=${spectralInversionSnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.${subStartX}_${subStartY}.Snow.nc
    echo "time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -PsubStartX=$subStartX -PsubStartY=$subStartY -Pdoy=$doy -PcomputeSnow=true -PsdrRootDir=$spectralSdrRootDir -e -f NetCDF4-BEAM -t $TARGET"
    time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -PsubStartX=$subStartX -PsubStartY=$subStartY -Pdoy=$doy -PcomputeSnow=true -PsdrRootDir=$spectralSdrRootDir -e -f NetCDF4-BEAM -t $TARGET
    status=$?
    echo "Status: $status"
fi

# later:
#if [ "$status" -eq 0 ]; then
#    echo "Compute MERGED BRDF for tile $tile, year $year, DoY $doy ..."
#    TARGET=${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.nc
#    echo "time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PinversionRootDir=$inversionRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET"
#    time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PinversionRootDir=$inversionRootDir -PusePrior=$usePrior -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-BRDF -t $TARGET
#    status=$?
#    echo "Status: $status"
#fi

# later:
# if [ "$status" -eq 0 ]; then
#    echo "Compute ALBEDO for tile $tile, year $year, DoY $doy ..."
#    TARGET=$albedoTargetDir/GlobAlbedo.albedo.$year$doy.$tile.nc
#    echo "time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$inversionRootDir -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-ALBEDO -t $TARGET"
#    time $gpt ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$inversionRootDir -PpriorRootDir=$priorRootDir -PmodisTileScaleFactor=$modisTileScaleFactor -e -f NetCDF4-GA-ALBEDO -t $TARGET
#    status=$?
#    echo "Status: $status"
#fi

echo `date`
