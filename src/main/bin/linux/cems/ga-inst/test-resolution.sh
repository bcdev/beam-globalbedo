#!/bin/bash

echo "entered test-resolution..."

year=$1
doy=$2
deg=$3

doy=`printf '%03d\n' "$((10#$doy))"`

echo "year: $year"
echo "doy: $doy"
echo "deg: '${deg}'"

if [ "$deg" == "005" ]
then
    echo "hier 1"
    scaling=6
elif [ "$deg" == "025" ]
then
    echo "hier 2"
    scaling=30
else
    echo "hier 3"
    scaling=60
fi
