#!/bin/bash
set -e

# copies for all tiles 19940831 into all later doys of that year

year=1994

OLDDIR=$PWD
cd $GA_INST/../GlobAlbedoTest/MsslAvhrrMask/1994
for tile in `ls -d h*v*`; do 
    #echo "$tile :" 
    cd $tile
    maskRefFile=msslFlag_v2__AVHRR2_NOAA11_19940831_19940831_L1_BRF_900S900N1800W1800E_PLC_0005D_v04_${tile}.nc
    #for maskFile in `ls *199409*.nc`; do
        #echo "rm -f $maskFile "
        #rm -f $PWD/$maskFile
        #echo "cp -p $maskRefFile $maskFile"
        #cp -p $PWD/$maskRefFile $PWD/$maskFile
    #done
    for maskFile in `ls *19941*.nc`; do
        echo "cp -p $maskRefFile $maskFile"
        cp -p $PWD/$maskRefFile $PWD/$maskFile
    done
    cd ..
done

echo "done."

cd $OLDDIR

