#!/bin/bash

l1bSinglePath=$1
l1bBaseName=$2
bbdrSingleDir=$3
sensor=$4
year=$5
month=$6
gaRootDir=$7
beamRootDir=$8

bbdrFile=$bbdrL2Dir/${l1bBaseName}_BBDR.dim

if [ ! -e "$bbdrSingleDir" ]
then
    mkdir -p $bbdrSingleDir
fi

echo "time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.l2.single -e -c 8000M -q 24 -Psensor=$sensor -t $bbdrFile $l1bPath"
time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.l2 -e -c 8000M -q 24 -Psensor=$sensor -t $bbdrFile $l1bPath

echo `date`
