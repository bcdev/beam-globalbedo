#!/bin/tcsh

set tile = $1
set year = $2
set monthIndex = $3
set gaRootDir = $4    # at BC: /bcserver12-data/GlobAlbedo
set beamRootDir = $5  # at BC:  /opt/beam-4.9.0.1

if ($monthIndex < "10") then
    set Month = 0$monthIndex
else
    set Month = $monthIndex
endif

time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo.monthly -Ptile=$tile -Pyear=$year -PmonthIndex=$monthIndex -PgaRootDir=$gaRootDir -e -t $gaRootDir/MonthlyAlbedo/$tile/GlobAlbedo.albedo.$year$Month.$tile.dim &
