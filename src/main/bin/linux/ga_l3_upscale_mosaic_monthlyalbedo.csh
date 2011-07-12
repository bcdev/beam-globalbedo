#!/bin/tcsh

set year = $1
set monthIndex = $2   # must be in interval [1,12]
set scaling = $3      # must be either 6 (for 0.05deg) or 60 (for 0.5deg)
set gaRootDir = $4    # at BC: /bcserver12-data/GlobAlbedo
set beamRootDir = $5  # at BC:  /opt/beam-4.9.0.1

if ($monthIndex < "10") then
    set Month = 0$monthIndex
else
    set Month = $monthIndex
endif

if ($scaling == "6") then
    set deg = 005
else
    set deg = 05
endif

time $beamRootDir/bin/gpt-ga.sh ga.l3.upscale.albedo -c 2000M -Pscaling=$scaling -Pyear=$year -PmonthIndex=$monthIndex -PisMonthlyAlbedo=true -PgaRootDir=$gaRootDir -e -t $gaRootDir/Mosaic/albedo/GlobAlbedo.$year$Month.albedo.mosaic.$deg.dim &
