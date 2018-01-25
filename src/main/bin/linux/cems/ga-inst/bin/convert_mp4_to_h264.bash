#!/bin/bash
set -e

year=$1

start=001
end=365

res=$2

FFMPEG=/group_workspaces/cems2/qa4ecv/vol1/globalabedo/soft/ffmpeg/ffmpeg-2.0.1-64bit-static/ffmpeg

OLDDIR=$PWD
cd /group_workspaces/cems2/qa4ecv/vol1/public/olafd/Movies/albedo_avhrrgeo/$year/Merge/$res/PC
for movie in `ls *.mp4`; do
    #echo "converting movie '$movie' to h264..."
    # Qa4ecv.albedo.avh_geo.Merge.005.1985001.PC_BHR_VIS.mp4
    # becomes
    # Qa4ecv.albedo.avh_geo.Merge.005.1985001.PC_BHR_VIS.h264.mp4
    movie_base=`basename $movie .mp4`
    movie_conv=${movie_base}.h264.mp4
    echo "$FFMPEG -i $movie -c:v libx264 -vf scale=iw:ih -c:a libfaac -strict experimental $movie_conv"
    $FFMPEG -i $movie -c:v libx264 -vf scale=iw:ih -c:a libfaac -strict experimental $movie_conv
    
    echo "rm -f $movie"
    rm -f $movie
    echo "mv $movie_conv $movie"
    mv $movie_conv $movie
done

echo "done."

cd $OLDDIR

