#!/bin/bash
set -e

year=2001

OLDDIR=$PWD
cd $GA_INST/../GlobAlbedoTest/Albedo/avh_geo/$year
for tile in `ls -d h*v*`; do 
    #for snowfile in `ls $tile/Qa4ecv*.Snow.nc`; do
    #    if [ -f "$snowfile" ];
    #    then
    #        echo "mv $snowfile $GA_INST/../GlobAlbedoTest/Albedo/Snow/avh_geo/$year/$tile"
    #        mkdir -p $GA_INST/../GlobAlbedoTest/Albedo/Snow/avh_geo/$year/$tile
    #        mv $snowfile $GA_INST/../GlobAlbedoTest/Albedo/Snow/avh_geo/$year/$tile
    #    fi
    #done
    for nosnowfile in `ls $tile/Qa4ecv*.NoSnow.nc`; do
        if [ -f "$nosnowfile" ];
        then
            #echo "mv $nosnowfile $GA_INST/../GlobAlbedoTest/Albedo/NoSnow/avh_geo/$year/$tile"
            mkdir -p $GA_INST/../GlobAlbedoTest/Albedo/NoSnow/avh_geo/$year/$tile
            mv $nosnowfile $GA_INST/../GlobAlbedoTest/Albedo/NoSnow/avh_geo/$year/$tile
        fi
    done

done

cd $OLDDIR

