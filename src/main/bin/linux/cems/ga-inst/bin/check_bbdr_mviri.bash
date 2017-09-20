#!/bin/bash
set -e

year=$1

OLDDIR=$PWD
cd $GA_INST/../GlobAlbedoTest/BBDR/MVIRI/$year
echo "BBDR MVIRI #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do echo "0 deg $tile : `ls $tile/*MVIRI_C_BBDR*.nc |wc`"; done
for tile in `ls -d h*v*`; do echo "57 deg $tile : `ls $tile/*MVIRI_057_C_BBDR*.nc |wc`"; done
for tile in `ls -d h*v*`; do echo "63 deg $tile : `ls $tile/*MVIRI_063_C_BBDR*.nc |wc`"; done

cd $OLDDIR

