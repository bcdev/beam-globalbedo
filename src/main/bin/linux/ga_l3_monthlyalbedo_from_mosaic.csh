#!/bin/tcsh

set year = $1
set monthIndex = $2  # must be in [1,12]
set deg = $3  # must be 005, 025 or 05
set gaRootDir = $4    # e.g. at BC: /home/globalbedo/Processing/GlobAlbedo
set beamRootDir = $5  # e.g. at BC:  /opt/beam-4.11

if ($monthIndex < "10") then
    set Month = 0$monthIndex
else
    set Month = $monthIndex
endif

set SUCCESS = 1

set TARGET = $gaRootDir/Mosaic/albedo/${deg}/Globalbedo.albedo.$year$Month.$deg.dim
echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.albedo.monthly -c 3000M -PgaRootDir=$gaRootDir -Pyear=$year -PisMosaicAlbedo=true -PmosaicScaling=$deg -PmonthIndex=$monthIndex -e -t $TARGET"
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.albedo.monthly -c 3000M -PgaRootDir=$gaRootDir -Pyear=$year -PisMosaicAlbedo=true -PmosaicScaling=$deg -PmonthIndex=$monthIndex -e -t $TARGET

set SUCCESS = $status
echo "Status: $SUCCESS"

@ TRY = 0
while ( $TRY < 2 && (! -e $TARGET || $SUCCESS != 0))
    # repeat up to 3 times in case product was not written
    echo "Product status unclear: $SUCCESS - reprocess..."
    echo "time $beamRootDir/bin/gpt-d-l3.sh ga.l3.albedo.monthly -c 3000M -PgaRootDir=$gaRootDir -Pyear=$year -PisMosaicAlbedo=true -PmosaicScaling=$deg -PmonthIndex=$monthIndex -e -t $TARGET"
    time $beamRootDir/bin/gpt-d-l3.sh ga.l3.albedo.monthly -c 3000M -PgaRootDir=$gaRootDir -Pyear=$year -PisMosaicAlbedo=true -PmosaicScaling=$deg -PmonthIndex=$monthIndex -e -t $TARGET
    set SUCCESS = $status
    echo "Status: $SUCCESS"
    @ TRY += 1
end


