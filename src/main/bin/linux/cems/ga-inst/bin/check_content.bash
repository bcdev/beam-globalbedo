#!/bin/bash
set -e

year=$1

# check number of files: 46 doys * 326 tiles = 14996 nc files for albedo and brdf tiles
OLDDIR=$PWD
cd $GA_INST/../GlobAlbedoTest/Albedo/$year
echo "ALBEDO #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do echo "$tile : `ls $tile/*.nc |wc`"; done

#cd $GA_INST/../GlobAlbedoTest/Inversion/Merge/$year
#echo "BRDF MERGE #tiles: `ls -d h*v* |wc`"
#for tile in `ls -d h*v*`; do echo "$tile : `ls $tile/*.nc |wc`"; done

cd $GA_INST/../GlobAlbedoTest/Inversion/NoSnow/$year
echo "BRDF NOSNOW #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do echo "$tile : `ls $tile/*.nc |wc`"; done

#cd $GA_INST/../GlobAlbedoTest/Inversion/Snow/$year
#echo "BRDF SNOW #tiles: `ls -d h*v* |wc`"
#for tile in `ls -d h*v*`; do echo "$tile : `ls $tile/*.nc |wc`"; done


cd $OLDDIR

