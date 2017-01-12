#!/bin/bash

gaRootDir=/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest
bbdrSingleDir=$gaRootDir/BBDR_single
priorDir=/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km
beamDir=/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1
priorPixelProduct=$GA_INST/from_bc/subset_1_of_kernel.121.005.h18v03.NoSnow.csv

echo "time $BEAM_HOME/bin/gpt-d-l2.sh ga.l3.inversion.single -e -SpriorPixelProduct=$priorPixelProduct -Ptile="h18v03" -Pyear=2006 -Pdoy=121 -PusePrior=true -PcomputeSnow=false -PpixelX=0756 -PpixelY=0787 -PgaRootDir=$gaRootDir -PbbdrRootDir=$bbdrSingleDir -PpriorRootDir=$priorDir -PpriorRootDirSuffix=121"
time $BEAM_HOME/bin/gpt-d-l2.sh ga.l3.inversion.single -e -SpriorPixelProduct=$priorPixelProduct -Ptile="h18v03" -Pyear=2006 -Pdoy=121 -PusePrior=true -PcomputeSnow=false -PpixelX=0756 -PpixelY=0787 -PgaRootDir=$gaRootDir -PbbdrRootDir=$bbdrSingleDir -PpriorRootDir=$priorDir -PpriorRootDirSuffix=121

echo `date`
