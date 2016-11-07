#!/bin/bash

# cleanup of daily accumulator binary files which use a lot of disk space...

tile=$1
year=$2
gaRootDir=$3

dailyAccNosnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow

#prevYear=`printf '%04d\n' "$((10#$year - 1))"`
#dailyAccPrevNosnowDir=$gaRootDir/BBDR/DailyAcc/$prevYear/$tile/NoSnow
#dailyAccPrevSnowDir=$gaRootDir/BBDR/DailyAcc/$prevYear/$tile/Snow
#nextYear=`printf '%04d\n' "$((10#$year + 1))"`
#dailyAccNextNosnowDir=$gaRootDir/BBDR/DailyAcc/$nextYear/$tile/NoSnow
#dailyAccNextSnowDir=$gaRootDir/BBDR/DailyAcc/$nextYear/$tile/Snow


# make sure albedos are done:
waitCount=0
while [  "$waitCount" -lt "120" ]; do
    # e.g. $GA_INST/log/ga-l3-tile-inversion-albedo-avhrrgeo-h21v08-2000-363.err:
    numAlbedoLogs=`ls -1 $GA_INST/log/ga-l3-tile-inversion-albedo-avhrrgeo-${tile}-${year}*.err |wc -l`
    echo "numAlbedoLogs: $numAlbedoLogs of 365"
    thedate=`date`
    echo "Waiting for $waitCount minutes for completion of albedos... $thedate"
    if [ "$numAlbedoLogs" -ge "360" ]
    then
        echo "Albedo computation completed - ready for inversion cleanup."
        break
    fi
    let waitCount=waitCount+10
    sleep 600
done
if [ "$waitCount" -ge "120" ]; then
    #stop this after 2 hours
    echo "WARNING: Albedo computation not complete after 2 hours for tile $tile, year $year... - will exit. Do cleanup manually!"
    exit 0
fi


# cleanup
echo "cleaning up daily accumulators for tile $tile, year $year..."
rm -Rf $dailyAccSnowDir
rm -Rf $dailyAccNosnowDir
#rm -Rf $dailyAccPrevSnowDir
#rm -Rf $dailyAccPrevNosnowDir
#rm -Rf $dailyAccNextSnowDir
#rm -Rf $dailyAccNextNosnowDir

echo "cleaning up albedo processing marker files for tile $tile, year $year..."
albedoTileDir=$gaRootDir/Albedo/$year/$tile
rm -Rf $albedoTileDir/PROCESSED_*

echo "done."
echo `date`
