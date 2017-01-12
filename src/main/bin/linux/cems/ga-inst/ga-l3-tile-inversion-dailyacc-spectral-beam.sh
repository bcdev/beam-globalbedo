#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
subStartX=$5
subStartY=$6
gaRootDir=$7
spectralSdrRootDir=$8
beamRootDir=$9

gpt=$beamRootDir/bin/gpt-d-l2.sh

dailyAccNosnowDir=$gaRootDir/SDR_spectral/DailyAcc/$year/$tile/NoSnow/SUB_${subStartX}_${subStartY}
dailyAccSnowDir=$gaRootDir/SDR_spectral/DailyAcc/$year/$tile/Snow/SUB_${subStartX}_${subStartY}
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

        echo "time $gpt  ga.l3.dailyacc.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PsubStartX=${subStartX} -PsubStartY=${subStartY} -PcomputeSnow=false -PsdrRootDir=$spectralSdrRootDir -e -t $TARGET"
        time $gpt  ga.l3.dailyacc.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PsubStartX=${subStartX} -PsubStartY=${subStartY} -PcomputeSnow=false -PsdrRootDir=$spectralSdrRootDir -e -t $TARGET

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

        echo "time $gpt  ga.l3.dailyacc.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PsubStartX=${subStartX} -PsubStartY=${subStartY} -PcomputeSnow=true -PsdrRootDir=$spectralSdrRootDir -e -t $TARGET"
        time $gpt  ga.l3.dailyacc.spectral -Ptile=$tile -Pyear=$year -Pdoy=$doy -PsubStartX=${subStartX} -PsubStartY=${subStartY} -PcomputeSnow=true -PsdrRootDir=$spectralSdrRootDir -e -t $TARGET

        status=$?
        echo "Status: $status"
        if [ "$status" -ne "0" ]; then
            break
        fi
    fi

done

# create marker files that all daily accs for given tile/DoY were processed
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
