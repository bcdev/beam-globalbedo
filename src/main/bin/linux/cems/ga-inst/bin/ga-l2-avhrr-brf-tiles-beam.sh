#!/bin/bash

brfPath=$1
brfTileDir=$2
hStart=$3
hEnd=$4
gaRootDir=$5
beamRootDir=$6

if [ ! -e "$brfTileDir" ]
then
    mkdir -p $brfTileDir
fi

echo "Create AVHRR BRF spectral/broadband tile products from global products..."

echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.avhrr -e -c 3000M -SsourceProduct=$brfPath -PconvertToBbdr=false -PbbdrDir=$brfTileDir -PhorizontalTileStartIndex=$hStart -PhorizontalTileEndIndex=$hEnd"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.avhrr -e -c 3000M -SsourceProduct=$brfPath -PconvertToBbdr=false -PbbdrDir=$brfTileDir -PhorizontalTileStartIndex=$hStart -PhorizontalTileEndIndex=$hEnd
status=$?
echo "Status: $status"

echo `date`
