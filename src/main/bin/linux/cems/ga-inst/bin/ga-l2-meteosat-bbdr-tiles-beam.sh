#!/bin/bash

bbdrPath=$1
bbdrTileDir=$2
diskId=$3
hIndex=$4
sensor=$5
gaRootDir=$6
beamRootDir=$7

if [ ! -e "$bbdrTileDir" ]
then
    mkdir -p $bbdrTileDir
fi

if [ "$sensor" == "MVIRI" ]
then
    latlonPath=$gaRootDir/auxdata/${sensor}/MET_${diskId}_VIS01_LatLon.nc
else
    latlonPath=$gaRootDir/auxdata/${sensor}/MSG_${diskId}_RES01_LatLon.nc
fi

echo "Create Meteosat BBDR spectral/broadband tile products from disk products..."

echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.meteosat -e -c 3000M -SsourceProduct=$bbdrPath -SlatlonProduct=$latlonPath -Psensor=$sensor -PconvertToBbdr=true -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hIndex -PhorizontalTileEndIndex=$hIndex"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.meteosat -e -c 3000M -SsourceProduct=$bbdrPath -SlatlonProduct=$latlonPath -Psensor=$sensor -PconvertToBbdr=true -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hIndex -PhorizontalTileEndIndex=$hIndex
status=$?
echo "Status: $status"

echo `date`
