#!/bin/bash

tile=$1
year=$2
start=$3
end=$4
gaRootDir=$5
priorRootDir=$6
beamRootDir=$7
albedoTargetDir=${8}  # remind the brackets if >= 10!!

if [ ! -d "$albedoTargetDir" ]
then
   mkdir -p $albedoTargetDir
fi

inversionNosnowTargetDir=$gaRootDir/Inversion/NoSnow/$year/$tile
inversionSnowTargetDir=$gaRootDir/Inversion/Snow/$year/$tile
inversionMergeTargetDir=$gaRootDir/Inversion/Merge/$year/$tile
if [ ! -d "$inversionNosnowTargetDir" ]
then
   mkdir -p $inversionNosnowTargetDir
fi
if [ ! -d "$inversionSnowTargetDir" ]
then
   mkdir -p $inversionSnowTargetDir
fi
if [ ! -d "$inversionMergeTargetDir" ]
then
   mkdir -p $inversionMergeTargetDir
fi


if [ ! -d "$albedoTargetDir" ]
then
   mkdir -p $albedoTargetDir
fi

bbdrRootDir=$gaRootDir/BBDR

noSnowPriorDir=$priorRootDir/NoSnow
snowPriorDir=$priorRootDir/Snow

dailyAccNosnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/NoSnow
dailyAccSnowDir=$gaRootDir/BBDR/DailyAcc/$year/$tile/Snow
if [ ! -d "$dailyAccNosnowDir" ]
then 
   mkdir -p $dailyAccNosnowDir
fi
if [ ! -d "$dailyAccSnowDir" ]
then
   mkdir -p $dailyAccSnowDir
fi

fullAccNosnowDir=$gaRootDir/BBDR/FullAcc/$year/$tile/NoSnow
fullAccSnowDir=$gaRootDir/BBDR/FullAcc/$year/$tile/Snow
if [ ! -d "$fullAccNosnowDir" ]
then
   mkdir -p $fullAccNosnowDir
fi
if [ ! -d "$fullAccSnowDir" ]
then
   mkdir -p $fullAccSnowDir
fi

wings=90  # tbd later

echo "BRDF computation for prior: '$priorRootDir', tile: '$tile' , year $year, DoY $start ..."
if [ -d "$snowPriorDir/$tile" ] 
then
    for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
        echo "DoY $doy..."

	echo "Create NOSNOW daily accumulators for tile $tile, year $year, DoY $doy..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $dailyAccNosnowDir/SUCCESS_dailyacc_${year}_$doy.dim"
        time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=false -PbbdrRootDir=$bbdrRootDir -e -t $dailyAccNosnowDir/SUCCESS_dailyacc_${year}_$doy.dim
        status=$?
        echo "Status: $status"
        if [ "$status" -ne 0 ]; then
            break
        fi

        echo "Create SNOW daily accumulators for tile $tile, year $year, DoY $doy..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $dailyAccSnowDir/SUCCESS_dailyacc_${year}_$doy.dim"
        time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.dailyacc -Ptile=$tile -Pyear=$year -Pdoy=$doy -PcomputeSnow=true -PbbdrRootDir=$bbdrRootDir -e -t $dailyAccSnowDir/SUCCESS_dailyacc_${year}_$doy.dim
        status=$?
        echo "Status: $status"
        if [ "$status" -ne 0 ]; then
            break
        fi

    done

    if [ "$status" -eq 0 ]; then
        echo "Create NOSNOW full accumulator for tile $tile, year $year, DoY $doy ..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -Pwings=$wings -PcomputeSnow=false -PgaRootDir=$gaRootDir -e -t $fullAccNosnowDir/SUCCESS_fullacc_${year}_$start.dim"
        time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -Pwings=$wings -PcomputeSnow=false -PgaRootDir=$gaRootDir -e -t $fullAccNosnowDir/SUCCESS_fullacc_${year}_$start.dim
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        echo "Create SNOW full accumulator for tile $tile, year $year, DoY $doy ..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -Pwings=$wings -PcomputeSnow=true -PgaRootDir=$gaRootDir -e -t $fullAccSnowDir/SUCCESS_fullacc_${year}_$start.dim"
        time $beamRootDir/bin/gpt-d-l2.sh  ga.l3.fullacc -Ptile=$tile -Pyear=$year -PstartDoy=$start -PendDoy=$end -Pwings=$wings -PcomputeSnow=true -PgaRootDir=$gaRootDir -e -t $fullAccSnowDir/SUCCESS_fullacc_${year}_$start.dim
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        doy=$start
        echo "Compute NOSNOW BRDF for tile $tile, year $year, DoY $doy, ..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=$wings -PcomputeSnow=false -PgaRootDir=$gaRootDir -PpriorRootDir=$noSnowPriorDir -e -t ${inversionNosnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.NoSnow.dim"
        time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=$wings -PcomputeSnow=false -PgaRootDir=$gaRootDir -PpriorRootDir=$noSnowPriorDir -e -t ${inversionNosnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.NoSnow.dim
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        doy=$start
        echo "Compute SNOW BRDF for tile $tile, year $year, DoY $doy, ..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=$wings -PcomputeSnow=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -t ${inversionSnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.Snow.dim"
        time $beamRootDir/bin/gpt-d-l2.sh ga.l3.inversion -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=$wings -PcomputeSnow=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -t ${inversionSnowTargetDir}/GlobAlbedo.brdf.$year$doy.$tile.Snow.dim
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        echo "Compute MERGED BRDF for tile $tile, year $year, DoY $doy ..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -f NetCDF4-BEAM -t ${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.nc"
        time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PmergedProductOnly=true -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -f NetCDF4-BEAM -t ${inversionMergeTargetDir}/GlobAlbedo.brdf.merge.$year$doy.$tile.nc
        status=$?
        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
        echo "Compute ALBEDO for tile $tile, year $year, DoY $doy ..."
        echo "time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -t $albedoTargetDir/GlobAlbedo.albedo.$year$doy.$tile.dim"
        time $beamRootDir/bin/gpt-d-l2.sh ga.l3.albedo -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -PpriorRootDir=$snowPriorDir -e -t $albedoTargetDir/GlobAlbedo.albedo.$year$doy.$tile.dim
        status=$?
        echo "Status: $status"
    fi

    # cleanup
    for doy in $(seq -w $start $end); do   # -w takes care for leading zeros
        rm -Rf $dailyAccDir/SUCCESS_dailyacc_${year}_$doy*.d*    
    done
    rm -Rf $fullAccDir/SUCCESS_fullacc_${year}_$start*.d*

else
    echo "Directory '$priorRootDir/$tile' does not exist - no BRDF computed for tile $tile, year $year, DoY $doy."
fi

echo `date`
