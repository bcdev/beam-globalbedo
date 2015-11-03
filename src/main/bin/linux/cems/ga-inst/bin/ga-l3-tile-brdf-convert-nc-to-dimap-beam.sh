#!/bin/bash

tile=$1
year=$2
doy=$3
gaRootDir=$4
beamRootDir=$5
brdfTargetDir=${6}

gpt=$beamRootDir/bin/gpt-d-l2.sh

inversionMergeTargetDir=$gaRootDir/Inversion/Merge/$year/$tile

echo "Compute BRDF NC to Dimap for tile $tile, year $year, DoY $doy ..."
SRC=${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.nc
TARGET=${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.dim
echo "time $gpt ga.passthrough -SsourceProduct=$SRC -t $TARGET"
time $gpt ga.passthrough -SsourceProduct=$SRC -t $TARGET
status=$?
echo "Status: $status"

echo `date`
