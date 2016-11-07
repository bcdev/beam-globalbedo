#!/bin/bash

# cleanup of daily accumulator binary files which use a lot of disk space...

tile=$1
year=$2
gaRootDir=$3

dailyAccNosnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow

albedoTileDir=$gaRootDir/Albedo/$year/$tile

# make sure albedos are done:
waitCount=0
while [  "$waitCount" -lt "120" ]; do
    # e.g. $GA_INST/log/ga-l3-tile-inversion-albedo-avhrrgeo-h21v08-2000-363.err:
    numAlbedoFiles=`ls -1 $albedoTileDir/PROCESSED_* |wc -l`
    echo "numAlbedoFiles: $numAlbedoFiles of 365"
    thedate=`date`
    echo "Waiting for $waitCount minutes for completion of albedos... $thedate"
    if [ "$numAlbedoFiles" -ge "360" ]
    then
        echo "Albedo computation completed - ready for $tile/$year cleanup."
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

echo "cleaning up for tile $tile, year $year..."
rm -Rf $albedoTileDir/PROCESSED_*
# remove logs and tasks, e.g. $GA_INST/log/ga-l3-tile-inversion-albedo-avhrrgeo-h21v08-2000-363.err:
#rm -Rf $GA_INST/log/*${tile}-${year}*.*
#rm -Rf $GA_INST/log/*step*
#rm -Rf $GA_INST/tasks/*${tile}-${year}*.*

echo "done."
echo `date`
