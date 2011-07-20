#!/bin/tcsh

set tile = $1
set year = $2
set doy = $3
set gaRootDir = $4    # at BC: /bcserver12-data/GlobAlbedo
set priorRootDir = $5    # currently at BC: /data/GlobAlbedo, but 3TB, should be on separate disk!!
set beamRootDir = $6  # at BC:  /opt/beam-4.9.0.1

if ($doy < "10") then
    set Day = 00$doy
else if ($doy < "100") then
    set Day = 0$doy
else
    set Day = $doy
endif

time $beamRootDir/bin/gpt-ga.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$Day -PgaRootDir=$gaRootDir -PpriorRootDir=$priorRootDir -e -t $gaRootDir/Albedo/$tile/GlobAlbedo.albedo.$year$Day.$tile.dim &
