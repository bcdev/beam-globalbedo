#!/bin/bash
set -e

year=2005

# check number of files: 46 doys * 326 tiles = 14996 nc files for albedo and brdf tiles
OLDDIR=$PWD
cd ../../GlobAlbedoTest/Albedo/$year
echo "#tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do echo "$tile : `ls $tile/*.nc |wc`"; done
#for tile in `ls -d h*v*`; do echo "$tile : `ls $tile/*.nc |wc |grep 42`"; done

cd ../../GlobAlbedoTest/Inversion/$year
echo "#tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do echo "$tile : `ls $tile/*.nc |wc`"; done
#for tile in `ls -d h*v*`; do echo "$tile : `ls $tile/*.nc |wc |grep 42`"; done

cd $OLDDIR

