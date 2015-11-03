#!/bin/bash

l1bAatsrDir=$1
l1bAatsrFile=$2
l1bMerisDir=$3
l1bMerisFile=$4
year=$5
month=$6
day=$7
gaRootDir=$8

COREG_ROOT_DIR=/group_workspaces/cems2/qa4ecv/vol1/olafd/ga-inst/bin/coregisteration

merisAatsrCollocDir=$gaRootDir/COLLOC/${year}/${month}/${day}/
if [ ! -e "$merisAatsrCollocDir" ]
then
    mkdir -p $merisAatsrCollocDir
fi

# collocation including coregistration...
echo "time python2.7 ${COREG_ROOT_DIR}/python_scripts/AatsrMerisCoregisterNc4.py ${l1bAatsrDir} ${l1bAatsrFile=$2}  ${l1bMerisDir} ${l1bMerisFile} ${COREG_ROOT_DIR}/work $merisAatsrCollocDir"
time python2.7 ${COREG_ROOT_DIR}/python_scripts/AatsrMerisCoregisterNc4.py ${l1bAatsrDir} ${l1bAatsrFile=$2}  ${l1bMerisDir} ${l1bMerisFile} ${COREG_ROOT_DIR}/work $merisAatsrCollocDir




#for SRCFILE_MERIS in $(ls ${l1bMerisDir}/*.N1.gz); do
#    SRCBASE_MERIS=`basename ${SRCFILE_MERIS} |cut -d'.' -f1`
#    for SRCFILE_AATSR in $(ls ${l1bAatsrDir}/*.N1.gz); do
#        SRCBASE_AATSR=`basename ${SRCFILE_AATSR} |cut -d'.' -f1`
#        echo "Coregister and collocate from MERIS L1b product '${SRCFILE_MERIS}', AATSR L1b product '${SRCFILE_AATSR}' ..."
#	echo "time python2.7 ${COREG_ROOT_DIR}/python_scripts/AatsrMerisCoregisterNc4.py $2 ${SRCFILE_AATSR} $1 ${SRCFILE_MERIS} ${COREG_ROOT_DIR}/work $merisAatsrCollocDir"
#	#time python2.7 ${COREG_ROOT_DIR}/python_scripts/AatsrMerisCoregisterNc4.py $2 ${SRCFILE_AATSR} $1 ${SRCFILE_MERIS} ${COREG_ROOT_DIR}/work $merisAatsrCollocDir
#    done
#done

echo `date`
