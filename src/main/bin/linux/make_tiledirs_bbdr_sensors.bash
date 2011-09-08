#!/bin/bash

gaRootDir=$1
sensor=$2 # MERIS or VGT
startYear=1997
endYear=2011

if [ -e ${gaRootDir}/BBDR/$sensor ]
    then
    echo "${gaRootDir}/BBDR/$sensor already exists."
else
    #Create sensor subdirectory
    mkdir -v ${gaRootDir}/BBDR/$sensor
fi

awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
#    echo $tile $ulx $uly
    year=$startYear
    while [ $year -le $endYear ]
    do
        if [ -e ${gaRootDir}/BBDR/$sensor/$year ] 
        then
            echo "${gaRootDir}/BBDR/$sensor/$year already exists."
	    if [ -e ${gaRootDir}/BBDR/$sensor/$year/$tile ]
            then
                echo "${gaRootDir}/BBDR/$sensor/$year/$tile already exists."
            else
                #Create tile subdirectories
                mkdir -v ${gaRootDir}/BBDR/$sensor/$year/$tile
            fi
        else
            #Create year and tile subdirectories
            mkdir -v ${gaRootDir}/BBDR/$sensor
            mkdir -v ${gaRootDir}/BBDR/$sensor/$year
            mkdir -v ${gaRootDir}/BBDR/$sensor/$year/$tile
        fi
        let year=$year+1
    done
done

