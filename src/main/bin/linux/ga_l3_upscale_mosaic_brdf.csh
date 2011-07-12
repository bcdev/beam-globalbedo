#!/bin/tcsh

set year = $1
set doy = $2
set scaling = $3      # must be either 6 (for 0.05deg) or 60 (for 0.5deg)
set gaRootDir = $4    # at BC: /bcserver12-data/GlobAlbedo
set beamRootDir = $5  # at BC:  /opt/beam-4.9.0.1

if ($doy < "10") then
    set Day = 00$doy
else if ($doy < "100") then
    set Day = 0$doy
else
    set Day = $doy
endif

time $beamRootDir/bin/gpt-ga.sh ga.l3.upscale.brdf -c 2000M -Pscaling=$scaling -Pyear=$year -Pdoy=$Day -PgaRootDir=$gaRootDir -e -t $gaRootDir/Mosaic/brdf/GlobAlbedo.$year$Day.brdf.mosaic.005.dim &
