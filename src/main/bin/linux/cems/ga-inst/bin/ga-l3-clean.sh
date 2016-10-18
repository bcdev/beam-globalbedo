#!/bin/bash

# example: ./ga-l3-clean.sh /group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest 2005 h18v04 121

gaRootDir=$1
year=$2
tile=$3
doy=$4

if [ ! -n "$gaRootDir" ]
then
    echo "gaRootDir not set!"
    exit 1
fi
if [ ! -n "$year" ]
then
    echo "year not set!"
    exit 1
fi

if [ -n "$tile" ]
then
    if [ -n "$doy" ]
    then
	echo "deleting inversion results for year $year, tile $tile, DoY $doy..."
        rm -Rf $gaRootDir/BBDR/DailyAcc/$year/$tile/*/matrices*_$year$doy.bin    
        rm -Rf $gaRootDir/Inversion/*/$year/$tile/*/GlobAlbedo.brdf.${year}${doy}.${tile}.*.*
        rm -Rf $gaRootDir/Albedo/$year/$tile/GlobAlbedo.albedo.${year}${doy}.${tile}*.*
        #rm -Rf $gaRootDir/Mosaic/*/*/*/GlobAlbedo.${year}${doy}*.*
    else
	echo "deleting inversion results for year $year, tile $tile, all doys..."
        rm -Rf $gaRootDir/BBDR/DailyAcc/$year/$tile/*/matrices*_$year*.bin    
        rm -Rf $gaRootDir/Inversion/*/$year/$tile/*/GlobAlbedo.brdf.${year}*.${tile}.*.*    
        rm -Rf $gaRootDir/Albedo/$year/$tile/GlobAlbedo.albedo.${year}*.${tile}*.*
        #rm -Rf $gaRootDir/Mosaic/*/*/*/GlobAlbedo.${year}*.*
    fi
else
    if [ -n "$doy" ]
    then
	echo "deleting inversion results for year $year, all tiles, DoY $doy..."
        rm -Rf $gaRootDir/BBDR/DailyAcc/$year/*/*/matrices*_$year$doy.bin    
        rm -Rf $gaRootDir/Inversion/*/$year/*/*/GlobAlbedo.brdf.${year}${doy}*.*
        rm -Rf $gaRootDir/Albedo/$year/*/GlobAlbedo.albedo.${year}${doy}*.*
        #rm -Rf $gaRootDir/Mosaic/*/*/*/GlobAlbedo.${year}${doy}*.*
    else
	echo "deleting inversion results for year $year, all tiles, all doys..."
        rm -Rf $gaRootDir/BBDR/DailyAcc/$year/*    

        for tileDir in `ls $gaRootDir/Inversion/Merge/$year/$tile`;
        do
            rm -Rf $tileDir/*
            rmdir $tileDir
        done 
        for tileDir in `ls $gaRootDir/Inversion/Snow/$year/$tile`;
        do
            rm -Rf $tileDir/*
            rmdir $tileDir
        done
        for tileDir in `ls $gaRootDir/Inversion/NoSnow/$year/$tile`;
        do
            rm -Rf $tileDir/*
            rmdir $tileDir
        done
        for tileDir in `ls $gaRootDir/Albedo/$year/$tile`;
        do
            rm -Rf $tileDir/*
            rmdir $tileDir
        done

        #rm -Rf $gaRootDir/Mosaic/*/*/*/GlobAlbedo.${year}*.*
    fi
fi
