#!/bin/bash


###################################################################
#this program is creating browse images from BBDR netcdf files
#authors: Said Kharbouche, MSSL.UCL(2014); O.Danne, BC (2016)
####################################################################


###################################################### INPUT #####################################################

#netcdf BBDR input file
INPUT=$1

if [ ! -f "$INPUT" ]
then
    echo "Nc2browse input file '$INPUT' does not exist - will exit."
    exit 1
fi

#output directory
OUTDIR=$2

#scripts and colorlut directory
#HOME=/group_workspaces/cems/globalalbedo/scripts/
HOME=$GA_INST/staging_said
###################################################################################################################

#if not existing, create output directory
mkdir -p $OUTDIR

idxDate=5  # todo: this is MVIRI only

COLORTXT='white'

PYTHON1=$HOME/python/ncbbdr2png_od.py
BANDS1=BB_VIS,BB_NIR,BB_SW
MINMAX1=0:0.2,0:0.2,0:0.2
LUT1=$HOME/params/color_lut.txt
BANDSname1=$BANDS1

SIZE='600x600'

\
echo -e "\n\n\n-------------------------------------------------------------"
echo python2.7 ${PYTHON1} $INPUT $OUTDIR  $BANDS1  $MINMAX1  $LUT1  $SIZE $idxDate $COLORTXT $BANDSname1
python2.7 ${PYTHON1} $INPUT $OUTDIR  $BANDS1  $MINMAX1  $LUT1  $SIZE $idxDate $COLORTXT $BANDSname1

echo -e "\n\n\nDone."





