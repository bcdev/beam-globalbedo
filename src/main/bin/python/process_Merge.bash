#!/bin/bash

FUNC=processMerge
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

PRIORDIR="/data/GlobAlbedo/Priors/$tile/background/processed.p1.0.618034.p2.1.00000"
SRCDIR="/data/GlobAlbedo/src"
INVERSIONDIR="/data/GlobAlbedo/Inversion/$tile"
OUTPUTDIR="/data/GlobAlbedo/Merge/$tile"

PriorMaskFile="$PRIORDIR/Kernels.$DoY.005.$tile.backGround.NoSnow.bin"

echo $SRCDIR/MergeBRDF_work.py $INVERSIONDIR/GlobAlbedo.$year$DoY.$tile.NoSnow.bin $INVERSIONDIR/GlobAlbedo.$year$DoY.$tile.Snow.bin $PriorMaskFile $OUTPUTDIR
/opt/epd-7.0-2-rh5-x86_64/bin/python $SRCDIR/MergeBRDF_work.py $INVERSIONDIR/GlobAlbedo.$year$DoY.$tile.NoSnow.bin $INVERSIONDIR/GlobAlbedo.$year$DoY.$tile.Snow.bin $PriorMaskFile $OUTPUTDIR



