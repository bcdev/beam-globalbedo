#!/bin/bash

gaRootDir=$1

awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
#    echo $tile $ulx $uly
    if [ -e ${gaRootDir}/Merge/$tile ] 
    then
        echo "${gaRootDir}/Merge/$tile already exists."
    else
        #Create tile subdirectories
        echo "creating directory ${gaRootDir}/Merge/$tile..."
        mkdir -v ${gaRootDir}/Merge/$tile
    fi
done

