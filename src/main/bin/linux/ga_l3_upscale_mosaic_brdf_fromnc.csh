#!/bin/tcsh

set year = $1
set doy = $2
set scaling = $3      # must be either 6 (for 0.05deg), 30 (for 0.25deg) or 60 (for 0.5deg)
set gaRootDir = $4    # e.g. at BC: /home/globalbedo/Processing/GlobAlbedo
set beamRootDir = $5  # e.g. at BC:  /opt/beam-4.11

if ($doy < "10") then
    set Day = 00$doy
else if ($doy < "100") then
    set Day = 0$doy
else
    set Day = $doy
endif

if ($scaling == "6") then
    set deg = 005
else if ($scaling == "30") then
    set deg = 025
else
    set deg = 05
endif

set SUCCESS = 1

set TARGET = $gaRootDir/Mosaic/brdf/$deg/GlobAlbedo.brdf.$year$Day.$deg.dim
echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -Pyear=$year -Pdoy=$Day -PgaRootDir=$gaRootDir -e -t $TARGET"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -Pyear=$year -Pdoy=$Day -PgaRootDir=$gaRootDir -e -t $TARGET

set SUCCESS = $status
echo "Status: $SUCCESS"

@ TRY = 0
while ( $TRY < 2 && (! -e $TARGET || $SUCCESS != 0))
    # repeat up to 3 times in case product was not written
    echo "Product status unclear: $SUCCESS - reprocess..."
    time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.brdf -c 3000M -PinputFormat=NETCDF -Pscaling=$scaling -Pyear=$year -Pdoy=$Day -PgaRootDir=$gaRootDir -e -t $TARGET
    set SUCCESS = $status
    echo "Status: $SUCCESS"
    @ TRY += 1
end

