#!/bin/bash

FUNC=process_DailyAccumulator

echo ""
echo ${FUNC}: Checking command line arguments

init_time=`date`

TILE=$1
year=$2
start=$3
end=$4

HOST=`hostname -s`
SRCDIR="/data/GlobAlbedo/src"

echo $TILE $year processing in $HOST $SENSOR
i=$start
while [ $i -le $end ]
do
	if (($i < "10")); then
            Day=00$i
        elif (($i < "100")); then
            Day=0$i
        else
            Day=$i
        fi
        echo "Day:  $i , $Day"

	SNOW=1
	echo "$SRCDIR/AlbedoInversionDailyAccumulator.py $TILE $year $Day $SNOW"
#       $SRCDIR/AlbedoInversionDailyAccumulator.py $TILE $year $Day $SNOW >> ./$TILE.$year.$Day.Snow.log &
        $SRCDIR/AlbedoInversionDailyAccumulator.py $TILE $year $Day $SNOW

	SNOW=0
	echo "$SRCDIR/AlbedoInversionDailyAccumulator.py $TILE $year $Day $SNOW"
#	$SRCDIR/AlbedoInversionDailyAccumulator.py $TILE $year $Day $SNOW >> ./$TILE.$year.$Day.NoSnow.log
	$SRCDIR/AlbedoInversionDailyAccumulator.py $TILE $year $Day $SNOW

	let i=$i+1
done 

wait

# Remove BBDRs
#echo "Removing tmp BBDRs..."
#/bin/rm -rf /data/GlobAlbedo/BBDR/MERIS/$year/$TILE/*
#/bin/rm -rf /data/GlobAlbedo/BBDR/VGT/$year/$TILE/*

echo "Process started at $init_time"
echo "ALL jobs finished at `date`"
