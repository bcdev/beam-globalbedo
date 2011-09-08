#!/bin/bash

# This scripts adds extension ".bin" to all prior files ending with "*Snow" and "*NoSnow".

gaRootDir=$1

# Read all tile names...
awk -F"," '{print $1,$2,$3}' Tiles_UpperLeftCorner_Coordinates.txt | while read tile ulx uly
do
     # Snow files...
     tileSnowRootDir="${gaRootDir}/Priors/PriorStage2Snow/$tile"
     if [ -d $tileSnowRootDir ]
     then
        listOfSnowBinFiles=`ls ${gaRootDir}/Priors/PriorStage2Snow/$tile/background/processed.p1.0.618034.p2.1.00000/Kernels.*Snow` 
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
    tileNoSnowRootDir="${gaRootDir}/Priors/PriorStage2/$tile"
     if [ -d $tileNoSnowRootDir ]
     then
        listOfNoSnowBinFiles=`ls ${gaRootDir}/Priors/PriorStage2/$tile/background/processed.p1.0.618034.p2.1.00000/Kernels.*NoSnow`
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


