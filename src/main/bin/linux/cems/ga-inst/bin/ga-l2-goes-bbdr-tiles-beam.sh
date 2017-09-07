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

latlonPath=$gaRootDir/auxdata/${sensor}/GOES_${diskId}_VIS02_LatLon.nc

echo "Create $sensor BBDR spectral/broadband tile products from disk products..."

echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.meteosat -e -c 3000M -SsourceProduct=$bbdrPath -SlatlonProduct=$latlonPath -Pyear=$year -PavhrrMaskRootDir=$avhrrMaskRootDir -Psensor=$sensor -PconvertToBbdr=true -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hIndex -PhorizontalTileEndIndex=$hIndex"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.meteosat -e -c 3000M -SsourceProduct=$bbdrPath -SlatlonProduct=$latlonPath -Pyear=$year -PavhrrMaskRootDir=$avhrrMaskRootDir -Psensor=$sensor -PconvertToBbdr=true -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hIndex -PhorizontalTileEndIndex=$hIndex
status=$?
echo "Status: $status"

echo `date`
