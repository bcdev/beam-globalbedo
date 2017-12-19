#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
gaRootDir=$5
spectralInversionRootDir=${6}
beamDir=${7}

### set GPT
gpt=$beamDir/bin/gpt-d-l2.sh


spectralInversionNosnowBandmergeTargetDir=$spectralInversionRootDir/NoSnow/$year/$tile
spectralInversionSnowBandmergeTargetDir=$spectralInversionRootDir/Snow/$year/$tile
mkdir -p $spectralInversionBandmergeNosnowTargetDir
mkdir -p $spectralInversionBandmergeSnowTargetDir

for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    echo "BRDF band merge for tile: '$tile' , year $year, DoY $doy ..."

    echo "Merge bands for NOSNOW SPECTRAL BRDF for tile $tile, year $year, DoY $doy ..."
    TARGET=${spectralInversionNosnowBandmergeTargetDir}/Qa4ecv.brdf.spectral.$year$doy.$tile.NoSnow.nc
    echo "time $gpt ga.albedo.brdf.spectral.bandmerge -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PspectralInversionRootDir=$spectralInversionRootDir -e -f NetCDF4-BEAM -t $TARGET"
    time $gpt ga.albedo.brdf.spectral.bandmerge -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PspectralInversionRootDir=$spectralInversionRootDir -e -f NetCDF4-BEAM -t $TARGET
    status=$?

    echo "Status: $status"

    if [ "$status" -eq 0 ]; then
        echo "Merge bands for SNOW SPECTRAL BRDF for tile $tile, year $year, DoY $doy ..."
        TARGET=${spectralInversionSnowBandmergeTargetDir}/Qa4ecv.brdf.spectral.$year$doy.$tile.Snow.nc
        echo "time $gpt ga.albedo.brdf.spectral.bandmerge -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PspectralInversionRootDir=$spectralInversionRootDir -e -f NetCDF4-BEAM -t $TARGET"
        time $gpt ga.albedo.brdf.spectral.bandmerge -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PspectralInversionRootDir=$spectralInversionRootDir -e -f NetCDF4-BEAM -t $TARGET
        status=$?
        echo "Status: $status"
    fi
done

echo `date`
