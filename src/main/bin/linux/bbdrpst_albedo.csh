#!/bin/tcsh

# example BBDR PST --> Albedo: nohup ./bbdrpst_albedo.csh /data/globalbedo/myGlobalbedoRootdir 2011 180W_90W 169 176 > test_bbdrpst_to_albedo.log &

# provides daily accumulation, full accumulation, inversion, BRDF --> albedo, and albedo dim --> netcdf conversion


set gaRootDir = $1 
set year = $2 
set tile = $3 
set startDoy = $4 # the start doy of 8day period
set endDoy = $5 # the end doy of 8day period

set bbdrDir = $gaRootDir/BBDR_PST
set accumulatorDir = $gaRootDir/BBDR_PST/AccumulatorFiles
set inversionDir = $gaRootDir/Inversion
set mergeDir = $gaRootDir/Merge
set albedoDir = $gaRootDir/Albedo
set accumulatorDir = $gaRootDir/BBDR_PST/AccumulatorFiles

set BEAMDIR = /opt/beam-4.11

echo "Start: `date`"

# create directories...
if (! -e "$accumulatorDir" ) then
    mkdir $accumulatorDir 
endif
if (! -e "$accumulatorDir/$year" ) then
    mkdir $accumulatorDir/$year 
endif
if (! -e "$accumulatorDir/$year/$tile" ) then
    mkdir $accumulatorDir/$year/$tile 
endif

if (! -e "$inversionDir" ) then
    mkdir $inversionDir 
endif
if (! -e "$inversionDir/$tile" ) then
    mkdir $inversionDir/$tile 
endif

if (! -e "$mergeDir" ) then
    mkdir $mergeDir 
endif
if (! -e "$mergeDir/$tile" ) then
    mkdir $mergeDir/$tile 
endif

if (! -e "$albedoDir" ) then
    mkdir $albedoDir 
endif
if (! -e "$albedoDir/$tile" ) then
    mkdir $albedoDir/$tile 
endif


# daily accumulation
#set doy = 169
#while ($doy <= 176)  # fix 8-days for the moment, June 18-25th
set doy = $startDoy
while ($doy <= $endDoy)
    echo "time $BEAMDIR/bin/gpt_8192.sh  ga.l3.dailyacc -PcomputeSeaice=true -Ptile=$tile -Pyear=$year -Pdoy=$doy -PbbdrRootDir="$bbdrDir" -e -t $accumulatorDir/$year/$tile/dailyacc_$doy.dim"
    time $BEAMDIR/bin/gpt_8192.sh  ga.l3.dailyacc -PcomputeSeaice=true -Ptile=$tile -Pyear=$year -Pdoy=$doy -PbbdrRootDir="$bbdrDir" -e -t $accumulatorDir/$year/$tile/dailyacc_$doy.dim
    @ doy = $doy + 1
end

# full accumulation
#set doy = 169
set doy = $startDoy
echo "time $BEAMDIR/bin/gpt_8192.sh  ga.l3.fullacc -PcomputeSeaice=true -Ptile=$tile -Pyear=$year -PstartDoy=$doy -PendDoy=$doy -Pwings=32 -PgaRootDir="$gaRootDir" -e -t $accumulatorDir/$year/$tile/fullacc_$doy.dim"
time $BEAMDIR/bin/gpt_8192.sh  ga.l3.fullacc -PcomputeSeaice=true -Ptile=$tile -Pyear=$year -PstartDoy=$doy -PendDoy=$doy -Pwings=32 -PgaRootDir="$gaRootDir" -e -t $accumulatorDir/$year/$tile/fullacc_$doy.dim

# inversion
echo "time $BEAMDIR/bin/gpt_8192.sh  ga.l3.inversion -PcomputeSeaice=true -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=32 -PusePrior=false -PgaRootDir="$gaRootDir" -e -t $inversionDir/$tile/GlobAlbedo.brdf.${year}${doy}.${tile}.Seaice.dim"
time $BEAMDIR/bin/gpt_8192.sh  ga.l3.inversion -PcomputeSeaice=true -Ptile=$tile -Pyear=$year -Pdoy=$doy -Pwings=32 -PusePrior=false -PgaRootDir="$gaRootDir" -e -t $inversionDir/$tile/GlobAlbedo.brdf.${year}${doy}.${tile}.Seaice.dim

# cleanup of accumulator binary files
if ( -e "$accumulatorDir" ) then
    rm -Rf $accumulatorDir
endif

# brdf --> albedo
echo "time $BEAMDIR/bin/gpt_8192.sh ga.l3.albedo -PcomputeSeaice=true -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -t $gaRootDir/Albedo/$tile/GlobAlbedo.albedo.$year$doy.$tile.Seaice.dim"
time $BEAMDIR/bin/gpt_8192.sh ga.l3.albedo -PcomputeSeaice=true -Ptile=$tile -Pyear=$year -Pdoy=$doy -PgaRootDir=$gaRootDir -e -t $gaRootDir/Albedo/$tile/GlobAlbedo.albedo.$year$doy.$tile.Seaice.dim

# Albedo dim --> nc...
if (! -e "$albedoDir/$tile/nc" ) then
    mkdir $albedoDir/$tile/nc
endif
echo "time ./albedo_dim2nc.csh ${albedoDir}/$tile ${albedoDir}/$tile/nc albedo_$tile.tar.gz"
time ./albedo_dim2nc.csh ${albedoDir}/$tile ${albedoDir}/$tile/nc albedo_$tile.tar.gz

echo "Finished: `date`"
