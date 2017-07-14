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

# new products have bz2 format:
# e.g. AVH_19980701_001D_900S900N1800W1800E_0005D_BRDF_N14.NC.bz2
bbdrNcFileName=`basename $bbdrPath .NC.bz2`

# new 201707: zip format
# AVHRR2_NOAA07_19820102_19820102_L1_BRF_900S900N1800W1800E_PLC_0005D_v03.zip
bbdrNcFileName=`basename $bbdrPath .zip`

# this is the unzipped product in tmp folder (from previous step):
# TODO: delete this product once all rows are processed
bbdrInputProduct=$gaRootDir/tmp/${bbdrNcFileName}.NC

# use this if file was already unzipped (not the case for v5!)
# bbdrInputProduct=$bbdrPath

# 2. process
#echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.avhrr -e -c 3000M -SsourceProduct=$bbdrPath -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hStart -PhorizontalTileEndIndex=$hEnd"
#time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.avhrr -e -c 3000M -SsourceProduct=$bbdrPath -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hStart -PhorizontalTileEndIndex=$hEnd

echo "time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.avhrr -e -c 3000M -SsourceProduct=$bbdrInputProduct -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hStart -PhorizontalTileEndIndex=$hEnd"
time  $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.tile.avhrr -e -c 3000M -SsourceProduct=$bbdrInputProduct -PbbdrDir=$bbdrTileDir -PhorizontalTileStartIndex=$hStart -PhorizontalTileEndIndex=$hEnd

status=$?
echo "Status: $status"

# 3. remove unpacked source product
#rm -f $bbdrInputProduct

echo `date`
