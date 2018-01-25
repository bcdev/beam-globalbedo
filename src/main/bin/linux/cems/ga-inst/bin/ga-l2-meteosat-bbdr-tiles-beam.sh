#!/bin/bash

year=$1
bbdrPath=$2
bbdrTileDir=$3
diskId=$4
hIndex=$5
sensor=$6
gaRootDir=$7
beamRootDir=$8

avhrrMaskRootDir=$gaRootDir/MsslAvhrrMask

mkdir -p $bbdrTileDir

if [ "$sensor" == "MVIRI" ]
then
    latlonPath=$gaRootDir/auxdata/${sensor}/MET_${diskId}_VIS01_LatLon.nc
else
    latlonPath=$gaRootDir/auxdata/${sensor}/MSG_${diskId}_RES01_LatLon.nc
fi

# remove global attribute 'granule_name' from source BRF, as it is null in 201801 new delivery, causing NPE in UCAR netcdf/hdf library
ncatted -a "granule(\.*)+",global,d,, $bbdrPath

echo "Create Meteosat BBDR spectral/broadband tile products from disk products..."

echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.meteosat -e -c 3000M -SsourceProduct=$bbdrPath -SlatlonProduct=$latlonPath -Pyear=$year -PavhrrMaskRootDir=$avhrrMaskRootDir -Psensor=$sensor -PconvertToBbdr=true -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hIndex -PhorizontalTileEndIndex=$hIndex"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.meteosat -e -c 3000M -SsourceProduct=$bbdrPath -SlatlonProduct=$latlonPath -Pyear=$year -PavhrrMaskRootDir=$avhrrMaskRootDir -Psensor=$sensor -PconvertToBbdr=true -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hIndex -PhorizontalTileEndIndex=$hIndex
status=$?
echo "Status: $status"

echo `date`
