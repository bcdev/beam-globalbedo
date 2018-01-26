#!/bin/bash

####################################################################
#This program is about creating movie from browse iamges of albedo
#author: Said Kharbouche, MSSL.UCL(2014)
####################################################################




############################################################# INPUTs#############################################
#
#dircotory of images for a given year
INDIR=$1
#target year
YEAR=$2
#output diroctory
OUTDIR=$3

#position of date in input file name
IDXDATE=
#input images  extension
INEXT='png'
#output movie extension
#OUTEXT='mov'
OUTEXT='mp4'
#movie size
size=1200x600

#FFMPEG path
#ffmpeg=/group_workspaces/cems/globalalbedo/soft/ffmpeg/ffmpeg-2.0.1-64bit-static/ffmpeg
# SK 20161118 (note the typo 'globalabedo'...):
ffmpeg=/group_workspaces/cems2/qa4ecv/vol1/globalabedo/soft/ffmpeg/ffmpeg-2.0.1-64bit-static/ffmpeg
#################################################################################################################


######################
#determine the file name of the output move
outfile=$(ls $INDIR | head -1)
echo "outfile (1): $outfile"
outfile=$(basename $outfile .$INEXT)
IFS='.' read -a tab <<< "${outfile}"
date="${tab[${IDXDATE}]}"
outfile=${outfile//".$date."/".$YEAR."}
outfile=$OUTDIR/${outfile}.${OUTEXT}
echo "OUTDIR: $OUTDIR"
echo "outfile (2): $outfile"
#####################



#create output directory
mkdir -p $OUTDIR



################################################################################################
#launch ffmpeg
#echo $ffmpeg -y -r 3 -f image2 -pattern_type glob -i "$indir/png/*.png" -vcodec mpeg4 $moviefile

#$ffmpeg -y -r 3 -f image2 -pattern_type glob -i  "$INDIR/*.$INEXT" -s $size  -vcodec mpeg4 $outfile
$ffmpeg -y -r 3 -f image2 -pattern_type glob -i  "$INDIR/*.$INEXT" -s $size -b 4M -vcodec mpeg4 $outfile
################################################################################################

echo 'End.'





