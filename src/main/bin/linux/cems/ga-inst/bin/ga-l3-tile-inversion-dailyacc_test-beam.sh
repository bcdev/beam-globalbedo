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
dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow
if [ ! -d "$dailyAccNosnowDir" ]
then 
   mkdir -p $dailyAccNosnowDir
fi
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
        #     LEO: MERIS, VGT, AATSR, PROBAV
        #     GEO: MVIRI, SEVIRI, AVHRR

        # MERIS, VGT (the GlobAlbedo default):
        echo "time $gpt  ga.l3.dailyacc -Psensors="MERIS","VGT" -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        time $gpt ga.l3.dailyacc -Ptile=$tile -Psensors="MERIS","VGT" -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # VGT only (test purposes):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="VGT" -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt ga.l3.dailyacc -Ptile=$tile -Psensors="VGT" -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # MERIS, VGT and AATSR (test purposes):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="MERIS","VGT","AATSR" -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt ga.l3.dailyacc -Ptile=$tile -Psensors="MERIS","VGT","AATSR" -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # PROBAV (as new LEO):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="PROBAV" -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt ga.l3.dailyacc -Ptile=$tile -Psensors="PROBAV" -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # MVIRI, SEVIRI and AVHRR (the GEO default):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt  ga.l3.dailyacc -Psensors="MVIRI","SEVIRI","AVHRR" -PmodisTileScaleFactor=$modisTileScaleFactor -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

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
#        rm -Rf $dailyAccNosnowDir/SUCCESS_dailyacc_${year}_$doy*.d*  # test: leave files!!
    fi

    TARGET=$dailyAccSnowDir/SUCCESS_dailyacc_${year}_$doy.dim
    ACC=$dailyAccSnowDir/matrices_${year}${doy}.bin
    if [ ! -f "$ACC" ] && [ "$doy" -lt "366" ];
    then
        echo "Create SNOW daily accumulators for tile $tile, year $year, DoY $doy..."

        # we have the possible sensors (set manually below):
        #     LEO: MERIS, VGT, AATSR, PROBAV
        # (for GEO we compute noSnow mode only, as we have no distinct snow detection)
        echo " (for GEO we compute noSnow mode only, as we have no distinct snow detection)"

        # MERIS, VGT (the GlobAlbedo default):
        echo "time $gpt  ga.l3.dailyacc -Psensors="MERIS","VGT" -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        time $gpt ga.l3.dailyacc -Ptile=$tile -Psensors="MERIS","VGT" -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # VGT only (test purposes):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="VGT" -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt ga.l3.dailyacc -Ptile=$tile -Psensors="VGT" -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # MERIS, VGT and AATSR (test purposes):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="MERIS","VGT","AATSR" -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt ga.l3.dailyacc -Ptile=$tile -Psensors="MERIS","VGT","AATSR" -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        # PROBAV (as new LEO):
        #echo "time $gpt  ga.l3.dailyacc -Psensors="PROBAV" -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        #time $gpt ga.l3.dailyacc -Ptile=$tile -Psensors="PROBAV" -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET

        #status=$?
        #echo "Status: $status"
        #if [ "$status" -ne "0" ]; then
        #    break
        #fi
#        rm -Rf $dailyAccSnowDir/SUCCESS_dailyacc_${year}_$doy*.d*   # test: leave files!!
    fi

done

# create marker file for PMonitor that all daily accs for given tile/DoY were processed
touch $dailyAccNosnowDir/PROCESSED_ALL_$start
touch $dailyAccSnowDir/PROCESSED_ALL_$start

# count existing marker files and create final marker file if we are done for all 46 DoYs of given year:
numDailyAccsNoSnow=`ls -1 $dailyAccNosnowDir/PROCESSED_ALL_* |wc -l`
echo "numDailyAccsNoSnow: $numDailyAccsNoSnow"
if [ $numDailyAccsNoSnow -eq 46 ]
then
    echo "All NOSNOW daily accs for year $year, tile $tile done."
    touch $dailyAccNosnowDir/PROCESSED_ALL
fi
numDailyAccsSnow=`ls -1 $dailyAccSnowDir/PROCESSED_ALL_* |wc -l`
echo "numDailyAccsSnow: $numDailyAccsSnow"
if [ $numDailyAccsSnow -eq 46 ]
then
    echo "All SNOW daily accs for year $year, tile $tile done."
    touch $dailyAccSnowDir/PROCESSED_ALL
fi

echo `date`
