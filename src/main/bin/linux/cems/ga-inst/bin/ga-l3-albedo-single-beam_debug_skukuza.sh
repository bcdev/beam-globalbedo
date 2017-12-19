#!/bin/bash

gaRootDir=/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest
albedoSingleDir=$gaRootDir/Albedo_single
beamDir=/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1

brdfDir=$gaRootDir/Inversion_single/2003/h20v11/NoSnow
brdfProduct=$brdfDir/Qa4ecv.brdf.single.2003.365.h20v11.1025.0602.NoSnow.csv
targetDir=$gaRootDir/Albedo_single/2003/h20v11/NoSnow
targetProduct=$targetDir/Qa4ecv.albedo.single.2003.365.h20v11.1025.0602.NoSnow.csv

if [ ! -e "$targetDir" ]
then
    mkdir -p $targetDir
fi

echo "time $BEAM_HOME/bin/gpt.sh ga.l3.albedo.single -e -Ptile="h20v11" -Pyear=2003 -Pdoy=365 -PcomputeSnow=false -SbrdfProduct=$brdfProduct -t $targetProduct"
time $BEAM_HOME/bin/gpt.sh ga.l3.albedo.single -e -Ptile="h20v11" -Pyear=2003 -Pdoy=365 -PcomputeSnow=false -SbrdfProduct=$brdfProduct -t $targetProduct

echo `date`
