#!/bin/bash
#BSUB -o %J.o
#BSUB -e %J.e
#BSUB -q lotus


#########################################################################
#This program is about adding global and variable attributes to the input netcdf file and time as a third dimension (lat, lon, time) 
#author: Said Kharbouche, MSSL.UCL(2014)
#########################################################################




################################################# INPUTs ###########################################
####################################################################################################
#text file that contains list of absulute pathsof input products 
LIST=$1
####################################################################################################







#output directory
OUTDIR=/group_workspaces/cems/globalalbedo/public/tmp/albedo_1/

#netcdf version of resulted file
NCVERSION=4

#position of date in file's name
idxDate=4

#position of spatial resolution in file's name
idxRes=3


#add lat/lon as 1D array
latlon1D="true"

#add lat/lon as 2D array
latlon2D="false"

# lat name in output file
latName="lat"
# lon name in output fi;e
lonName="lon"


# add time as third dimension 
addTime="true"
# time name 
timeName="time"


# lat/lon min/max
latMin=-90
latMax=90
lonMin=-180
lonMax=180




#names of dimensions input files and thier corresponding lat/lon 
txtDim="y:lat,x:lon
"
#Global attributes
txtGlobAttr="Conventions:CF-1.4++history:GlobAlbedo Processing, 2013-2014++title:GlobAlbedo Albedo $mod Product++institution:Mullard Space Science Laboratory, Department of Space and Climate Physics, University College London++source:Satellite observations, BRDF/Albedo Inversion Model++references:GlobAlbedo ATBD V3.1++comment:none";

##############################################################################################################################################################
# varible attributes
#############################################################################################################################################################
txtBand="DHR_VIS:long_name=Black Sky Albedo in VIS=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
txtBand=${txtBand}"DHR_NIR:long_name=Black Sky Albedo in NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
txtBand=${txtBand}"DHR_SW:long_name=Black Sky Albedo in SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
txtBand=${txtBand}"BHR_VIS:long_name=White Sky Albedo in VIS=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
txtBand=${txtBand}"BHR_NIR:long_name=White sky albedo in NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
txtBand=${txtBand}"BHR_SW:long_name=White Sky Albedo in SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
						
txtBand=${txtBand}"DHR_sigmaVIS:long_name=Uncertainity of Black Sky Albedo in VIS=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"DHR_sigmaNIR:long_name=Uncertainity of Black Sky Albedo in NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"DHR_sigmaSW:long_name=Uncertainity of Black Sky Albedo in SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"

txtBand=${txtBand}"BHR_sigmaVIS:long_name=Uncertainity of White Sky Albedo in VIS=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"BHR_sigmaNIR:long_name=Uncertainity of White Sky Albedo in NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"BHR_sigmaSW:long_name=Uncertainity of White Sky Albedo in SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		
txtBand=${txtBand}"DHR_alpha_VIS_NIR:long_name=Covariance between DHR_VIS and DHR_NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"DHR_alpha_VIS_SW:long_name=Covariance between DHR_VIS and DHR_SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"DHR_alpha_NIR_SW:long_name=Covariance between DHR_NIR and DHR_SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"

txtBand=${txtBand}"BHR_alpha_VIS_NIR:long_name=Covariance between BHR_VIS and BHR_NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"BHR_alpha_VIS_SW:long_name=Covariance between BHR_VIS and BHR_SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"BHR_alpha_NIR_SW:long_name=Covariance between BHR_NIR and BHR_SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"

txtBand=${txtBand}"Relative_Entropy:long_name=Relative Entropy=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"Goodness_of_Fit:long_name=Goodness_of_Fit=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"Snow_Fraction:long_name=Snow Fraction=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"Data_Mask:long_name=Data Mask=string++_FillValue=0=byte++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"Weighted_Number_of_Samples:long_name=Weighted number of BRDF samples=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
txtBand=${txtBand}"Time_to_the_Closest_Sample:long_name=Number of days to the closest sample=string++_FillValue=NaN=float++coordinates=lat lon=string++units=days=string;"
txtBand=${txtBand}"Solar_Zenith_Angle:long_name=Solar Zenith Angle=string++_FillValue=NaN=float++coordinates=lat lon=string++units=degrees=string"							
######################################################################################################################################################################


#JARFILE
JARFILE=/home/users/saidkharbouche/jar2/addattributes.jar


#Process files listed in input list....
while read line
do
	echo -e "\n\n\n\n\n\n process $line "

	#extract infos from file' nsme
	bn=$(basename $line .gz)
	IFS='.' read -a tab <<< "${bn}"
	dat="${tab[$idxDate]}"
	year=${dat:0:4}
	doy=${dat:4:3}
	RES="${tab[idxRes]}"
	tile=""

	echo "basename: $bn"
	echo "year: $year"
	echo "doy: $doy"
	echo "tile: $tile"
	echo "RES: $RES"
	echo "mod: $mod"

	# create subdirectories
	outdir=$OUTDIR/$RES/$tile/$year/$doy/		
	echo "mkdir -p $outdir"
	mkdir -p $outdir

	
	# launch java program
	java -Xms2g -Xmx4g -jar $JARFILE $line $outdir "'$txtBand'" "'$txtDim'" "'$txtGlobAttr'" $latlon1D $latlon2D $latName $lonName $addTime $timeName $idxDate $latMin $latMax $lonMin $lonMax $NCVERSION



done < $LIST
echo "Done."

