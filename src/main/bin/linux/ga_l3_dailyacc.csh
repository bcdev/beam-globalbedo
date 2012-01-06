#!/bin/tcsh

#input parameters:
set tile = $1
set year = $2
set start = $3
set end = $4
set gaRootDir = $5    # at BC:  /bcserver12-data/GlobAlbedo
set nosnowPriorRootDir = $6    # currently at BC:  /data/Priors, but sohould be on separate disk!
set snowPriorRootDir = $7    # currently at BC:  /data/Priors, but sohould be on separate disk!
set beamRootDir = $8  # at BC:  /opt/beam-4.9.0.1

set doy = $start
set index = 0

while ( $doy <= $end )
	echo "DoY $doy..."
	if ($doy < "10") then
            set Day = 00$doy
        else if ($doy < "100") then
            set Day = 0$doy
        else
            set Day = $doy
        endif
        echo "Create accumulators for tile $tile, year $year, DoY $Day..."
        if ( -e "$nosnowPriorRootDir/$tile" ) then
            time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$Day -PcomputeSnow=false -PbbdrRootDir="$gaRootDir/BBDR" -e -t $gaRootDir/BBDR/AccumulatorFiles/$year/$tile/tileInfo_$index.dim
        endif
        @ index = $index + 1
        if ( -e "$snowPriorRootDir/$tile" ) then
            time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$Day -PcomputeSnow=true -PbbdrRootDir="$gaRootDir/BBDR" -e -t $gaRootDir/BBDR/AccumulatorFiles/$year/$tile/tileInfo_$index.dim
        endif
	@ index = $index + 1
	@ doy = $doy + 1
end

echo `date`















