#!/bin/bash

gaRootDir=/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest
bbdrSingleDir=$gaRootDir/BBDR_single
priorDir=/group_workspaces/cems2/qa4ecv/vol1/prior.c6/stage2/1km
beamDir=/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1

echo "time $BEAM_HOME/bin/gpt.sh ga.l3.inversion.single -e -Ptile="h20v11" -Pyear=2003 -Pdoy=365 -PusePrior=true -PcomputeSnow=false -PpixelX=1025 -PpixelY=0602 -PgaRootDir=$gaRootDir -PbbdrRootDir=$bbdrSingleDir -PpriorRootDir=$priorDir -PpriorRootDirSuffix=365"
time $BEAM_HOME/bin/gpt.sh ga.l3.inversion.single -e -Ptile="h20v11" -Pyear=2003 -Pdoy=365 -PusePrior=true -PcomputeSnow=false -PpixelX=1025 -PpixelY=0602 -PgaRootDir=$gaRootDir -PbbdrRootDir=$bbdrSingleDir -PpriorRootDir=$priorDir -PpriorRootDirSuffix=365

echo `date`
