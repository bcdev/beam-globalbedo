#!/bin/bash

sdrModisPath=$1
sdrModisTileDir=$2
gaRootDir=$3
beamRootDir=$4

echo "Create MODIS SDR subtile products..."

# MODIS SDR tiles --> subtiles:
echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.tile.subtile -e -SsourceProduct=$sdrModisPath -PsubtileFactor=4"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.tile.subtile -e -SsourceProduct=$sdrModisPath -PsubtileFactor=4

status=$?
echo "Status: $status"

echo `date`
