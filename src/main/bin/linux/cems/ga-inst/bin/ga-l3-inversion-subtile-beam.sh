#!/bin/bash

tile=$1
year=$2
doy=$3
startX=$4
startY=$5
endX=$6
endY=$7
gaRootDir=$8
bbdrRootDir=$9
priorDir=${10}
beamDir=${11}
targetDir=${12}

if [ ! -e "$targetDir" ]
then
    mkdir -p $targetDir
fi

targetProduct=$targetDir/Globalbedo.brdf.${year}${doy}.${tile}.${startX}.${startY}.${endX}.${endY}.subtile.nc

echo "time $BEAM_HOME/bin/gpt-d-l2.sh ga.l3.inversion.subtile -e -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false \
-PstartX=$startX -PstartY=$startY -PendX=$endX -PendY=$endY \
-PgaRootDir=$gaRootDir -PbbdrRootDir=$bbdrRootDir -PpriorRootDir=$priorDir -PpriorRootDirSuffix=$doy \
-f NetCDF4-BEAM -t $targetProduct"

time $BEAM_HOME/bin/gpt-d-l2.sh ga.l3.inversion.subtile -e -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false \
-PstartX=$startX -PstartY=$startY -PendX=$endX -PendY=$endY \
-PgaRootDir=$gaRootDir -PbbdrRootDir=$bbdrRootDir -PpriorRootDir=$priorDir -PpriorRootDirSuffix=$doy \
-f NetCDF4-BEAM -t $targetProduct

echo `date`
