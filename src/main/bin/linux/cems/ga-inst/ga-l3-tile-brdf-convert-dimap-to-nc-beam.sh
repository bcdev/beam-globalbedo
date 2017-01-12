#!/bin/bash

tile=$1
year=$2
doy=$3
gaRootDir=$4
beamRootDir=$5
brdfTargetDir=${6}

gpt=$beamRootDir/bin/gpt-d-l2.sh

inversionMergeTargetDir=$gaRootDir/Inversion/Merge/$year/$tile

echo "Compute BRDF Dimap to NC for tile $tile, year $year, DoY $doy ..."
SRC=${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.dim
TARGET=${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.nc
echo "time $gpt ga.passthrough -SsourceProduct=$SRC -f NetCDF4-BEAM -t $TARGET"
time $gpt ga.passthrough -SsourceProduct=$SRC -f NetCDF4-BEAM -t $TARGET
status=$?
echo "Status: $status"

echo `date`
