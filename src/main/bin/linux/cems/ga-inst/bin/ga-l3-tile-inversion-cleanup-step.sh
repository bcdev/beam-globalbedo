#!/bin/bash

tile=$1
year=$2
gaRootDir=$3

dailyAccNosnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow

prevYear=`printf '%04d\n' "$((10#$year - 1))"`
dailyAccPrevNosnowDir=$gaRootDir/BBDR/DailyAcc/$prevYear/$tile/NoSnow
dailyAccPrevSnowDir=$gaRootDir/BBDR/DailyAcc/$prevYear/$tile/Snow
nextYear=`printf '%04d\n' "$((10#$year + 1))"`
dailyAccNextNosnowDir=$gaRootDir/BBDR/DailyAcc/$nextYear/$tile/NoSnow
dailyAccNextSnowDir=$gaRootDir/BBDR/DailyAcc/$nextYear/$tile/Snow

fullAccNosnowDir=$gaRootDir/BBDR/FullAcc/$year/$tile/NoSnow
fullAccSnowDir=$gaRootDir/BBDR/FullAcc/$year/$tile/Snow

# cleanup
rm -Rf $dailyAccSnowDir/*
rm -Rf $dailyAccNosnowDir/*
rm -Rf $dailyAccPrevSnowDir/*
rm -Rf $dailyAccPrevNosnowDir/*
rm -Rf $dailyAccNextSnowDir/*
rm -Rf $dailyAccNextNosnowDir/*
rm -Rf $fullAccSnowDir/*
rm -Rf $fullAccNosnowDir/*

echo `date`
