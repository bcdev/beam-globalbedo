#!/bin/tcsh

#input parameters:
set tile = $1
set year = $2
set startDoy = $3
set endDoy = $4
set wings = $5
set gaRootDir = $6    # at BC:  /bcserver12-data/GlobAlbedo
set nosnowPriorRootDir = $7    # currently at BC:  /data/Priors, but sohould be on separate disk!
set snowPriorRootDir = $8    # currently at BC:  /data/Priors, but sohould be on separate disk!
set beamRootDir = $9  # at BC:  /opt/beam-4.9.0.1

echo "StartDoY $startDoy..."
if ($startDoy < "10") then
    set StartDay = 00$startDoy
else if ($startDoy < "100") then
    set StartDay = 0$startDoy
else
    set StartDay = $startDoy
endif
echo "EndDoY $endDoy..."
if ($endDoy < "10") then
    set EndDay = 00$endDoy
else if ($endDoy < "100") then
    set EndDay = 0$endDoy
else
    set EndDay = $endDoy
endif

echo "Create accumulators for tile $tile, year $year, DoYs from $StartDay to $EndDay..."
if ( -e "$nosnowPriorRootDir/$tile" ) then
    time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$StartDay -PendDoy=$EndDay -Pwings=$wings -PcomputeSnow=false -PgaRootDir=$gaRootDir -e
endif
if ( -e "$snowPriorRootDir/$tile" ) then
    time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$StartDay -PendDoy=$EndDay -Pwings=$wings -PcomputeSnow=true -PgaRootDir=$gaRootDir -e
endif

echo `date`















