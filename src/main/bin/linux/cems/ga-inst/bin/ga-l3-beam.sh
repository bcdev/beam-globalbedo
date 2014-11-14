#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
snow=$5
gaRootDir=$6
priorRootDir=$7
snowPriorRootDir=$8  # explicitly needed in merge step
beamRootDir=$9

bbdrRootDir=$gaRootDir/BBDR
inversionDir=$gaRootDir/Inversion

if [ $snow eq "true" ]
then
    snowId=Snow
else
    snowId=NoSnow
fi
snowAccDir=$gaRootDir/BBDR/AccumulatorFiles/$year/$tile/$snowId

wings=90  # tbd later

if [ -e "$priorRootDir/$tile" ] 
then
    for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
        echo "DoY $doy..."

	echo "Create daily accumulators for tile $tile, year $year, DoY $doy, $snowId ..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=$snow -PbbdrRootDir=$bbdrRootDir -e"
        time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=$snow -PbbdrRootDir=$bbdrRootDir -e
    done

    echo "Create full accumulator for tile $tile, year $year, DoY $doy, $snowId ..."
    echo "time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -Pwings=$wings -PcomputeSnow=$snow -PgaRootDir=$gaRootDir -e"
    time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -Pwings=$wings -PcomputeSnow=$snow -PgaRootDir=$gaRootDir -e

    doy=$start

    echo "Compute BRDF for tile $tile, year $year, DoY $doy, $snowId ..."
    echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=$wings -PcomputeSnow=$snow -PgaRootDir=$gaRootDir -PpriorRootDir=$priorRootDir -e -t ${inversionDir}/${tile}/GlobAlbedo.brdf.$year$doy.$tile.$snowId.dim"
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=$wings -PcomputeSnow=$snow -PgaRootDir=$gaRootDir -PpriorRootDir=$priorRootDir -e -t ${inversionDir}/${tile}/GlobAlbedo.brdf.$year$doy.$tile.$snowId.dim

    echo "Compute MERGED BRDF for tile $tile, year $year, DoY $doy ..."
    echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorRootDir -e -t $gaRootDir/Merge/$tile/GlobAlbedo.brdf.merge.$year$doy.$tile.dim"
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorRootDir -e -t $gaRootDir/Merge/$tile/GlobAlbedo.brdf.merge.$year$doy.$tile.dim

    echo "Compute ALBEDO for tile $tile, year $year, DoY $doy ..."
    echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -PpriorRootDir=snowPriorRootDir -e -t $gaRootDir/Albedo/$tile/GlobAlbedo.albedo.$year$doy.$tile.dim"
    time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -PpriorRootDir=snowPriorRootDir -e -t $gaRootDir/Albedo/$tile/GlobAlbedo.albedo.$year$doy.$tile.dim

    rm -Rf $PWD/target.*
fi

echo `date`
