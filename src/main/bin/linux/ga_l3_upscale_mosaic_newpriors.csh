#!/bin/tcsh

# example call:
# ./ga_l3_upscale_mosaic_newpriors.csh 2005 001 60 Snow /data/globalbedo/cems/priors /data/globalbedo/cems/mosaics /opt/beam-4.11

# valid prior stage3 input files would be e.g. 
# /data/globalbedo/cems/priors/background/processed/Kernels.001.005.h18v04.background.NoSnow.nc
# /data/globalbedo/cems/priors/background/processed/Kernels.001.005.h18v04.background.Snow.nc
# /data/globalbedo/cems/priors/background/processed/Kernels.001.005.h18v04.background.SnowAndNoSnow.nc

#input parameters:
set year = $1
set doy = $2           # 001, 009, 017, ...
set scaling = $3       # must be 6 (for 0.05deg) or 60 (for 0.5deg)
set snowMode = $4      # must be 'Snow', 'NoSnow' or 'SnowAndNoSnow'
set priorRootDir = $5  # e.g. /data/globalbedo/priors/cems 
set mosaicDir = $6     # output, e.g. /data/globalbedo/priors/cems/mosaics
set beamRootDir = $7   # e.g. /opt/beam-4.11

echo `date`
echo "time $beamRootDir/bin/gpt_8192.sh ga.l3.upscale.priors.new -c 3000M -PpriorRootDir=$priorRootDir -Pscaling=$scaling -Pyear=$year -Pdoy=$doy -PisPriors=true -PsnowMode=$snowMode -PpriorStage=3 -e -t $mosaicDir/Cems.prior.3.${year}.${doy}.mosaic.dim"
time $beamRootDir/bin/gpt_8192.sh ga.l3.upscale.priors.new -c 3000M -PpriorRootDir=$priorRootDir -Pscaling=$scaling -Pyear=$year -Pdoy=$doy -PisPriors=true -PsnowMode=$snowMode -PpriorStage=3 -e -t $mosaicDir/Cems.prior.3.${year}.${doy}.mosaic.dim
echo "done."
