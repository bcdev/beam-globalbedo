#!/bin/tcsh

set tile = $1
set year = $2
set doy = $3
set wings = $4
set gaRootDir = $5    # at BC:  /bcserver12-data/GlobAlbedo
set nosnowPriorRootDir = $6    # currently at BC:  /data/Priors, but sohould be on separate disk!
set snowPriorRootDir = $7    # currently at BC:  /data/Priors, but sohould be on separate disk!
set beamRootDir = $8  # at BC:  /opt/beam-4.9.0.1

if ($doy < "10") then
    set Day = 00$doy
else if ($doy < "100") then
    set Day = 0$doy
else
    set Day = $doy
endif

if ( -e "$nosnowPriorRootDir/$tile" ) then
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$Day -Pwings=$wings -PcomputeSnow=false -PgaRootDir=$gaRootDir -PpriorRootDir=$nosnowPriorRootDir -e -t $gaRootDir/Inversion/$tile/GlobAlbedo.brdf.$year$Day.$tile.NoSnow.dim
endif
if ( -e "$snowPriorRootDir/$tile" ) then
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$Day -Pwings=$wings -PcomputeSnow=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorRootDir -e -t $gaRootDir/Inversion/$tile/GlobAlbedo.brdf.$year$Day.$tile.Snow.dim
endif
