#!/bin/bash

gaRootDir=$1

awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
#    echo $tile $ulx $uly
    if [ -e ${gaRootDir}MonthlyAlbedo/$tile ] 
    then
        echo "${gaRootDir}MonthlyAlbedo/$tile already exists."
    else
        #Create tile subdirectories
        echo "creating directory ${gaRootDir}MonthlyAlbedo/$tile..."
        mkdir -v ${gaRootDir}MonthlyAlbedo/$tile
    fi
done

