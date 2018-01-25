#!/bin/bash
set -e

year=$1

start=001
end=365

res=$2

OLDDIR=$PWD
# e.g.  ../GlobAlbedoTest/Mosaic/Albedo/NoSnow/avh_geo/2001/05/Qa4ecv.albedo.avh_geo.NoSnow.05.2001020.PC.nc
# e.g.  /group_workspaces/cems2/qa4ecv/vol4/olafd/qa4ecv_archive/qa4ecv/albedo/L3_Mosaic_NoSnow/v0.9/2001/QA4ECV-L3-Mosaic-Albedo-NoSnow-05-2001337.nc
#cd $GA_INST/../GlobAlbedoTest/Mosaic/Albedo/NoSnow/avh_geo/$year/$res
cd /group_workspaces/cems2/qa4ecv/vol3/olafd/qa4ecv_archive/qa4ecv/albedo/L3_Mosaic_NoSnow/v0.92/$year
echo "ALBEDO NOSNOW #mosaics: `ls QA4ECV*.nc |wc`"
for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    albedoNoSnowFile=QA4ECV-L3-Mosaic-Albedo-NoSnow-${res}-$year$doy.nc
    if [ ! -f "$PWD/$tile/$albedoNoSnowFile" ]
    then
        echo "missing NOSNOW albedo mosaic: $PWD/$albedoNoSnowFile"
    fi
done

cd /group_workspaces/cems2/qa4ecv/vol3/olafd/qa4ecv_archive/qa4ecv/albedo/L3_Mosaic_Snow/v0.92/$year
echo "ALBEDO SNOW #mosaics: `ls QA4ECV*.nc |wc`"
for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    albedoSnowFile=QA4ECV-L3-Mosaic-Albedo-Snow-${res}-$year$doy.nc
    if [ ! -f "$PWD/$tile/$albedoSnowFile" ]
    then
        echo "missing SNOW albedo mosaic: $PWD/$albedoSnowFile"
    fi
done

cd /group_workspaces/cems2/qa4ecv/vol3/olafd/qa4ecv_archive/qa4ecv/albedo/L3_Mosaic_Merge/v0.92/$year
echo "ALBEDO MERGE #mosaics: `ls QA4ECV*.nc |wc`"
for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
    albedoMergeFile=QA4ECV-L3-Mosaic-Albedo-Merge-${res}-$year$doy.nc
    if [ ! -f "$PWD/$tile/$albedoMergeFile" ]
    then
        echo "missing MERGE albedo mosaic: $PWD/$albedoMergeFile"
    fi
done


echo "done."

cd $OLDDIR

