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

dailyAccNosnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
if [ ! -d "$dailyAccNosnowDir" ]
then 
   mkdir -p $dailyAccNosnowDir
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
        #     AVHRR + GEO (MVIRI, SEVIRI)

        # MVIRI, SEVIRI and AVHRR (the GEO default):
        echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

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

done

# create marker file for PMonitor that all daily accs for given tile/DoY were processed
touch $dailyAccNosnowDir/PROCESSED_ALL_$start

# count existing marker files and create final marker file if we are done for all 46 DoYs of given year:
numDailyAccsNoSnow=`ls -1 $dailyAccNosnowDir/PROCESSED_ALL_* |wc -l`
echo "numDailyAccsNoSnow: $numDailyAccsNoSnow"
if [ $numDailyAccsNoSnow -eq 46 ]
then
    echo "All NOSNOW daily accs for year $year, tile $tile done."
    touch $dailyAccNosnowDir/PROCESSED_ALL
fi

echo `date`
