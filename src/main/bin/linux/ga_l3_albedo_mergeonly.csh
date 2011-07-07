#!/bin/tcsh

set tile = $1
set year = $2
set doy = $3
set gaRootDir = $4    # at BC: /bcserver12-data/GlobAlbedo
set beamRootDir = $5  # at BC:  /opt/beam-4.9.0.1

if ($doy < "10") then
    set Day = 00$doy
else if ($doy < "100") then
    set Day = 0$doy
else
    set Day = $doy
endif

time $beamRootDir/bin/gpt-d.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$Day -PmergedProductOnly=true -PgaRootDir=$gaRootDir -e -t $gaRootDir/Merge/$tile/GlobAlbedo.Merge.$year$doy.$tile.dim
