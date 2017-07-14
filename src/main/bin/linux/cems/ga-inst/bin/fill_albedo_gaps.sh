#!/bin/bash

year=$1

gaRootDir=/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest

albedoNoSnowRootDir=$gaRootDir/Albedo/NoSnow/avh_geo/$year
albedoSnowRootDir=$gaRootDir/Albedo/Snow/avh_geo/$year

for tile in `ls $albedoNoSnowRootDir`; do
    for iDoy in $(seq -w 1 365); do   # -w takes care for leading zeros
        noSnowFile=$albedoNoSnowRootDir/$tile/Qa4ecv.albedo.avh_geo.$year$iDoy.$tile.NoSnow.nc
        #echo "noSnowFile: $noSnowFile"

        if [ ! -f $noSnowFile ]; then
            echo "File $noSnowFile does not exist..."
            nextDoy=$((10#$iDoy+1))
            nextDoy=$(printf "%03d" $nextDoy)
            nextNoSnowFile=$albedoNoSnowRootDir/$tile/Qa4ecv.albedo.avh_geo.$year$nextDoy.$tile.NoSnow.nc
            while [ ! -f $nextNoSnowFile ] && [ $nextDoy -lt 365 ]
            do
                nextDoy=$((10#$nextDoy+1))
                nextDoy=$(printf "%03d" $nextDoy)

                nextNoSnowFile=$albedoNoSnowRootDir/$tile/Qa4ecv.albedo.avh_geo.$year$nextDoy.$tile.NoSnow.nc
                if [ ! -f $noSnowFile ]; then
                    echo "Next file $nextNoSnowFile does not exist either..."
                fi
            done
            if [ $nextDoy -gt 365 ]; then
                prevDoy=$((10#$iDoy-1))
                prevDoy=$(printf "%03d" $prevDoy)
            	nextNoSnowFile=$albedoNoSnowRootDir/$tile/Qa4ecv.albedo.avh_geo.$year$prevDoy.$tile.NoSnow.nc
            fi
            echo "cp -p $nextNoSnowFile $noSnowFile"
            cp -p $nextNoSnowFile $noSnowFile
        fi
    done
done

for tile in `ls $albedoSnowRootDir`; do
    for iDoy in $(seq -w 1 365); do   # -w takes care for leading zeros
        snowFile=$albedoSnowRootDir/$tile/Qa4ecv.albedo.avh_geo.$year$iDoy.$tile.Snow.nc
        #echo "snowFile: $snowFile"

        if [ ! -f $snowFile ]; then
            echo "File $snowFile does not exist..."
            nextDoy=$((10#$iDoy+1))
            nextDoy=$(printf "%03d" $nextDoy)
            nextSnowFile=$albedoSnowRootDir/$tile/Qa4ecv.albedo.avh_geo.$year$nextDoy.$tile.Snow.nc
            while [ ! -f $nextSnowFile ] && [ $nextDoy -lt 365 ]
            do
                nextDoy=$((10#$nextDoy+1))
                nextDoy=$(printf "%03d" $nextDoy)

                nextSnowFile=$albedoSnowRootDir/$tile/Qa4ecv.albedo.avh_geo.$year$nextDoy.$tile.Snow.nc
                if [ ! -f $snowFile ]; then
                    echo "Next file $nextSnowFile does not exist either..."
                fi
            done
            if [ $nextDoy -gt 365 ]; then
                prevDoy=$((10#$iDoy-1))
                prevDoy=$(printf "%03d" $prevDoy)
                nextSnowFile=$albedoSnowRootDir/$tile/Qa4ecv.albedo.avh_geo.$year$prevDoy.$tile.Snow.nc
            fi
            echo "cp -p $nextSnowFile $snowFile"
            cp -p $nextSnowFile $snowFile
        fi
    done
done

