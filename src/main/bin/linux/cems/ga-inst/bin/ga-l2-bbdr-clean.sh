#!/bin/bash

# example: ./ga-l3-tile-inversion-clean.sh /group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest 2005 h18v04 121

gaRootDir=$1
sensor=$2
year=$2
month=$3

if [ ! -n "$gaRootDir" ]
then
    echo "gaRootDir not set!"
    exit 1
fi
if [ ! -n "$sensor" ]
then
    echo "sensor not set!"
    exit 1
fi
if [ ! -n "$year" ]
then
    echo "year not set!"
    exit 1
fi

if [ -n "$month" ]
then
    # delete specific month
    echo "deleting BBDR intermediate results for sensor: $sensor, year: $year, month: $month..."
    rm -Rf $gaRootDir/BBDR_L2/$sensor/$year/$month
    echo "deleting BBDR tile results for sensor: $sensor, year: $year, month: $month..."
    rm -Rf $gaRootDir/BBDR/$sensor/$year/$month/*
else
    # delete all months in given year
    echo "deleting BBDR intermediate results for sensor: $sensor, year: $year..."
    rm -Rf $gaRootDir/BBDR_L2/$sensor/$year/*
    
fi
