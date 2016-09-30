#!/bin/bash

# wrapper script fpr python coregistration call 

aatsrProduct=$1
merisProduct=$2
merisFile=$3
gaRootDir=$4

cp $merisProduct $GA_INST/tmp
gunzip $GA_INST/tmp/$merisFile
merisUnzippedFile=`basename $merisFile .gz`
merisProduct=$GA_INST/tmp/$merisUnzippedFile

COREG_ROOT_DIR=$GA_INST/bin/coregisteration_tmp

coregResultDir=$gaRootDir/AATSR_COREG
if [ ! -e "$coregResultDir" ]
then
    mkdir -p $coregResultDir
fi

source $GA_INST/my_python_env/bin/activate
echo "time python2.7 $COREG_ROOT_DIR/python_scripts/meris_to_aatsr_nadir_example_script.py $aatsrProduct $merisProduct $coregResultDir"
time python2.7 $COREG_ROOT_DIR/python_scripts/meris_to_aatsr_nadir_example_script.py $aatsrProduct $merisProduct $coregResultDir
