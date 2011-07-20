#!/bin/bash

gaRootDir=$1

awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
#    echo $tile $ulx $uly
    if [ -e ${gaRootDir}/Albedo/$tile ] 
    then
        echo "${gaRootDir}/Albedo/$tile already exists."
    else
        #Create tile subdirectories
        echo "creating directory ${gaRootDir}/Albedo/$tile..."
        mkdir -v ${gaRootDir}/Albedo/$tile
    fi
done

