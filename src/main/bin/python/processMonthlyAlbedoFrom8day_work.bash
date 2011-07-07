#!/bin/bash

tile=$1
SRCDIR="/data/GlobAlbedo/src"

OUTPUTDIR="/data/GlobAlbedo/MonthlyAlbedo/$tile"
#mkdir -p $OUTPUTDIR

month=$2
echo $SRCDIR/MonthlyAlbedoFrom8day_work.py $tile $month
$SRCDIR/MonthlyAlbedoFrom8day_work.py $tile $month
