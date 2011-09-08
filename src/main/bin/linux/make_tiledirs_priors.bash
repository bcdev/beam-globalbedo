#!/bin/bash

gaRootDir=$1

awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
#    echo $tile $ulx $uly
    if [ -e ${gaRootDir}Priors/$tile ] 
    then
        echo "${gaRootDir}Priors/$tile already exists."
    else
        #Create tile subdirectories
        echo "creating directory ${gaRootDir}Priors/$tile..."
        mkdir -v ${gaRootDir}Priors/$tile
    fi
done

