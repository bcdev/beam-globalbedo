#!/bin/bash

tile=$1
doy=$2
scaleFactor=$3
gaRootDir=$4
priorDir=$5
beamRootDir=$6

preprocessedPriorDir=$gaRootDir/Priors/200x200/$tile/$doy

if [ ! -e "$preprocessedPriorDir" ]
then
    mkdir -p $preprocessedPriorDir
fi

echo "Preprocess MODIS Priors for AVHRRGEO..."

priorNosnowInputProduct=$priorDir/$tile/$doy/prior.modis.c6.${doy}.${tile}.nosnow.stage2.nc
priorSnowInputProduct=$priorDir/$tile/$doy/prior.modis.c6.${doy}.${tile}.snow.stage2.nc

nosnowTargetProduct=$preprocessedPriorDir/prior.modis.c6.${doy}.${tile}.nosnow.stage2.nc
echo "time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.modis.prior.preprocess -e -c 3000M -SpriorProduct=$priorNosnowInputProduct -Ptile=$tile -Pdoy=$doy -PcomputeSnow=false -PscaleFactor=$scaleFactor -f NetCDF-BEAM -t $nosnowTargetProduct"
time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.modis.prior.preprocess -e -c 3000M -SpriorProduct=$priorNosnowInputProduct -Ptile=$tile -Pdoy=$doy -PcomputeSnow=false -PscaleFactor=$scaleFactor -f NetCDF-BEAM -t $nosnowTargetProduct

snowTargetProduct=$preprocessedPriorDir/prior.modis.c6.${doy}.${tile}.snow.stage2.nc
echo "time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.modis.prior.preprocess -e -c 3000M -SpriorProduct=$priorSnowInputProduct -Ptile=$tile -Pdoy=$doy -PcomputeSnow=true -PscaleFactor=$scaleFactor -f NetCDF-BEAM -t $snowTargetProduct"
time $beamRootDir/bin/gpt-d-l1b-bbdr.sh ga.modis.prior.preprocess -e -c 3000M -SpriorProduct=$priorSnowInputProduct -Ptile=$tile -Pdoy=$doy -PcomputeSnow=true -PscaleFactor=$scaleFactor -f NetCDF-BEAM -t $snowTargetProduct


status=$?
echo "Status: $status"

echo `date`
