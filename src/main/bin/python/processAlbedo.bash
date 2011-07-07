#!/bin/bash

FUNC=processAlbedo
init_time=`date`

echo ""
echo ${FUNC}: Checking command line arguments
echo `date`

#tile="h18v04"
tile=$1
year=$2

day=$3
if (($day < "10")); then
    DoY=00$day
elif (($day < "100")); then
    DoY=0$day
else
    DoY=$day
fi

echo "Processing tile $tile, year $year, DoY $DoY..."

DATADIR="/bcserver12-data/GlobAlbedo/Merge/$tile"
ANGLESDIR="/bcserver12-data/GlobAlbedo/Angles/$tile"
SRCDIR="/bcserver12-data/GlobAlbedo/src"
OUTDIR="/bcserver12-data/GlobAlbedo/Albedo/$tile"

MergeFile="$DATADIR/GlobAlbedo.Merge.$year$DoY.$tile.bin"
AnglesFile="$ANGLESDIR/$tile.$DoY.SZA.bin"

if [-e "$OUTDIR/GlobAlbedo.$year$DoY.$tile.bin" ]; then
    echo "File GlobAlbedo.$year$DoY.$tile.bin already processed."
else
    echo $SRCDIR/Albedo_work.py $tile $MergeFile $AnglesFile
    /opt/epd-7.0-2-rh5-x86_64/bin/python $SRCDIR/Albedo_work.py $tile $MergeFile $AnglesFile
fi

end_time=`date`

echo "Starting time: $init_time - Finishing time: $end_time"
echo "Albedo processing for tile $tile finished."
