#!/bin/bash

tile=$1
year=$2
doy=$3
snow=$4
gaRootDir=$5
priorRootDir=$6
beamRootDir=$7
inversionTargetDir=$8

if [ ! -d "$inversionTargetDir" ]
then
   mkdir -p $inversionTargetDir
fi

if [ $snow == "true" ]
then
    snowId=Snow
else
    snowId=NoSnow
fi

wings=90  # tbd later

echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $doy ..."
if [ -d "$priorRootDir/$tile" ] 
then
    echo "Compute BRDF for tile $tile, year $year, DoY $doy, $snowId ..."
    echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=$wings -PcomputeSnow=$snow -PgaRootDir=$gaRootDir -PpriorRootDir=$priorRootDir -e -t ${inversionTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.$snowId.dim"
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=$wings -PcomputeSnow=$snow -PgaRootDir=$gaRootDir -PpriorRootDir=$priorRootDir -e -t ${inversionTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.$snowId.dim
    status=$?
    echo "Status: $status"
else
    echo "Directory '$priorRootDir/$tile' does not exist - no BRDF computed for tile $tile, year $year, DoY $doy."
fi

echo `date`
