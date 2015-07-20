#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
snow=$5
gaRootDir=$6
priorRootDir=$7
snowPriorRootDir=$8  # explicitly needed in merge step
beamRootDir=$9

bbdrRootDir=$gaRootDir/BBDR
fullaccRootDir=$bbdrRootDir/FullAcc

if [ $snow == "true" ]
then
    snowId=Snow
else
    snowId=NoSnow
fi

wings=90  # tbd later

echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $start ..."
if [ -d "$priorRootDir/$tile" ] 
then
    if [ ! -d "$fullaccRootDir/$year/$tile/$snowId" ]
    then
        mkdir -p $fullaccRootDir/$year/$tile/$snowId
    fi

    echo "Create full accumulator for tile $tile, year $year, DoY $doy, $snowId ..."
    echo "time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -Pwings=$wings -PcomputeSnow=$snow -PgaRootDir=$gaRootDir -e -t $fullaccRootDir/$year/$tile/$snowId/SUCCESS_fullacc_${year}_$start"
    time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -Pwings=$wings -PcomputeSnow=$snow -PgaRootDir=$gaRootDir -e -t $fullaccRootDir/$year/$tile/$snowId/SUCCESS_fullacc_${year}_$start
    status=$?
    echo "Status: $status"
else
    echo "Directory '$priorRootDir/$tile' does not exist - no FULLACC computed for tile $tile, year $year, DoY $doy."
fi

echo `date`
