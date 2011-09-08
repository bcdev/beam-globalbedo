#!/bin/bash

gaRootDir=$1

awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
#    echo $tile $ulx $uly
    if [ -e ${gaRootDir}/Inversion/$tile ] 
    then
        echo "${gaRootDir}/Inversion/$tile already exists."
    else
        #Create tile subdirectories
        echo "creating directory ${gaRootDir}/Inversion/$tile..."
        mkdir -v ${gaRootDir}/Inversion/$tile
    fi
done

