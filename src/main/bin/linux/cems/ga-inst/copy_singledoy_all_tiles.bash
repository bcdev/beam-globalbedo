#!/bin/bash
set -e

year=2005

OLDDIR=$PWD
#cd $GA_INST/../GlobAlbedoTest/Albedo/$year
cd $GA_INST/../GlobAlbedoTest/Albedo/${year}_8day
echo "ALBEDO #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do 
    #mkdir -p $GA_INST/to_bc/tiles_for_mosaic_200/$tile;
    mkdir -p $GA_INST/to_bc/tiles_for_mosaic_1200/$tile;
    file2copy=$tile/GlobAlbedo.albedo.${year}121.$tile.nc 
    if [ -f "$file2copy" ];
    then
        cp $file2copy $GA_INST/to_bc/tiles_for_mosaic_1200/$tile
    fi
done

cd $OLDDIR

