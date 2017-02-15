#!/bin/bash

gaRootDir=/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest
albedoSingleDir=/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest/Albedo_single
priorDir=/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km
beamDir=/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1

brdfDir=$gaRootDir/Inversion_single/2006/h18v03/NoSnow
brdfProduct=$brdfDir/Qa4ecv.brdf.single.2006.121.h18v03.0756.0787.NoSnow.csv
targetDir=$gaRootDir/Albedo_single/2006/h18v03/NoSnow
targetProduct=$targetDir/Qa4ecv.albedo.single.2006.121.h18v03.0756.0787.NoSnow.csv

if [ ! -e "$targetDir" ]
then
    mkdir -p $targetDir
fi

echo "time $BEAM_HOME/bin/gpt-d-l2.sh ga.l3.albedo.single -e -Ptile="h18v03" -Pyear=2006 -Pdoy=121 -PcomputeSnow=false -SbrdfProduct=$brdfProduct -t $targetProduct"
time $BEAM_HOME/bin/gpt-d-l2.sh ga.l3.albedo.single -e -Ptile="h18v03" -Pyear=2006 -Pdoy=121 -PcomputeSnow=false -SbrdfProduct=$brdfProduct -t $targetProduct

echo `date`
