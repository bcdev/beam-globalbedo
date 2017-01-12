#!/bin/bash

year=$1
month=$2
day=$3
gaRootDir=$4
beamRootDir=$5
l1bN1SourceProduct=$6
l1bNcTargetProduct=$7
l1bNcTargetDir=$8
upperLat=$9
lowerLat=${10}

if [ ! -d "$l1bNcTargetDir" ]
then
   mkdir -p $l1bNcTargetDir
fi

gpt=$beamRootDir/bin/gpt-d-l2.sh

echo "Compute L1b N1 to NC for year $year, month $month, day $day ..."

#echo "time $gpt ga.passthrough -SsourceProduct=$l1bN1SourceProduct -f NetCDF4-BEAM -t $l1bNcTargetProduct"
#time $gpt ga.passthrough -SsourceProduct=$l1bN1SourceProduct -f NetCDF4-BEAM -t $l1bNcTargetProduct
#status=$?
#echo "Status: $status"

# the Dan Fisher Python code is slow, so prepare subsets...
echo "time $gpt Subset -Ssource=$l1bN1SourceProduct -PgeoRegion=POLYGON((-179.9 $upperLat, 179.9 $upperLat, 179.9 $lowerLat, -179.9 $lowerLat, -179.9 $upperLat)) -f NetCDF4-BEAM -t $l1bNcTargetProduct"
time $gpt Subset -Ssource=$l1bN1SourceProduct -PgeoRegion="POLYGON((-179.9 $upperLat, 179.9 $upperLat, 179.9 $lowerLat, -179.9 $lowerLat, -179.9 $upperLat))" -f NetCDF4-BEAM -t $l1bNcTargetProduct
status=$?
echo "Status: $status"

echo `date`
