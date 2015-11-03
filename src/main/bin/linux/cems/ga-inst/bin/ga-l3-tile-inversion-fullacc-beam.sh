#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
gaRootDir=$5
beamRootDir=$6

gpt=$beamRootDir/bin/gpt-d-l2.sh

bbdrRootDir=$gaRootDir/BBDR

fullAccNosnowDir=$gaRootDir/BBDR/FullAcc/$year/$tile/NoSnow
fullAccSnowDir=$gaRootDir/BBDR/FullAcc/$year/$tile/Snow
if [ ! -d "$fullAccNosnowDir" ]
then
   mkdir -p $fullAccNosnowDir
fi
if [ ! -d "$fullAccSnowDir" ]
then
   mkdir -p $fullAccSnowDir
fi

if [ "$start" -le "0" ]; then
    start=1
fi
if [ "$end" -ge "361" ]; then
    end=361
fi

echo "FULL accumulation for tile: '$tile' , year $year, DoY $start ..."

echo "Create NOSNOW full accumulator for tile $tile, year $year, DoY $doy ..."
TARGET=$fullAccNosnowDir/SUCCESS_fullacc_${year}_$start.dim
echo "time $gpt  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -PcomputeSnow=false -PgaRootDir=$gaRootDir -e -t $TARGET"
time $gpt ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -PcomputeSnow=false -PgaRootDir=$gaRootDir -e -t $TARGET
status=$?
echo "Status: $status"

echo "Create SNOW full accumulator for tile $tile, year $year, DoY $doy ..."
TARGET=$fullAccSnowDir/SUCCESS_fullacc_${year}_$start.dim
echo "time $gpt  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -PcomputeSnow=true -PgaRootDir=$gaRootDir -e -t $TARGET"
time $gpt ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -PcomputeSnow=true -PgaRootDir=$gaRootDir -e -t $TARGET
status=$?
echo "Status: $status"

touch $fullAccNosnowDir/PROCESSED_ALL_$start
touch $fullAccSnowDir/PROCESSED_ALL_$start

echo `date`
