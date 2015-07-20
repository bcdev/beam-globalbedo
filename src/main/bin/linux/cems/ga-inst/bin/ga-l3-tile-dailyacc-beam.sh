#!/bin/bash

tile=$1
year=$2
startdoy=$3
enddoy=$4
snow=$5
gaRootDir=$6
priorRootDir=$7
beamRootDir=$8

bbdrRootDir=$gaRootDir/BBDR
dailyaccRootDir=$bbdrRootDir/DailyAcc

if [ $snow == "true" ]
then
    snowId=Snow
else
    snowId=NoSnow
fi

echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $startdoy ..."
if [ -d "$priorRootDir/$tile" ] 
then
    if [ ! -d "$dailyaccRootDir/$year/$tile/$snowId" ] 
    then
        mkdir -p $dailyaccRootDir/$year/$tile/$snowId
    fi
    for doy in $(seq -w $startdoy $enddoy); do   # -w takes care for leading zeros
        echo "DoY $doy..."

	echo "Create daily accumulators for tile $tile, year $year, DoY $doy, $snowId ..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=$snow -PbbdrRootDir=$bbdrRootDir -e -t $dailyaccRootDir/$year/$tile/$snowId/SUCCESS_dailyacc_${year}_$startdoy"
        time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=$snow -PbbdrRootDir=$bbdrRootDir -e -t $dailyaccRootDir/$year/$tile/$snowId/SUCCESS_dailyacc_${year}_$startdoy
        status=$?
        echo "Status: $status"
    done
else
    echo "Directory '$priorRootDir/$tile' does not exist - no DAILYACC computed for tile $tile, year $year, startDoY $startdoy."
fi

echo `date`
