#!/bin/tcsh

set year = $1
set doy = $2
set deg = $3          # must be either 005, 025 or 05
set gaRootDir = $4    # e.g. at BC: /home/globalbedo/Processing/GlobAlbedo
set beamRootDir = $5  # e.g. at BC:  /opt/beam-4.11

if ($doy < "10") then
    set Day = 00$doy
else if ($doy < "100") then
    set Day = 0$doy
else
    set Day = $doy
endif

set SUCCESS = 1

set brdfMosaicProduct = $gaRootDir/Mosaic/brdf/$deg/GlobAlbedo.brdf.${year}${Day}.${deg}.dim 
set TARGET = $gaRootDir/Mosaic/albedo/$deg/GlobAlbedo.albedo.${year}${Day}.${deg}.dim 
if ( -e "$brdfMosaicProduct" ) then
    echo "time $beamRootDir/bin/gpt-d-l2.sh ga.albedo.brdfmosaic.albedomosaic -Pdoy=$Day -SbrdfMergedProduct=$brdfMosaicProduct -e -t $TARGET"
    time $beamRootDir/bin/gpt-d-l2.sh ga.albedo.brdfmosaic.albedomosaic -Pdoy=$Day -SbrdfMergedProduct=$brdfMosaicProduct -e -t $TARGET

    set SUCCESS = $status
    echo "Status: $SUCCESS"

    @ TRY = 0
    while ( $TRY < 2 && (! -e $TARGET || $SUCCESS != 0))
        # repeat up to 3 times in case product was not written
        echo "Product status unclear: $SUCCESS - reprocess..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh ga.albedo.brdfmosaic.albedomosaic -Pdoy=$Day -SbrdfMergedProduct=$brdfMosaicProduct -e -t $TARGET"
        time $beamRootDir/bin/gpt-d-l2.sh ga.albedo.brdfmosaic.albedomosaic -Pdoy=$Day -SbrdfMergedProduct=$brdfMosaicProduct -e -t $TARGET
        set SUCCESS = $status
        echo "Status: $SUCCESS"
        @ TRY += 1
    end
else
    echo Product $brdfMosaicProduct does not exist.
endif
