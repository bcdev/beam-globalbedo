#!/bin/bash

# example: ./ga-l3-tile-brdf-clean.sh /group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest 2005 h18v04 121

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
        rm -Rf $gaRootDir/BBDR/AccumulatorFiles/$year/$tile/*/matrices*_$year$doy.bin    
        rm -Rf $gaRootDir/BBDR/DailyAcc/$year/$tile/*/matrices*_$year$doy.bin    
        rm -Rf $gaRootDir/BBDR/FullAcc/$year/$tile/*/matrices*_$year$doy.bin    
        rm -Rf $gaRootDir/Inversion/*/$year/$tile/*/GlobAlbedo.brdf.${year}${doy}.${tile}.*.d*
        rm -Rf $gaRootDir/Albedo/$year/$tile/GlobAlbedo.albedo.${year}${doy}.${tile}*.d*
        rm -Rf $gaRootDir/Mosaic/*/*/*/GlobAlbedo.${year}${doy}.d*
    else
	echo "deleting inversion results for year $year, tile $tile, all doys..."
        rm -Rf $gaRootDir/BBDR/AccumulatorFiles/$year/$tile/*/matrices*_$year*.bin    
        rm -Rf $gaRootDir/BBDR/DailyAcc/$year/$tile/*/matrices*_$year*.bin    
        rm -Rf $gaRootDir/BBDR/FullAcc/$year/$tile/*/matrices*_$year*.bin    
        rm -Rf $gaRootDir/Inversion/*/$year/$tile/*/GlobAlbedo.brdf.${year}*.${tile}.*.d*    
        rm -Rf $gaRootDir/Albedo/$year/$tile/GlobAlbedo.albedo.${year}*.${tile}*.d*
        rm -Rf $gaRootDir/Mosaic/*/*/*/GlobAlbedo.${year}.d*
    fi
else
    if [ -n "$doy" ]
    then
	echo "deleting inversion results for year $year, all tiles, DoY $doy..."
        rm -Rf $gaRootDir/BBDR/AccumulatorFiles/$year/*/*/matrices*_$year$doy.bin    
        rm -Rf $gaRootDir/BBDR/DailyAcc/$year/*/*/matrices*_$year$doy.bin    
        rm -Rf $gaRootDir/BBDR/FullAcc/$year/*/*/matrices*_$year$doy.bin    
        rm -Rf $gaRootDir/Inversion/*/$year/*/*/GlobAlbedo.brdf.${year}${doy}*.d*
        rm -Rf $gaRootDir/Albedo/$year/*/GlobAlbedo.albedo.${year}${doy}*.d*
        rm -Rf $gaRootDir/Mosaic/*/*/*/GlobAlbedo.${year}${doy}.d*
    else
	echo "deleting inversion results for year $year, all tiles, all doys..."
        rm -Rf $gaRootDir/BBDR/AccumulatorFiles/$year/*/*/matrices*_$year*.bin    
        rm -Rf $gaRootDir/BBDR/DailyAcc/$year/*    
        rm -Rf $gaRootDir/BBDR/FullAcc/$year/*    
        rm -Rf $gaRootDir/Inversion/*/$year/*/*    
        rm -Rf $gaRootDir/Albedo/$year/*/GlobAlbedo.albedo.${year}*.d*
        rm -Rf $gaRootDir/Mosaic/*/*/*/GlobAlbedo.${year}.d*
    fi
fi
