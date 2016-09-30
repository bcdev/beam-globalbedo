#!/bin/bash

bbdrPath=$1
bbdrTileDir=$2
hStart=$3
hEnd=$4
gaRootDir=$5
beamRootDir=$6

if [ ! -e "$bbdrTileDir" ]
then
    mkdir -p $bbdrTileDir
fi

echo "Create AVHRR BRF spectral/broadband tile products from global products..."

echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.avhrr -e -c 3000M -SsourceProduct=$bbdrPath -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hStart -PhorizontalTileEndIndex=$hEnd"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.avhrr -e -c 3000M -SsourceProduct=$bbdrPath -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hStart -PhorizontalTileEndIndex=$hEnd
status=$?
echo "Status: $status"

echo `date`
