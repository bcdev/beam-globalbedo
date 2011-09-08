#!/bin/bash

gaRootDir=$1
if [ -e ${gaRootDir} ] 
then
    echo "${gaRootDir} already exists."
else
    #Create ga root dir
    mkdir -v ${gaRootDir}
    if [ -e ${gaRootDir}/Albedo ]
    then
        echo "${gaRootDir}/Albedo already exists."
    else
        #Create Albedo dir
        mkdir -v ${gaRootDir}/Albedo
    fi

    if [ -e ${gaRootDir}/BBDR ]
    then
        echo "${gaRootDir}/BBDR already exists."
    else
        #Create BBDR dir
        mkdir -v ${gaRootDir}/BBDR
    fi

    if [ -e ${gaRootDir}/Inversion ]
    then
        echo "${gaRootDir}/Inversion already exists."
    else
        #Create Inversion dir
        mkdir -v ${gaRootDir}/Inversion
    fi

    if [ -e ${gaRootDir}/Merge ]
    then
        echo "${gaRootDir}/Merge already exists."
    else
        #Create Merge dir
        mkdir -v ${gaRootDir}/Merge
    fi

    if [ -e ${gaRootDir}/MonthlyAlbedo ]
    then
        echo "${gaRootDir}/MonthlyAlbedo already exists."
    else
        #Create MonthlyAlbedo dir
        mkdir -v ${gaRootDir}/MonthlyAlbedo
    fi

    if [ -e ${gaRootDir}/Mosaic ]
    then
        echo "${gaRootDir}/Mosaic already exists."
    else
        #Create Mosaic dir
        mkdir -v ${gaRootDir}/Mosaic
        mkdir -v ${gaRootDir}/Mosaic/albedo
        mkdir -v ${gaRootDir}/Mosaic/brdf
    fi

fi

