#!/bin/bash
set -e

year=$1

# rename old Ga-style filenames to new QA4ECV filenames
# e.g. rename GlobAlbedo.albedo.1998121.h20v06.nc to Qa4ecv.avhrrgeo.albedo.1999318.h20v06.nc:
OLDDIR=$PWD
cd $GA_INST/../GlobAlbedoTest/Albedo/$year

echo "ALBEDO #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do
    echo "tile: $tile"
    for ncold in `ls $tile/GlobAlbedo.albedo.${year}*.$tile.nc`; do
        ncnew=${ncold//GlobAlbedo.albedo/Qa4ecv.avhrrgeo.albedo}
        mv $ncold $ncnew
    done
done

cd $OLDDIR
