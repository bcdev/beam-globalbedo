#!/bin/tcsh

#input parameters:
set l1bproduct = $1
set sensor = $2   # MERIS, AATSR or VGT
set year = $3

set l1bRootDir = $4   # at BC: "/data/MER_RR__1P"
set gaRootDir = $5    # at BC:  /bcserver12-data/GlobAlbedo
set beamRootDir = $6  # at BC:  /opt/beam-4.9.0.1

set gpt = $beamRootDir/bin/gpt-d-l2.sh

if ($sensor == "MERIS") then
    set l1bBaseName = `basename $l1bproduct .N1`
endif

if ($sensor == "VGT") then
    set l1bBaseName = `basename $l1bproduct .ZIP`
endif

set bbdrL2Dir = $gaRootDir/BBDR_L2/$sensor/$year/
set bbdrTileDir = $gaRootDir/BBDR/$sensor/$year/
set bbdrFile = $bbdrL2Dir/${l1bBaseName}_BBDR.dim

mkdir -p $bbdrL2Dir
mkdir -p $bbdrTileDir

time $gpt ga.l2 -e -c 8000M -q 24 -Psensor=$sensor -t $bbdrFile $l1bRootDir/$year/$l1bproduct
if ($status == "0") then
    echo "BBDR product created."

    time $gpt ga.tile -e -c 8000M -q 8 -PbbdrDir=$bbdrTileDir  $bbdrFile
    echo "Tile products created."
else
    echo "No BBDR product created."
endif

echo "Removing BBDR intermediate product..."
if ( -e "${bbdrL2Dir}/${l1bBaseName}_BBDR.dim" ) then
    rm -f ${bbdrL2Dir}/${l1bBaseName}_BBDR.dim
    rm -rf ${bbdrL2Dir}/${l1bBaseName}_BBDR.data
endif

if ($sensor == "VGT") then
    echo "Removing tmp products..."
    rm -rf /tmp/*    
    rm -f $l1bRootDir/$year/$l1bBaseName
endif

echo "Done."

