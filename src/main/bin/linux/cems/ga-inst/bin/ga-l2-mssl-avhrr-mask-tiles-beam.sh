#!/bin/bash

msslMaskPath=$1
msslMaskTileDir=$2
gaRootDir=$3
beamRootDir=$4

if [ ! -e "$msslMaskTileDir" ]
then
    mkdir -p $msslMaskTileDir
fi

echo "Create AVHRR MSSL mask tile products from global products..."

echo "time  $beamRootDir/bin/gpt-d-l2.sh ga.tile.msslflag -e -c 3000M -SsourceProduct=$msslMaskPath -PmsslFlagTileDir=$msslMaskTileDir"
time  $beamRootDir/bin/gpt-d-l2.sh ga.tile.msslflag -e -c 3000M -SsourceProduct=$msslMaskPath -PmsslFlagTileDir=$msslMaskTileDir
status=$?
echo "Status: $status"

echo `date`
