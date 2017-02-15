#!/bin/bash


###################################################################
#this program is about creating brwose images frm albedo netcdf files
#author: Said Kharbouche, MSSL.UCL(2014)

# THIS VERSION: only 2 bands in nc file: BHR_SW, Weighted_Number_of_Samples
# (OD, 20161118)

####################################################################


###################################################### INPUTs #####################################################
###################################################################################################################

#list of netcdf albedo files
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
HOME=$GA_INST/bin/staging_said
###################################################################################################################

#if not existing, create output directory
mkdir -p $OUTDIR

# for product name like 'GlobAlbedo.albedo.NoSnow.005.2007365.PC.nc' the date appears after the 4th dot --> idxdate=4
#idxDate=4
# for product name like 'Qa4ecv.albedo.avhrrgeo.NoSnow.005.2007365.PC.nc' the date appears after the 5th dot --> idxdate=5
idxDate=5
SIZE05='720x360'
SIZE005='7200x3600'

COLORTXT='white'

#PYTHON1=$HOME/python/ncalbedo2png_od.py
PYTHON1=$HOME/python/ncalbedo2png_od_newcmap.py
BANDS1=BHR_SW
MINMAX1=0:1,0:1,0:1,0:1,0:1,0:1
#LUT1=$HOME/params/color_lut.txt
LUT1=$HOME/params/color_lut_ga.txt
BANDSname1=$BANDS1


PYTHON2=$HOME/python/ncalbedo2png_od.py
BANDS2=Weighted_Number_of_Samples
MINMAX2=1:11,0:30
BANDSname2=WNSamples
LUT2='none'

bn=$(basename $INPUT)

SIZE='none'

if [[ "$bn" =~ '.005.' ]]
then
	SIZE=$SIZE005
fi

if [[ "$bn" =~ '.05.' ]]
then
       	SIZE=$SIZE05
fi
\
echo -e "\n\n\n-------------------------------------------------------------"
echo python2.7 ${PYTHON1} $INPUT $OUTDIR  $BANDS1  $MINMAX1  $LUT1  $SIZE $idxDate $COLORTXT $BANDSname1
python2.7 ${PYTHON1} $INPUT $OUTDIR  $BANDS1  $MINMAX1  $LUT1  $SIZE $idxDate $COLORTXT $BANDSname1
echo -e "\n\n\n-------------------------------------------------------------"
echo python2.7 ${PYTHON2} $INPUT $OUTDIR  $BANDS2  $MINMAX2  $LUT2  $SIZE $idxDate $COLORTXT $BANDSname2
python2.7 ${PYTHON2} $INPUT $OUTDIR  $BANDS2  $MINMAX2  $LUT2  $SIZE $idxDate $COLORTXT $BANDSname2

status=$?
echo "Status: $status"

echo -e "\n\n\nDone."





