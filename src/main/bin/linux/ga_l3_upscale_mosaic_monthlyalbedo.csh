#!/bin/tcsh

set year = $1
set monthIndex = $2
set scaling = $3      # must be either 5 (for 5km) or 60 (for 60km)
set gaRootDir = $4    # at BC: /bcserver12-data/GlobAlbedo
set beamRootDir = $5  # at BC:  /opt/beam-4.9.0.1

if ($monthIndex < "10") then
    set Month = 0$monthIndex
else
    set Month = $monthIndex
endif

if ($scaling == "5") then
    set deg = 05km
else
    set deg = 60km
endif

set SUCCESS = 1

set TARGET = $gaRootDir/Mosaic/albedo/GlobAlbedo.albedo.$year$Month.$deg.PC.dim
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo -c 3000M -Pscaling=$scaling -Pyear=$year -PmonthIndex=$monthIndex -PisMonthlyAlbedo=true -PgaRootDir=$gaRootDir -e -t $TARGET
set SUCCESS = $status
echo "Status: $SUCCESS"
while ( ! -e $TARGET || $SUCCESS != 0)
    # repeat in case product was not written
    echo "Product status unclear: $SUCCESS - reprocess..."
    time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo -c 3000M -Pscaling=$scaling -Pyear=$year -PmonthIndex=$monthIndex -PisMonthlyAlbedo=true -PgaRootDir=$gaRootDir -e -t $TARGET
    set SUCCESS = $status
end

set TARGET = $gaRootDir/Mosaic/albedo/GlobAlbedo.albedo.$year$Month.$deg.SIN.dim
time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo -c 3000M -Pscaling=$scaling -Pyear=$year -PmonthIndex=$monthIndex -PisMonthlyAlbedo=true -PgaRootDir=$gaRootDir  -PreprojectToPlateCarre=false -e -t $TARGET
set SUCCESS = $status
echo "Status: $SUCCESS"
while ( ! -e $TARGET || $SUCCESS != 0)
    # repeat in case product was not written
    echo "Product status unclear: $SUCCESS - reprocess..."
    time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.albedo -c 3000M -Pscaling=$scaling -Pyear=$year -PmonthIndex=$monthIndex -PisMonthlyAlbedo=true -PgaRootDir=$gaRootDir  -PreprojectToPlateCarre=false -e -t $TARGET
    set SUCCESS = $status
end


