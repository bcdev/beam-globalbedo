#!/bin/tcsh

# example L1b --> BBDR MERIS: nohup ./l1b_nocoreg_bbdrpst.csh /data/globalbedo/seaice/L1b/MERIS/ /data/globalbedo/seaice/L1b/AATSR /data/globalbedo/seaice/20070621/fulldaytest MERIS > fulldaytest_MERIS.log &

# NO coregistration!

set masterInputDataDir = $1 
set slaveInputDataDir = $2 
set outputRootDir = $3 
set bbdrSensor = $4
set collocDir = $outputRootDir/$bbdrSensor/COLLOC
set aotDir = $outputRootDir/$bbdrSensor/AOT
set bbdrDir = $outputRootDir/$bbdrSensor/BBDR
set bbdrPstDir = $outputRootDir/$bbdrSensor/BBDR_PST
set BEAMDIR = /opt/beam-4.11

echo "Start: `date`"

# create directories...
if (! -e "$outputRootDir" ) then
    mkdir $outputRootDir 
endif
if (! -e "$collocDir" ) then
    mkdir $collocDir 
endif
if (! -e "$aotDir" ) then
    mkdir $aotDir 
endif
if (! -e "$bbdrDir" ) then
    mkdir $bbdrDir 
endif
if (! -e "$bbdrPstDir" ) then
    mkdir $bbdrPstDir 
endif
if (! -e "$bbdrPstDir/nc" ) then
    mkdir $bbdrPstDir/nc 
endif


# collocation without coregistration...
echo "time $BEAMDIR/bin/gpt_8192.sh ga.seaice.merisaatsr.colloc -PmasterSensor=$bbdrSensor -PmasterInputDataDir=$masterInputDataDir -PslaveInputDataDir=$slaveInputDataDir -PcollocOutputDataDir=$collocDir"
time $BEAMDIR/bin/gpt_8192.sh ga.seaice.merisaatsr.colloc -PmasterSensor=$bbdrSensor -PmasterInputDataDir=$masterInputDataDir -PslaveInputDataDir=$slaveInputDataDir -PcollocOutputDataDir=$collocDir

# colloc --> aot...

foreach SRCFILE (`ls ${collocDir}/COLLOC*.dim`)
    set SRCBASE = `basename ${SRCFILE} |cut -d'.' -f1`
    echo "Compute AOT from collocation product " ${SRCFILE} "..."
    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.colloc.aot -SsourceProduct=${SRCFILE} -Psensor=$bbdrSensor -t ${aotDir}/AOT_${SRCBASE}.dim"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.colloc.aot -SsourceProduct=${SRCFILE} -Psensor=$bbdrSensor -t ${aotDir}/AOT_${SRCBASE}.dim
    echo "Done AOT computation of product " ${SRCFILE} "."
end

#rm -Rf ${collocDir}

# aot --> bbdr...

foreach SRCFILE (`ls ${aotDir}/AOT*.dim`)
    set SRCBASE = `basename ${SRCFILE} |cut -d'.' -f1`
    echo "Compute BBDR from AOT product " ${SRCFILE} "..."
    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.aot.bbdr -SsourceProduct=${SRCFILE} -Psensor=$bbdrSensor -t ${bbdrDir}/BBDR_${SRCBASE}.dim"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.aot.bbdr -SsourceProduct=${SRCFILE} -Psensor=$bbdrSensor -t ${bbdrDir}/BBDR_${SRCBASE}.dim
    echo "Done BBDR computation of product " ${SRCFILE} "."
end

#rm -Rf ${aotDir}

# bbdr --> bbdr_pst...

foreach SRCFILE (`ls ${bbdrDir}/BBDR*.dim`)
    set SRCBASE = `basename ${SRCFILE} |cut -d'.' -f1`
    #echo "SRCBASE: " ${SRCBASE}
    echo "Reproject BBDR product " ${SRCFILE} " to quadrants..."

    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.bbdr.pst.single -SsourceProduct=${SRCFILE} -PquadrantName="180W_90W" -t ${bbdrPstDir}/180W_90W/${SRCBASE}_180W_90W_PST.dim"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.bbdr.pst.single -SsourceProduct=${SRCFILE} -PquadrantName="180W_90W" -t ${bbdrPstDir}/180W_90W/${SRCBASE}_180W_90W_PST.dim

    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.bbdr.pst.single -SsourceProduct=${SRCFILE} -PquadrantName="90W_0" -t ${bbdrPstDir}/90W_0/${SRCBASE}_90W_0_PST.dim"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.bbdr.pst.single -SsourceProduct=${SRCFILE} -PquadrantName="90W_0" -t ${bbdrPstDir}/90W_0/${SRCBASE}_90W_0_PST.dim

    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.bbdr.pst.single -SsourceProduct=${SRCFILE} -PquadrantName="0_90E" -t ${bbdrPstDir}/0_90E/${SRCBASE}_0_90E_PST.dim"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.bbdr.pst.single -SsourceProduct=${SRCFILE} -PquadrantName="0_90E" -t ${bbdrPstDir}/0_90E/${SRCBASE}_0_90E_PST.dim

    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.bbdr.pst.single -SsourceProduct=${SRCFILE} -PquadrantName="90E_180E" -t ${bbdrPstDir}/90E_180E/${SRCBASE}_90E_180E_PST.dim"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.bbdr.pst.single -SsourceProduct=${SRCFILE} -PquadrantName="90E_180E" -t ${bbdrPstDir}/90E_180E/${SRCBASE}_90E_180E_PST.dim

    echo "Done reprojecting BBDR product " ${SRCFILE} " to quadrants."
end

#rm -Rf ${bbdrDir}

# bbdrpst dim --> nc...
if (! -e "$bbdrPstDir/180W_90W/nc" ) then
    mkdir $bbdrPstDir/180W_90W/nc
endif
echo "time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/180W_90W ${bbdrPstDir}/180W_90W/nc bbdr_pst_180W_90W.tar.gz"
time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/180W_90W ${bbdrPstDir}/180W_90W/nc bbdr_pst_180W_90W.tar.gz

if (! -e "$bbdrPstDir/90W_0/nc" ) then
    mkdir $bbdrPstDir/90W_0/nc
endif
echo "./time bbdrpst_dim2nc.csh ${bbdrPstDir}/90W_0 ${bbdrPstDir}/90W_0/nc bbdr_pst_90W_0.tar.gz"
time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/90W_0 ${bbdrPstDir}/90W_0/nc bbdr_pst_90W_0.tar.gz

if (! -e "$bbdrPstDir/0_90E/nc" ) then
    mkdir $bbdrPstDir/0_90E/nc
endif
echo "./time bbdrpst_dim2nc.csh ${bbdrPstDir}/0_90E ${bbdrPstDir}/0_90E/nc bbdr_pst_0_90E.tar.gz"
time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/0_90E ${bbdrPstDir}/0_90E/nc bbdr_pst_0_90E.tar.gz

if (! -e "$bbdrPstDir/90E_180E/nc" ) then
    mkdir $bbdrPstDir/90E_180E/nc
endif
echo "./time bbdrpst_dim2nc.csh ${bbdrPstDir}/90E_180E ${bbdrPstDir}/90E_180E/nc bbdr_pst_90E_180E.tar.gz"
time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/90E_180E ${bbdrPstDir}/90E_180E/nc bbdr_pst_90E_180E.tar.gz

echo "Finished: `date`"
