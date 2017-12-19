#!/bin/bash
set -e

year=$1

start=001
end=366

# check number of files: 46 doys * 326 tiles = 14996 nc files for albedo and brdf tiles
OLDDIR=$PWD
cd $GA_INST/../GlobAlbedoTest/Albedo/NoSnow/avh_geo/$year
echo "ALBEDO NOSNOW #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do 
    echo "$tile : `ls $tile/*.nc |wc`" 
    #echo "$tile : `ls -l $tile/*.nc |grep Dec |wc`" 
    for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
        albedoNoSnowFile=Qa4ecv.albedo.avh_geo.$year$doy.$tile.NoSnow.nc
        if [ ! -f "$PWD/$tile/$albedoNoSnowFile" ]
	then
	    echo "missing NOSNOW albedo file: $PWD/$tile/$albedoNoSnowFile"
	fi
    done
done

cd $GA_INST/../GlobAlbedoTest/Albedo/Snow/avh_geo/$year
echo "ALBEDO SNOW #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do
    echo "$tile : `ls $tile/*.nc |wc`"
    #echo "$tile : `ls -l $tile/*.nc |grep Dec |wc`" 
    for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
        albedoSnowFile=Qa4ecv.albedo.avh_geo.$year$doy.$tile.Snow.nc
        if [ ! -f "$PWD/$tile/$albedoSnowFile" ]
        then
            echo "missing SNOW albedo file: $PWD/$tile/$albedoSnowFile"
        fi
    done
done

echo "done."

cd $OLDDIR

