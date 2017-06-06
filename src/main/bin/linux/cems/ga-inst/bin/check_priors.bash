#!/bin/bash
set -e

# check number of files: 365 doys * 3 prior files = 1095 nc files per tile
OLDDIR=$PWD
cd /group_workspaces/cems2/qa4ecv/vol3/prior.c6/stage2/1km
echo "PRIOR #tiles: `ls -d h*v* |wc`"
for tile in `ls -d h*v*`; do 
    echo "# priors $tile : `ls $tile/*/*.nc |wc`" 
done

cd $OLDDIR

