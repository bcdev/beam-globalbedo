#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
modisTileScaleFactor=$5
gaRootDir=$6
bbdrRootDir=$7
beamRootDir=$8

gpt=$beamRootDir/bin/gpt-d-l2.sh

avhrrMaskRootDir=$gaRootDir/MsslAvhrrMask

dailyAccNosnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
if [ ! -d "$dailyAccNosnowDir" ]
then 
   mkdir -p $dailyAccNosnowDir
fi

dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow
if [ ! -d "$dailyAccSnowDir" ]
then
   mkdir -p $dailyAccSnowDir
fi

### now start regular daily accumulation ###

for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    echo "DoY $doy..."

    TARGET=$dailyAccNosnowDir/SUCCESS_dailyacc_${year}_$doy.dim
    ACC=$dailyAccNosnowDir/matrices_${year}${doy}.bin
    if [ ! -f "$ACC" ] && [ "$doy" -lt "366" ];
    then
        echo "Create NOSNOW daily accumulators for tile $tile, year $year, DoY $doy..."

        # we have the possible sensors (set manually below):
        #     AVHRR + GEO (MVIRI, SEVIRI, GOES_E, GOES_W, GMS)

        # MVIRI, SEVIRI, GOES_E, GOES_W, GMS and AVHRR (the new AVHRR+GEO default, 20170315):
        echo "time $gpt  ga.l3.dailyacc -PavhrrMaskRootDir=$avhrrMaskRootDir -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        time $gpt  ga.l3.dailyacc -PavhrrMaskRootDir=$avhrrMaskRootDir -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # MVIRI, SEVIRI and AVHRR (the former AVHRR+GEO default):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # MVIRI and SEVIRI (test purposes):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # MVIRI and AVHRR (test purposes):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI","AVHRR" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt  ga.l3.dailyacc -Psensors="MVIRI","AVHRR" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # AVHRR only (test purposes):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="AVHRR" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt  ga.l3.dailyacc -Psensors="AVHRR" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # MVIRI only (test purposes):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e"
        #time $gpt  ga.l3.dailyacc -Psensors="MVIRI" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e

        status=$?
        echo "Status: $status"
        if [ "$status" -ne "0" ]; then
           break
        fi
    fi

    TARGET=$dailyAccSnowDir/SUCCESS_dailyacc_${year}_$doy.dim
    ACC=$dailyAccSnowDir/matrices_${year}${doy}.bin
    if [ ! -f "$ACC" ] && [ "$doy" -lt "366" ];
    then
        echo "Create SNOW daily accumulators for tile $tile, year $year, DoY $doy..."

        # we have the possible sensors (set manually below):
        #     AVHRR + GEO (MVIRI, SEVIRI, GOES_E, GOES_W, GMS)

        # MVIRI, SEVIRI, GOES_E, GOES_W, GMS and AVHRR (the new AVHRR+GEO default, 20170315):
        echo "time $gpt  ga.l3.dailyacc -PavhrrMaskRootDir=$avhrrMaskRootDir -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        time $gpt  ga.l3.dailyacc -PavhrrMaskRootDir=$avhrrMaskRootDir -Psensors="MVIRI","SEVIRI","AVHRR","GOES_E","GOES_W","GMS" -PmeteosatUseAllLongitudes=true -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        status=$?
        echo "Status: $status"
        if [ "$status" -ne "0" ]; then
           break
        fi
    fi
    
done

# create marker file for PMonitor that all daily accs for given tile/DoY were processed
touch $dailyAccNosnowDir/PROCESSED_ALL_$start
touch $dailyAccSnowDir/PROCESSED_ALL_$start

# count existing marker files and create final marker file if we are done for all 16 DoYs (test 20170120: step=24days) of given year:
numDailyAccsNoSnow=`ls -1 $dailyAccNosnowDir/PROCESSED_ALL_* |wc -l`
echo "numDailyAccsNoSnow: $numDailyAccsNoSnow"
#if [ $numDailyAccsNoSnow -eq 46 ]
if [ $numDailyAccsNoSnow -eq 16 ]
then
    echo "All NOSNOW daily accs for year $year, tile $tile done."
    touch $dailyAccNosnowDir/PROCESSED_ALL
fi

numDailyAccsSnow=`ls -1 $dailyAccSnowDir/PROCESSED_ALL_* |wc -l`
echo "numDailyAccsSnow: $numDailyAccsSnow"
#if [ $numDailyAccsSnow -eq 46 ]
if [ $numDailyAccsSnow -eq 16 ]
then
    echo "All SNOW daily accs for year $year, tile $tile done."
    touch $dailyAccSnowDir/PROCESSED_ALL
fi

echo `date`
