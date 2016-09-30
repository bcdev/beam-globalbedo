#!/bin/bash

bbdrPath=$1
bbdrTileDir=$2
diskId=$3
hIndex=$4
gaRootDir=$5
beamRootDir=$6

if [ ! -e "$bbdrTileDir" ]
then
    mkdir -p $bbdrTileDir
fi

latlonPath=$gaRootDir/auxdata/MVIRI/MET_${diskId}_VIS01_LatLon.nc

echo "Create Meteosat BRF spectral/broadband tile products from disk products..."

echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.meteosat -e -c 3000M -SsourceProduct=$bbdrPath -SlatlonProduct=$latlonPath -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hIndex -PhorizontalTileEndIndex=$hIndex"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.meteosat -e -c 3000M -SsourceProduct=$bbdrPath -SlatlonProduct=$latlonPath -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hIndex -PhorizontalTileEndIndex=$hIndex
status=$?
echo "Status: $status"

echo `date`
