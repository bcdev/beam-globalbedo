#!/bin/bash
set -e

year=$1

# check number of files: 46 doys * 326 tiles = 14996 nc files for albedo and brdf tiles
OLDDIR=$PWD
#cd $GA_INST/../GlobAlbedoTest/BBDR/DailyAcc/$year
cd /group_workspaces/cems2/qa4ecv/vol3/olafd/GlobAlbedoTest/DailyAccumulators/$year
echo "Dailyacc #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do echo "NoSnow $tile : `ls $tile/NoSnow/*.bin |wc`"; done
for tile in `ls -d h*v*`; do echo "Snow $tile : `ls $tile/Snow/*.bin |wc`"; done

cd $OLDDIR

