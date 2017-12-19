#!/bin/bash

bandIndex=$1
tile=$2
year=$3
start=$4
end=$5
gaRootDir=$6
spectralSdrRootDir=$7
spectralDailyAccRootDir=$8
beamDir=${9}

gpt=$beamDir/bin/gpt-d-l2.sh

dailyAccNosnowDir=$spectralDailyAccRootDir/band_${bandIndex}/$year/$tile/NoSnow
mkdir -p $dailyAccNosnowDir

dailyAccSnowDir=$spectralDailyAccRootDir/band_${bandIndex}/$year/$tile/Snow
mkdir -p $dailyAccSnowDir

### now start regular daily accumulation ###

for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    echo "DoY $doy..."

    echo "Create NOSNOW spectral daily accumulators for tile $tile, year $year, DoY $doy, bandIndex $bandIndex ..."

    #TARGET=$dailyAccNosnowDir/SUCCESS_dailyacc_${year}_$doy.dim
    echo "Create NOSNOW daily accumulators for tile $tile, year $year, DoY $doy..."

    echo "time $gpt  ga.l3.dailyacc.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PsingleBandIndex=$bandIndex -PsdrRootDir=$spectralSdrRootDir -PdailyAccRootDir=$spectralDailyAccRootDir -e"
    time $gpt  ga.l3.dailyacc.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PsingleBandIndex=$bandIndex -PsdrRootDir=$spectralSdrRootDir -PdailyAccRootDir=$spectralDailyAccRootDir -e

    status=$?
    echo "Status: $status"
    if [ "$status" -ne "0" ]; then
       break
    fi

    echo "Create SNOW daily accumulators for tile $tile, year $year, DoY $doy..."

    echo "time $gpt  ga.l3.dailyacc.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PsingleBandIndex=$bandIndex -PsdrRootDir=$spectralSdrRootDir -PdailyAccRootDir=$spectralDailyAccRootDir -e"
    time $gpt  ga.l3.dailyacc.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PsingleBandIndex=$bandIndex -PsdrRootDir=$spectralSdrRootDir -PdailyAccRootDir=$spectralDailyAccRootDir -e

    status=$?
    echo "Status: $status"
    if [ "$status" -ne "0" ]; then
        break
    fi

done

echo `date`
