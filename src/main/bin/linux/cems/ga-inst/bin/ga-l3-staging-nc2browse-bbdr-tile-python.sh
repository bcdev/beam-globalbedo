#!/bin/bash


###################################################################
# Wrapper script to call Python from LSF job
# authors: Said Kharbouche, MSSL.UCL(2014); O.Danne, BC (2016)
####################################################################

stagingNc2browseFile=$1
stagingNc2browseResultDir=$2
gaRootDir=$3
datestring=$4
band=$5
plot_min=$6
plot_max=$7

if [ ! -f "$stagingNc2browseFile" ]
then
    echo "Nc2browse input file '$stagingNc2browseFile' does not exist - will exit."
    echo "Status: 1"
    exit 1
fi

mkdir -p $stagingNc2browseResultDir

stagingDir=${gaRootDir}/staging
stagingSrcDir=bin/staging_said

LUT=${stagingSrcDir}/params/color_lut.txt
MINMAX=${plot_min}:${plot_max}
SIZE='600x600'
COLORTXT='white'

pythonCmd=${stagingSrcDir}/python/ncbbdr2png.py

echo -e "\n\n\n-------------------------------------------------------------"
echo "python2.7 ${pythonCmd} ${stagingNc2browseFile} ${stagingNc2browseResultDir} ${datestring} ${band} ${MINMAX} ${LUT} ${SIZE} ${COLORTXT}"
python2.7 ${pythonCmd} ${stagingNc2browseFile} ${stagingNc2browseResultDir} ${datestring} ${band} ${MINMAX} ${LUT} ${SIZE} ${COLORTXT}

status=$?
echo "Status: $status"

echo -e "\n\n\nDone."





