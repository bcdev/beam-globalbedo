#!/bin/tcsh

set doy = $1
set scaling = $2      # must be either 5 (for 5km) or 60 (for 60km)
set gaRootDir = $3    # at BC: /bcserver12-data/GlobAlbedo
set nosnowPriorRootDir = $4    # currently at BC:  /data/Priors, but sohould be on separate disk!
set snowPriorRootDir = $5    # currently at BC:  /data/Priors, but sohould be on separate disk!
set beamRootDir = $6  # at BC:  /opt/beam-4.9.0.1

if ($doy < "10") then
    set Day = 00$doy
else if ($doy < "100") then
    set Day = 0$doy
else
    set Day = $doy
endif

if ($scaling == "5") then
    set deg = 05km
else
    set deg = 60km
endif

set SUCCESS = 1

set TARGET = $gaRootDir/Mosaic/albedo/GlobAlbedo.prior.nosnow.$Day.$deg.SIN.dim
if ( -e "$nosnowPriorRootDir" ) then
    time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.priors -Pscaling=$scaling -Pdoy=$Day -PcomputeSnow=false -PgaRootDir=$gaRootDir -PpriorStage=1 -PpriorRootDir=$nosnowPriorRootDir -e -t $TARGET
    set SUCCESS = $status
    echo "Status: $SUCCESS"
    while ( ! -e $TARGET || $SUCCESS != 0)
        # repeat in case product was not written
        echo "Product status unclear: $SUCCESS - reprocess..."
        time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.priors -Pscaling=$scaling -Pdoy=$Day -PcomputeSnow=false -PgaRootDir=$gaRootDir -PpriorStage=1 -PpriorRootDir=$nosnowPriorRootDir -e -t $TARGET
        set SUCCESS = $status
    end
endif

set TARGET = $gaRootDir/Mosaic/albedo/GlobAlbedo.prior.snow.$Day.$deg.SIN.dim
if ( -e "$snowPriorRootDir" ) then
    time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.priors -Pscaling=$scaling -Pdoy=$Day -PcomputeSnow=true -PgaRootDir=$gaRootDir -PpriorStage=1 -PpriorRootDir=$snowPriorRootDir -e -t $TARGET
    set SUCCESS = $status
    echo "Status: $SUCCESS"
    while ( ! -e $TARGET || $SUCCESS != 0)
        # repeat in case product was not written
        echo "Product status unclear: $SUCCESS - reprocess..."
        time $beamRootDir/bin/gpt-d-l3.sh ga.l3.upscale.priors -Pscaling=$scaling -Pdoy=$Day -PcomputeSnow=true -PgaRootDir=$gaRootDir -PpriorStage=1 -PpriorRootDir=$snowPriorRootDir -e -t $TARGET
        set SUCCESS = $status
    end
endif





