#!/bin/bash

tile=$1
SRCDIR="/data/GlobAlbedo/src"

OUTPUTDIR="/data/GlobAlbedo/MonthlyAlbedo/$tile"
#mkdir -p $OUTPUTDIR

month=$2
 echo  /opt/epd-7.0-2-rh5-x86_64/bin/python $SRCDIR/MonthlyAlbedoFrom8day_work.py $tile $month
 /opt/epd-7.0-2-rh5-x86_64/bin/python $SRCDIR/MonthlyAlbedoFrom8day_work.py $tile $month
