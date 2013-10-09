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
if (! -e "$outputRootDir/$bbdrSensor" ) then
    mkdir $outputRootDir/$bbdrSensor
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


# collocation without coregistration...
echo "time $BEAMDIR/bin/gpt_8192.sh ga.seaice.merisaatsr.colloc -PmasterSensor=$bbdrSensor -PmasterInputDataDir=$masterInputDataDir -PslaveInputDataDir=$slaveInputDataDir -PcollocOutputDataDir=$collocDir"
time $BEAMDIR/bin/gpt_8192.sh ga.seaice.merisaatsr.colloc -PmasterSensor=$bbdrSensor -PmasterInputDataDir=$masterInputDataDir -PslaveInputDataDir=$slaveInputDataDir -PcollocOutputDataDir=$collocDir

foreach SRCFILE (`ls ${masterInputDataDir}/*.N1`)
    set SRCBASE = `basename ${SRCFILE} |cut -d'.' -f1`
    echo "Collocate from MERIS L1b product " ${SRCFILE} "..."
    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.seaice.merisaatsr.colloc -SsourceProduct=${SRCFILE} -PmasterSensor=$bbdrSensor -PslaveInputDataDir=$slaveInputDataDir -PcollocOutputDataDir=$collocDir"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.seaice.merisaatsr.colloc -SsourceProduct=${SRCFILE} -PmasterSensor=$bbdrSensor -PslaveInputDataDir=$slaveInputDataDir -PcollocOutputDataDir=$collocDir
    echo "Done collocation of product " ${SRCFILE} "."
end


# colloc --> aot...

foreach SRCFILE (`ls ${collocDir}/COLLOC*.dim`)
    set SRCBASE = `basename ${SRCFILE} |cut -d'.' -f1`
    echo "Compute AOT from collocation product " ${SRCFILE} "..."
    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.colloc.aot -SsourceProduct=${SRCFILE} -Psensor=$bbdrSensor -t ${aotDir}/AOT_${SRCBASE}.dim"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.colloc.aot -SsourceProduct=${SRCFILE} -Psensor=$bbdrSensor -t ${aotDir}/AOT_${SRCBASE}.dim
    echo "Done AOT computation of product " ${SRCFILE} "."
end

rm -Rf ${collocDir}

# aot --> bbdr...

foreach SRCFILE (`ls ${aotDir}/AOT*.dim`)
    set SRCBASE = `basename ${SRCFILE} |cut -d'.' -f1`
    echo "Compute BBDR from AOT product " ${SRCFILE} "..."
    echo "time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.aot.bbdr -SsourceProduct=${SRCFILE} -Psensor=$bbdrSensor -t ${bbdrDir}/BBDR_${SRCBASE}.dim"
    time ${BEAMDIR}/bin/gpt_8192.sh ga.l2.aot.bbdr -SsourceProduct=${SRCFILE} -Psensor=$bbdrSensor -t ${bbdrDir}/BBDR_${SRCBASE}.dim
    echo "Done BBDR computation of product " ${SRCFILE} "."
end

rm -Rf ${aotDir}

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

    # remove BBDRs which are not needed any more
    echo "Done reprojecting BBDR product " ${SRCFILE} " to quadrants - remove it now..."
    echo "rm -f ${SRCFILE}" 
    rm -f ${SRCFILE} 
    echo "rm -Rf ${bbdrDir}/${SRCBASE}.data"
    rm -Rf ${bbdrDir}/${SRCBASE}.data
end

rm -Rf ${bbdrDir}

echo "Convert bbdrpst dim --> nc..."
# bbdrpst dim --> nc...
if (! -e "$bbdrPstDir/180W_90W/nc" ) then
    mkdir $bbdrPstDir/180W_90W/nc
endif
echo "time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/180W_90W ${bbdrPstDir}/180W_90W/nc bbdr_pst_180W_90W.tar.gz"
time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/180W_90W ${bbdrPstDir}/180W_90W/nc bbdr_pst_180W_90W.tar.gz
echo "Cleanup BBDR PST Dimaps for 180W_90W ..."
rm -Rf ${bbdrPstDir}/180W_90W/*.dim ${bbdrPstDir}/180W_90W/*.data

if (! -e "$bbdrPstDir/90W_0/nc" ) then
    mkdir $bbdrPstDir/90W_0/nc
endif
echo "./time bbdrpst_dim2nc.csh ${bbdrPstDir}/90W_0 ${bbdrPstDir}/90W_0/nc bbdr_pst_90W_0.tar.gz"
time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/90W_0 ${bbdrPstDir}/90W_0/nc bbdr_pst_90W_0.tar.gz
echo "Cleanup BBDR PST Dimaps for 90W_0 ..."
rm -Rf ${bbdrPstDir}/90W_0/*.dim ${bbdrPstDir}/90W_0/*.data

if (! -e "$bbdrPstDir/0_90E/nc" ) then
    mkdir $bbdrPstDir/0_90E/nc
endif
echo "./time bbdrpst_dim2nc.csh ${bbdrPstDir}/0_90E ${bbdrPstDir}/0_90E/nc bbdr_pst_0_90E.tar.gz"
time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/0_90E ${bbdrPstDir}/0_90E/nc bbdr_pst_0_90E.tar.gz
echo "Cleanup BBDR PST Dimaps for 0_90E ..."
rm -Rf ${bbdrPstDir}/0_90E/*.dim ${bbdrPstDir}/0_90E/*.data

if (! -e "$bbdrPstDir/90E_180E/nc" ) then
    mkdir $bbdrPstDir/90E_180E/nc
endif
echo "./time bbdrpst_dim2nc.csh ${bbdrPstDir}/90E_180E ${bbdrPstDir}/90E_180E/nc bbdr_pst_90E_180E.tar.gz"
time ./bbdrpst_dim2nc.csh ${bbdrPstDir}/90E_180E ${bbdrPstDir}/90E_180E/nc bbdr_pst_90E_180E.tar.gz
echo "Cleanup BBDR PST Dimaps for 90E_180E ..."
rm -Rf ${bbdrPstDir}/90E_180E/*.dim ${bbdrPstDir}/90E_180E/*.data

echo "Finished: `date`"
