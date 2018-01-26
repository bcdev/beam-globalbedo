#!/bin/bash
set -e

year=$1

start=001
end=365

res=$2

OLDDIR=$PWD
# e.g.  ../GlobAlbedoTest/Mosaic/Albedo/Merge/avh_geo/2001/05/Qa4ecv.albedo.avh_geo.Merge.05.2001020.PC.nc
cd $GA_INST/../GlobAlbedoTest/Mosaic/Albedo/Merge/avh_geo/$year/$res
echo "ALBEDO MERGE #mosaics: `ls Qa4ecv*.nc |wc`"
for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    albedoMergeFile=Qa4ecv.albedo.avh_geo.Merge.$res.$year$doy.PC.nc
    if [ ! -f "$PWD/$albedoMergeFile" ]
    then
        echo "missing MERGE albedo mosaic: $PWD/$albedoMergeFile"
    fi
done

echo "done."

cd $OLDDIR

