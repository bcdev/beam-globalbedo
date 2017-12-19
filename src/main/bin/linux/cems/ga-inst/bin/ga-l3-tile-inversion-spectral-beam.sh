#!/bin/bash

sensorID=$1
bandIndex=$2
tile=$3
year=$4
start=$5
end=$6
gaRootDir=$7
spectralInversionRootDir=${8}
spectralDailyAccRootDir=${9}
beamDir=${10}

### set GPT
gpt=$beamDir/bin/gpt-d-l2.sh

spectralInversionNosnowTargetDir=$spectralInversionRootDir/NoSnow/$year/$tile/band_${bandIndex}
spectralInversionSnowTargetDir=$spectralInversionRootDir/Snow/$year/$tile/band_${bandIndex}
#spectralInversionMergeTargetDir=$spectralInversionRootDir/Merge/$year/$tile/band_${bandIndex}
spectralAlbedoTargetDir=$spectralAlbedoRootDir/$year/$tile/band_${bandIndex}
mkdir -p $spectralInversionNosnowTargetDir
mkdir -p $spectralInversionSnowTargetDir
#mkdir -p $spectralInversionMergeTargetDir
mkdir -p $spectralAlbedoTargetDir

for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    echo "BRDF computation for tile: '$tile' , year $year, DoY $doy, bandIndex $bandIndex ..."

    echo "Compute NOSNOW SPECTRAL BRDF for tile $tile, year $year, DoY $doy, bandIndex $bandIndex ..."
    TARGET=${spectralInversionNosnowTargetDir}/Qa4ecv.brdf.spectral.${bandIndex}.$year$doy.$tile.NoSnow.nc
    echo "time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PsingleBandIndex=$bandIndex -PdailyAccRootDir=$spectralDailyAccRootDir -e -f NetCDF4-BEAM -t $TARGET"
    time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PsingleBandIndex=$bandIndex -PdailyAccRootDir=$spectralDailyAccRootDir -e -f NetCDF4-BEAM -t $TARGET
    status=$?

    echo "Status: $status"

    if [ "$status" -eq 0 ]; then
        echo "Compute SNOW SPECTRAL BRDF for tile $tile, year $year, DoY $doy, bandIndex $bandIndex ..."
        TARGET=${spectralInversionSnowTargetDir}/Qa4ecv.brdf.spectral.${bandIndex}.$year$doy.$tile.Snow.nc
        echo "time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PsingleBandIndex=$bandIndex -PdailyAccRootDir=$spectralDailyAccRootDir -e -f NetCDF4-BEAM -t $TARGET"
        time $gpt ga.l3.inversion.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PsingleBandIndex=$bandIndex -PdailyAccRootDir=$spectralDailyAccRootDir -e -f NetCDF4-BEAM -t $TARGET
        status=$?
        echo "Status: $status"
    fi
done

#if [ "$status" -eq 0 ]; then
#    echo "Compute ALBEDO for tile $tile, year $year, DoY $doy ..."
#    TARGET=$spectralAlbedoTargetDir/Qa4ecv.albedo.spectral.${bandIndex}.$year$doy.$tile.nc
#    echo "time $gpt ga.l3.albedo.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$spectralInversionRootDir -e -f NetCDF4-BEAM -t $TARGET"
#    time $gpt ga.l3.albedo.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PinversionRootDir=$spectralInversionRootDir -e -f NetCDF4-BEAM -t $TARGET
#    status=$?
#    echo "Status: $status"
#fi

echo `date`
