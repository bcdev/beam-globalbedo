#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
gaRootDir=$5
beamRootDir=$6

gpt=$beamRootDir/bin/gpt-d-l2.sh

bbdrRootDir=$gaRootDir/BBDR

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
        echo "time $gpt  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        time $gpt ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $TARGET
        status=$?
        echo "Status: $status"
        if [ "$status" -ne "0" ]; then
           break
        fi
        rm -Rf $dailyAccNosnowDir/SUCCESS_dailyacc_${year}_$doy*.d*
    fi

    TARGET=$dailyAccSnowDir/SUCCESS_dailyacc_${year}_$doy.dim
    ACC=$dailyAccSnowDir/matrices_${year}${doy}.bin
    if [ ! -f "$ACC" ] && [ "$doy" -lt "366" ];
    then
        echo "Create SNOW daily accumulators for tile $tile, year $year, DoY $doy..."
        echo "time $gpt  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET"
        time $gpt ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $TARGET
        status=$?
        echo "Status: $status"
        if [ "$status" -ne "0" ]; then
            break
        fi
        rm -Rf $dailyAccSnowDir/SUCCESS_dailyacc_${year}_$doy*.d*
    fi

done

touch $dailyAccNosnowDir/PROCESSED_ALL_$start
touch $dailyAccSnowDir/PROCESSED_ALL_$start

echo `date`
