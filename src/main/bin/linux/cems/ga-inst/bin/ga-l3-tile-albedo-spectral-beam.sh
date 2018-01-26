#!/bin/bash

# computes spectral NOSNOW, SNOW and finally MERGED albedo from spectral BRDF SNOW and NOSNOW band merged products

tile=$1
year=$2
start=$3
end=$4
gaRootDir=$5
spectralInversionRootDir=${6}
spectralAlbedoRootDir=${7}
beamDir=${8}

### set GPT
gpt=$beamDir/bin/gpt-d-l2.sh


spectralInversionNosnowDir=$spectralInversionRootDir/NoSnow/$year/$tile
spectralInversionSnowDir=$spectralInversionRootDir/Snow/$year/$tile

spectralAlbedoNosnowTargetDir=$spectralAlbedoRootDir/NoSnow/$year/$tile
spectralAlbedoSnowTargetDir=$spectralAlbedoRootDir/Snow/$year/$tile
spectralAlbedoMergeTargetDir=$spectralAlbedoRootDir/Merge/$year/$tile
spectralInversionSnowBandmergeTargetDir=$spectralInversionRootDir/Snow/$year/$tile
mkdir -p $spectralAlbedoNosnowTargetDir
mkdir -p $spectralAlbedoSnowTargetDir
mkdir -p $spectralAlbedoMergeTargetDir

for doy in $(seq -w $start $end); do   # -w takes care for leading zeros

    echo "Spectral NOSNOW albedo for tile: '$tile' , year $year, DoY $doy ..."
    SRC=${spectralInversionNosnowDir}/Qa4ecv.brdf.spectral.$year$doy.$tile.NoSnow.nc
    TARGET_NOSNOW=${spectralAlbedoNosnowTargetDir}/Qa4ecv.albedo.spectral.$year$doy.$tile.NoSnow.nc
    echo "time $gpt ga.albedo.albedo.spectral -SspectralBrdfProduct=$SRC -Pdoy=$doy -e -f NetCDF4-BEAM -t ${TARGET_NOSNOW}"
    time $gpt ga.albedo.albedo.spectral -SspectralBrdfProduct=$SRC -Pdoy=$doy -e -f NetCDF4-BEAM -t ${TARGET_NOSNOW}
    status=$?

    echo "Status: $status"

    if [ "$status" -eq 0 ]; then
        echo "Spectral SNOW albedo for tile $tile, year $year, DoY $doy ..."
	SRC=${spectralInversionSnowDir}/Qa4ecv.brdf.spectral.$year$doy.$tile.Snow.nc
        TARGET_SNOW=${spectralAlbedoSnowTargetDir}/Qa4ecv.albedo.spectral.$year$doy.$tile.Snow.nc
        echo "time $gpt ga.albedo.albedo.spectral -SspectralBrdfProduct=$SRC -Pdoy=$doy -e -f NetCDF4-BEAM -t ${TARGET_SNOW}"
        time $gpt ga.albedo.albedo.spectral -SspectralBrdfProduct=$SRC -Pdoy=$doy -e -f NetCDF4-BEAM -t ${TARGET_SNOW}
        status=$?

        echo "Status: $status"
    fi

    if [ "$status" -eq 0 ]; then
       if [ -f ${TARGET_NOSNOW} ] && [ -f ${TARGET_SNOW} ]
       then
          TARGET_MERGE=${spectralAlbedoMergeTargetDir}/Qa4ecv.albedo.spectral.$year$doy.$tile.Merge.nc
          echo "time $gpt ga.albedo.mergealbedo -SnoSnowProduct=${TARGET_NOSNOW} -SsnowProduct=${TARGET_SNOW} -e -f NetCDF4-BEAM -t ${TARGET_MERGE}"
          time $gpt ga.albedo.mergealbedo -SnoSnowProduct=${TARGET_NOSNOW} -SsnowProduct=${TARGET_SNOW} -e -f NetCDF4-BEAM -t ${TARGET_MERGE}
       else
          if [ ! -f ${TARGET_NOSNOW} ]
          then
             echo "Input ${TARGET_NOSNOW} does not exist - exiting..."
          else
             echo "Input ${TARGET_SNOW} does not exist - exiting..."
          fi
          exit 1
       fi

       echo "Status: $status"
    fi


done

echo `date`
