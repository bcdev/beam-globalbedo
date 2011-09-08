#!/bin/bash

gaRootDir=$1
startYear=1997
endYear=2011

if [ -e ${gaRootDir}/BBDR ]
then
    echo "${gaRootDir}/BBDR already exists."
else
    #Create sensor subdirectory
    mkdir -v ${gaRootDir}/BBDR
fi

if [ -e ${gaRootDir}/BBDR/AccumulatorFiles ]
then
    echo "${gaRootDir}/BBDR/AccumulatorFiles already exists."
else
    #Create sensor subdirectory
    mkdir -v ${gaRootDir}/BBDR/AccumulatorFiles
fi

awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
#    echo $tile $ulx $uly
    year=$startYear
    while [ $year -le $endYear ]
    do
        if [ -e ${gaRootDir}/BBDR/AccumulatorFiles/$year ] 
        then
            mkdir -v ${gaRootDir}/BBDR/AccumulatorFiles/$year/$tile
            mkdir -v ${gaRootDir}/BBDR/AccumulatorFiles/$year/$tile/Snow
            mkdir -v ${gaRootDir}/BBDR/AccumulatorFiles/$year/$tile/NoSnow
        else
            #Create year and tile subdirectories
            mkdir -v ${gaRootDir}/BBDR/AccumulatorFiles/$year
            mkdir -v ${gaRootDir}/BBDR/AccumulatorFiles/$year/$tile
            mkdir -v ${gaRootDir}/BBDR/AccumulatorFiles/$year/$tile/Snow
            mkdir -v ${gaRootDir}/BBDR/AccumulatorFiles/$year/$tile/NoSnow
        fi
        let year=$year+1
    done
done

