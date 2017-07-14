#!/bin/bash

brfPath=$1
brfBaseName=$2
bbdrDir=$3
gaRootDir=$4
beamRootDir=$5

if [ ! -e "$bbdrDir" ]
then
    mkdir -p $bbdrDir
fi

echo "Create AVHRR BBDR from BRF tile products..."

target=$bbdrDir/${brfBaseName}_BBDR.nc

# avhrrBrfPhiBandName=REL_PHI in new v5, 201707 (was PHI before)
# avhrrLtdrFlagBandName=LTDR_FLAG in new v5, 201707 (was LDTR_FLAG before)
echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.avhrr -e -c 3000M -SsourceProduct=$brfPath -PavhrrBrfPhiBandName=REL_PHI -PavhrrLtdrFlagBandName=LTDR_FLAG -f NetCDF4-BEAM -t $target"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.bbdr.avhrr -e -c 3000M -SsourceProduct=$brfPath -PavhrrBrfPhiBandName=REL_PHI -PavhrrLtdrFlagBandName=LTDR_FLAG -f NetCDF4-BEAM -t $target
status=$?
echo "Status: $status"

echo `date`
