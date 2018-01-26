#!/bin/bash
set -e

year=$1

start=001
end=365

res=$2

OLDDIR=$PWD
# e.g.  ../GlobAlbedoTest/Mosaic/Albedo/NoSnow/avh_geo/2001/05/Qa4ecv.albedo.avh_geo.NoSnow.05.2001020.PC.nc
cd $GA_INST/../GlobAlbedoTest/Mosaic/Albedo/NoSnow/avh_geo/$year/$res
echo "ALBEDO NOSNOW #mosaics: `ls Qa4ecv*.nc |wc`"
for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    albedoNoSnowFile=Qa4ecv.albedo.avh_geo.NoSnow.$res.$year$doy.PC.nc
    if [ ! -f "$PWD/$albedoNoSnowFile" ]
    then
        echo "missing NOSNOW albedo mosaic: $PWD/$albedoNoSnowFile"
    fi
done

cd $GA_INST/../GlobAlbedoTest/Mosaic/Albedo/Snow/avh_geo/$year/$res
echo "ALBEDO SNOW #mosaics: `ls Qa4ecv*.nc |wc`"
for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    albedoSnowFile=Qa4ecv.albedo.avh_geo.Snow.$res.$year$doy.PC.nc
    if [ ! -f "$PWD/$albedoSnowFile" ]
    then
        echo "missing SNOW albedo mosaic: $PWD/$albedoSnowFile"
    fi
done

echo "done."

cd $OLDDIR

