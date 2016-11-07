#!/bin/bash

# Script to count Albedo tile netcdf files generated from inversion step
# Prints day of year (1..365) and number of files per day (should be #tiles (326) in case of successful completion
# An identifying string can be put as parameter to filter ls output
# --> call: ./count_albedo_nc.sh <year> [<identifier>]
#     e.g.: ./count_albedo_nc.sh <year> 2005 "Oct 21"

year=$1
identifier=$2

echo "year: $year"
echo "identifier: $identifier"

echo "DOY #files"
for doy in {1..9}; do echo $doy `ls -l ../GlobAlbedoTest/Albedo/${year}/*/*${year}00${doy}*.nc |grep "$identifier" |wc | awk -F" " '{print $1}'`; done
for doy in {10..99}; do echo $doy `ls -l ../GlobAlbedoTest/Albedo/${year}/*/*${year}0${doy}*.nc |grep "$identifier" |wc | awk -F" " '{print $1}'`; done
for doy in {100..365}; do echo $doy `ls -l ../GlobAlbedoTest/Albedo/${year}/*/*${year}${doy}*.nc |grep "$identifier" |wc | awk -F" " '{print $1}'`; done
