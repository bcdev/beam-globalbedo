#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
modisTileScaleFactor=$5
gaRootDir=$6
bbdrRootDir=$7
dailyAccRootDir=$8
beamRootDir=$9

gpt=$beamRootDir/bin/gpt-d-l2.sh

dailyAccNosnowDir=$dailyAccRootDir/$year/$tile/NoSnow
mkdir -p $dailyAccNosnowDir

dailyAccSnowDir=$dailyAccRootDir/$year/$tile/Snow
mkdir -p $dailyAccSnowDir

### now start regular daily accumulation ###

for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    echo "DoY $doy..."

    #TARGET=$dailyAccNosnowDir/SUCCESS_dailyacc_${year}_$doy.dim
    echo "Create NOSNOW daily accumulators for tile $tile, year $year, DoY $doy..."

    # we have the possible sensors (set manually below):
    #     AVHRR + GEO (MVIRI, SEVIRI, GOES_E, GOES_W, GMS)

    # MVIRI, SEVIRI, GOES_E, GOES_W, GMS and AVHRR (the new AVHRR+GEO default, 20170315):
    #echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -PdailyAccDir=$dailyAccNosnowDir -e"
    #time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -PdailyAccDir=$dailyAccNosnowDir -e
    
    # test (REMOVE):
    #time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -PdailyAccDir=$dailyAccNosnowDir -e
    #time $gpt  ga.l3.dailyacc -Psensors="GOES_W" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -PdailyAccDir=$dailyAccNosnowDir -e

    status=$?
    echo "Status: $status"
    if [ "$status" -ne "0" ]; then
       break
    fi

    #TARGET=$dailyAccSnowDir/SUCCESS_dailyacc_${year}_$doy.dim
    echo "Create SNOW daily accumulators for tile $tile, year $year, DoY $doy..."

    # we have the possible sensors (set manually below):
    #     AVHRR + GEO (MVIRI, SEVIRI, GOES_E, GOES_W, GMS)

    # MVIRI, SEVIRI, GOES_E, GOES_W, GMS and AVHRR (the new AVHRR+GEO default, 20170315):
    #echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -PdailyAccDir=$dailyAccSnowDir -e -t $TARGET"
    #time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -PdailyAccDir=$dailyAccSnowDir -e -t $TARGET

    echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -PdailyAccDir=$dailyAccSnowDir -e"
    time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -PdailyAccDir=$dailyAccSnowDir -e

    status=$?
    echo "Status: $status"
    if [ "$status" -ne "0" ]; then
       break
    fi
    
done

echo `date`
