#!/bin/bash

### get input parameters
sensorID=$1
tile=$2
year=$3
doy=$4
subStartX=$5
subStartY=$6
gaRootDir=$7
spectralSdrRootDir=$8
spectralInversionRootDir=$9
beamRootDir=${10}
spectralAlbedoTargetDir=${11}  # remind the brackets if argument index >= 10!!

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
TARGET=${spectralInversionNosnowTargetDir}/Qa4ecv.brdf.spectral.$year$doy.$tile.${subStartX}_${subStartY}.NoSnow.nc
echo "time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -PsubStartX=$subStartX -PsubStartY=$subStartY -Pdoy=$doy -PcomputeSnow=false -PsdrRootDir=$spectralSdrRootDir -e -f NetCDF4-BEAM -t $TARGET"
time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -PsubStartX=$subStartX -PsubStartY=$subStartY -Pdoy=$doy -PcomputeSnow=false -PsdrRootDir=$spectralSdrRootDir -e -f NetCDF4-BEAM -t $TARGET
status=$?
echo "Status: $status"

if [ "$status" -eq 0 ]; then
    echo "Compute SNOW BRDF for tile $tile, x=$subStartX, y=$subStartY, year $year, DoY $doy, ..."
    TARGET=${spectralInversionSnowTargetDir}/Qa4ecv.brdf.spectral.$year$doy.$tile.${subStartX}_${subStartY}.Snow.nc
    echo "time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -PsubStartX=$subStartX -PsubStartY=$subStartY -Pdoy=$doy -PcomputeSnow=true -PsdrRootDir=$spectralSdrRootDir -e -f NetCDF4-BEAM -t $TARGET"
    time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -PsubStartX=$subStartX -PsubStartY=$subStartY -Pdoy=$doy -PcomputeSnow=true -PsdrRootDir=$spectralSdrRootDir -e -f NetCDF4-BEAM -t $TARGET
    status=$?
    echo "Status: $status"
fi

# later:
if [ "$status" -eq 0 ]; then
    echo "Compute MERGED BRDF for tile $tile, year $year, DoY $doy ..."
    TARGET=${spectralInversionMergeTargetDir}/Qa4ecv.brdf.spectral.$year$doy.$tile.${subStartX}_${subStartY}.Merge.nc
    echo "time $gpt ga.l3.albedo.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$spectralInversionRootDir -PmergedProductOnly=true -PsubStartX=$subStartX -PsubStartY=$subStartY -e -f NetCDF4-BEAM -t $TARGET"
    time $gpt ga.l3.albedo.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$spectralInversionRootDir -PmergedProductOnly=true -PsubStartX=$subStartX -PsubStartY=$subStartY -e -f NetCDF4-BEAM -t $TARGET
    status=$?
    echo "Status: $status"
fi

# later:
if [ "$status" -eq 0 ]; then
    echo "Compute ALBEDO for tile $tile, year $year, DoY $doy ..."
    TARGET=$spectralAlbedoTargetDir/Qa4ecv.albedo.spectral.$sensorID.$year$doy.$tile.${subStartX}_${subStartY}.nc
    echo "time $gpt ga.l3.albedo.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$spectralInversionRootDir -PsubStartX=$subStartX -PsubStartY=$subStartY -e -f NetCDF4-BEAM -t $TARGET"
    time $gpt ga.l3.albedo.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$spectralInversionRootDir -PsubStartX=$subStartX -PsubStartY=$subStartY -e -f NetCDF4-BEAM -t $TARGET
    status=$?
    echo "Status: $status"
fi

echo `date`
