#!/bin/bash
#BSUB -o %J.o
#BSUB -e %J.e
#BSUB -q lotus

####################################################################
#This program is about creating albedo product from brdf product (merge, snow, nosnow)
#author: Said Kharbouche, MSSL.UCL(2014)
####################################################################


################################################# INPUTs ###########################################
####################################################################################################
#text file that contains list of absulute paths of brdf products
#create sub lists and run job by one sublist
LIST=$1
####################################################################################################






#1: 1km-tile brdf array will be trasposed, 0: will not be transposed
TRANSPOSE=0

#DIR where brdf products will be stocked and unzipped
#TMP=tmp/

# position of date in file's name
IdxDate=4
# position of resolution in file's name, -1: there is no resolution. If -1 it will take 1km as resolution name
IdxRES=3
# position of tile name in file's name, -1: there is no tile.  
IdxTILE=-1

# product name that will replace the string 'brdf' in file's name
productName=albedo

#NetCDF version of resulted files
NCDFvi=4

#albedo bands that will be computed
BANDS="DHR_VIS,DHR_NIR,DHR_SW,BHR_VIS,BHR_NIR,BHR_SW,DHR_sigmaVIS,DHR_sigmaNIR,DHR_sigmaSW,BHR_sigmaVIS,BHR_sigmaNIR,BHR_sigmaSW,DHR_alpha_VIS_NIR,DHR_alpha_VIS_SW,DHR_alpha_NIR_SW,BHR_alpha_VIS_NIR,BHR_alpha_VIS_SW,BHR_alpha_NIR_SW,Data_Mask,Relative_Entropy,Snow_Fraction,Solar_Zenith_Angle,Weighted_Number_of_Samples,Goodness_of_Fit,Time_to_the_Closest_Sample" 
#BANDS="BHR_VIS,BHR_NIR,BHR_sigmaVIS,BHR_sigmaNIR,BHR_alpha_VIS_NIR,Weighted_Number_of_Samples"
#BANDS="BHR_SW,BHR_sigmaSW"		

#DIR of resulted albedo
OUTDIR="/group_workspaces/cems/globalalbedo/public/tmp/albedo/"

#JARFILE
JARFILE=/home/users/saidkharbouche/jar2/brdf2albedo.jar 






#process brdf files listed in input file
while read line
do

	echo -e "\n\n\n\n\n\n process $line "

	bn=$(basename $line .gz)

	IFS='.' read -a tab <<< "${bn}"

	dat="${tab[$IdxDate]}"
	year=${dat:0:4}
	doy=${dat:4:3}

	tile='/'
	if [ $IdxTILE -ge "0" ]
	then
		tile="${tab[$IdxTILE]}"
	fi

 	##################
	# spatial resolution?
	RES=1km
	if [ $IdxRES -ge "0" ]
	then
		RES="${tab[$IdxRES]}"
	fi
	##################

	##################
	# is bedf snow, nosnow or merge?
	mod=""
	if [[ $bn =~ ".merge." ]]
	then
		mod="merge"
	fi

	if [[ $bn =~ ".Snow." ]]
	then
		mod="Snow"
	fi

	if [[ $bn =~ ".NoSnow." ]]
	then
		mod="NoSnow"
	fi
	####################

	#chaekning extracted info from file'name....
	echo "basename: $bn"
	echo "year: $year"
	echo "doy: $doy"
	echo "tile: $tile"
	echo "RES: $RES"
	echo "mod: $mod"



	outdir=$OUTDIR/$RES/$tile/$year/$doy/		
	# create output subdirectories
	echo "mkdir -p $outdir"
	mkdir -p $outdir

	# copy brdf file
	#brdfFile=$TMP/$bn
	#echo cp $line $TMP
	#cp $line $TMP
	
	#unzip brdf file if nc.gz
	#if [ -e ${brdfFile}.gz ]
	#then
	#	echo "gzip -df ${brdfFile}.gz"
	#	gzip -df ${brdfFile}.gz
	#fi
	brdfFile=$line
	
	# launch java program
	echo "java -Xms8g -Xmx10g -jar $JARFILE $TRANSPOSE $brdfFile $outdir  $BANDS $IdxDate $tile  $RES $productName $NCDFvi"
	java -Xms2g -Xmx10g -jar $JARFILE $TRANSPOSE $brdfFile $outdir  $BANDS $IdxDate $tile  $RES $productName $NCDFvi

	# remove copied brdf file
	#echo rm $brdfFile
	#rm $brdfFile

done < $LIST


echo "Done."
