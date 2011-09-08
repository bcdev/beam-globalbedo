#!/bin/bash

# This scripts adds extension ".bin" to all prior files ending with "*Snow" and "*NoSnow".

priorsRootDir=$1

# Read all tile names...
awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
     # Snow files...
     tileSnowRootDir="${priorsRootDir}/$tile"
     if [ -d $tileSnowRootDir ]
     then
        listOfSnowBinFiles=`ls ${priorsRootDir}/$tile/background/processed.p1.0.618034.p2.1.00000/Kernels.*Snow` 
        for file in $listOfSnowBinFiles
        do 
            if [ -e $file ]
            then
                mv $file $file.bin
                echo "Added extension .bin to file $file ..."
            fi 
        done
    fi

    # NoSnow files...
    tileNoSnowRootDir="${priorsRootDir}/$tile"
     if [ -d $tileNoSnowRootDir ]
     then
        listOfNoSnowBinFiles=`ls ${priorsRootDir}/$tile/background/processed.p1.0.618034.p2.1.00000/Kernels.*NoSnow`
        for file in $listOfNoSnowBinFiles
        do
            if [ -e $file ]
            then
                mv $file $file.bin
                echo "Added extension .bin to file $file ..."
            fi
        done
    fi
done


