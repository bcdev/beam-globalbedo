#!/bin/tcsh

# This script converts dimap products to netcdf products.
# The netcdf products will be archived in a tgz 

set inputDir = $1   # the input directory
set outputDir = $2  # the directory containing the output netcdfs. They will be packed there. 
set outputTgz = $3  # the output tgz filename. 
set BEAMDIR = /opt/beam-4.11

foreach SRCFILE (`ls ${inputDir}/*.dim`)
    set SRCBASE = `basename ${SRCFILE} .dim`
    set numBbdrFiles = `ls -1 ${inputDir}/${SRCBASE}.data/BB_* | wc -l`
    echo "SRCBASE, numBbdrfiles: ${SRCBASE} , $numBbdrFiles" 
    if ( $numBbdrFiles == 6 ) then   # for non-empty products, we should have 3 BB_ .img files and 3 BB_ .hdr files
	echo "Producing netCDF '${outputDir}/${SRCBASE}.nc' from Dimap product " ${SRCFILE} "..."
    	echo "time ${BEAMDIR}/bin/gpt.sh ga.passthrough -SsourceProduct=${SRCFILE} -f NetCDF4-BEAM -t ${outputDir}/${SRCBASE}.nc"
    	time ${BEAMDIR}/bin/gpt.sh ga.passthrough -SsourceProduct=${SRCFILE} -f NetCDF4-BEAM -t ${outputDir}/${SRCBASE}.nc
    	echo "Done producing netCDF from Dimap product " ${SRCFILE} "."
    	echo "Removing Dimap product " ${SRCFILE} "..."
    #    rm ${SRCFILE}
    #    rm -Rf ${inputDir}/${SRCBASE}.data
    endif
end

echo "Done producing all netCDF products - now archiving to tgz... "

# pack netcdfs into new tgz...
set CURR_DIR = $PWD
echo $CURR_DIR
cd ${outputDir}
echo "tar zcvf ${outputTgz} *.nc"
tar zcvf ${outputTgz} *.nc
mv ${outputTgz} ../../${outputTgz}
cd $CURR_DIR

# cleanup: remove single netcdfs and input tgz...
echo "Cleanup..."
# remove netcdfs...
echo "rm -f ${outputDir}/*.nc"
#rm -f ${outputDir}/*.nc

echo "Done."
