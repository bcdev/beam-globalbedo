#!/bin/tcsh

set tile = $1
set year = $2
set doy = $3
set gaRootDir = $4    # at BC: /bcserver12-data/GlobAlbedo
set nosnowPriorRootDir = $5    # currently at BC:  /data/Priors, but sohould be on separate disk!
set snowPriorRootDir = $6    # currently at BC:  /data/Priors, but sohould be on separate disk!
set beamRootDir = $7  # at BC:  /opt/beam-4.9.0.1

if ($doy < "10") then
    set Day = 00$doy
else if ($doy < "100") then
    set Day = 0$doy
else
    set Day = $doy
endif

if ( -e "$nosnowPriorRootDir/$tile" || -e "$snowPriorRootDir/$tile" ) then
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$Day -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorRootDir -e -t $gaRootDir/Merge/$tile/GlobAlbedo.brdf.merge.$year$doy.$tile.dim
endif
