#!/bin/bash

# This script creates the whole Globalbedo processing directory tree

gaRootDir=$1
if [ -e ${gaRootDir} ] 
then
    echo "${gaRootDir} already exists."
else
    # run all subscripts to create subdirs
    ./make_maindirs.bash $gaRootDir
    ./make_tiledirs_albedo.bash $gaRootDir
    ./make_tiledirs_bbdr_accumulators.bash $gaRootDir
    ./make_tiledirs_bbdr_sensors.bash $gaRootDir MERIS    
    ./make_tiledirs_bbdr_sensors.bash $gaRootDir VGT    
    ./make_tiledirs_inversion.bash $gaRootDir
    ./make_tiledirs_merge.bash $gaRootDir
    ./make_tiledirs_monthlyalbedo.bash $gaRootDir
fi

