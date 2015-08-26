#!/bin/bash

tile=$1
year=$2
doy=$3
gaRootDir=$4
priorRootDir=$5
snowPriorRootDir=$6  # explicitly needed in merge step
beamRootDir=$7
albedoTargetDir=$8

if [ ! -d "$albedoTargetDir" ]
then
    mkdir -p $albedoTargetDir
fi

bbdrRootDir=$gaRootDir/BBDR
inversionDir=$gaRootDir/Inversion
inversionMergeDir=$inversionDir/$year/$tile/merge
if [ ! -d "$inversionMergeDir" ]
then
    mkdir -p $inversionMergeDir
fi

echo "Merge and Albedo computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $doy ..."
if [ -d "$priorRootDir/$tile" ] 
then
    echo "Compute MERGED BRDF for tile $tile, year $year, DoY $doy ..."
    echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorRootDir -e -t $inversionMergeDir/GlobAlbedo.brdf.merge.$year$doy.$tile.dim"
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorRootDir -e -t $inversionMergeDir/GlobAlbedo.brdf.merge.$year$doy.$tile.dim
    status=$?
    echo "Status: $status"

    echo "Compute ALBEDO for tile $tile, year $year, DoY $doy ..."
    echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorRootDir -e -t $albedoTargetDir/GlobAlbedo.albedo.$year$doy.$tile.dim"
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorRootDir -e -t $albedoTargetDir/GlobAlbedo.albedo.$year$doy.$tile.dim
    status=$?
    echo "Status: $status"

    rm -Rf $PWD/target.*
else
    echo "Directory '$priorRootDir/$tile' does not exist - no Merge and Albedo computed for tile $tile, year $year, DoY $doy."
fi

echo `date`
