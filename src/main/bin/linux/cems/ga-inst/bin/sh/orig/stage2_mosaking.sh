#!/bin/bash
#BSUB -o %J.o
#BSUB -e %J.e
#BSUB -q lotus

############################################################################################################
#This program create netcdf mosaics (stage2) from 1km tiles and text files resulted from matching program (stage1)
#It fills mosaics parts one by one   
#The aggregation methods (energy conservation, nearest neighbor, average, ) for each layer are givens as input within a text file  
#author: Said Kharbouche, MSSL.UCL(2014)
##########################################################################################################


################################################ INPUTs #######################################
###############################################################################################
#list of brdf nc.gz files for a given year.doy 
#set of lits are on ../txt/listbrdf/ 
#launch job by list file (eg. list='../lists/brdf1km/list.2005001.merge.txt')
list=$1
##############################################################################################
##############################################################################################








#for first resolution.....
#x-axis resolution
resDegX[0]=0.05
#y-axis resolution
resDegY[0]=0.05
#resolution name (must be the same as in stage1)
resName[0]=005
#number of mosaic subdivisions 4 by axis --> 16 parts (must be the same as in satge1)
div[0]=4
#lat/lon: max/min
latMin[0]=-90
latMax[0]=90
lonMin[0]=-180
lonMax[0]=180

#dor second resolution...
resDegX[1]=0.5
resDegY[1]=0.5
resName[1]=05
div[1]=1
latMin[1]=-90
latMax[1]=90
lonMin[1]=-180
lonMax[1]=180





# if 1 transpose input 1km-tiles array
transpose=0

# width of 1km product
hTile=1200
# higth of 1km product
wTile=1200

# where product will be unzipped
INDIR=/group_workspaces/cems/globalalbedo/public/tmp/tmp/

#dir of resulted brdf 
OUTDIR=/group_workspaces/cems/globalalbedo/public/tmp/brdf/

# txt file contains bands names with their upscalling method
pathbands=/group_workspaces/cems/globalalbedo/scripts/params/bands.txt

# folder contais maps 1km to Grid (output of stage1)
matchFolder=/group_workspaces/cems/globalalbedo/public/tmp/match/

#JARFILE
JARFILE=/home/users/saidkharbouche/jar2/mosaiking.jar 


#position of tile in file name
idxTile=3
#NetCDF 'version of resulted files
version=4

#nodata values in 1-km brdf files
nodata=0

########################################################################
#threshold function on the ratio of intersection area:
#To fix the issue of empty pixels in high latitude.
#It reduces the threshold in high altitude: T = alpha/(1+exp(lamda(|lat|-highLat))
#for first resolution...
alpha[0]='0.6'
lamda[0]='2.0'
highLat[0]='65';

#for second resolution...
alpha[1]='0.6'
lamda[1]='2.0'
highLat[1]='65';



########################################################################
#numObs function:
#To fix the issue of mosaics circle-shapes in bands_VAR in high latitude
#Ut limits the num of observation in high latitude: obsMax = exp(-beta(|lat|-highLat))+obs
#this function can be desabled if obs=-1
# use obs=5 for 0.05deg and obs=-1 for 0.5deg

#for first resolution....
obs[0]=5
beta[0]=0.1

#for second resolution...
#desabled
obs[1]=-1
beta[1]='NaN'





#########################################################################
########################################################################

bn=$(basename $list)

mod='mod'
if [[ $bn =~ ".merge." ]]
then
	mod="merge"
	idxTile=4
fi

if [[ $bn =~ ".NoSnow." ]]
then
	mod="NoSnow"
fi

if [[ $bn =~ ".Snow." ]]
then
	mod="Snow"
fi

var=${bn//list./}
var=${var//merge./}
var=${var//NoSnow./}
var=${var//Snow./}

year=${var:0:4}
doy=${var:4:3}


inTilesDIR=$INDIR/$mod/$year/$doy/


mkdir -p $inTilesDIR
mkdir -p $outfolder


###################################
# copy and unzip nc.gz brdf files
###################################
echo -e "\n\n\n\n\ ------------- $year.$doy, $mod, resolution: ${resDeg[*]} ------------------"
echo "unzipping..."

while read line
do
	bn=$(basename $line)

	echo "cp -f $line $inTilesDIR/"
	cp -f $line $inTilesDIR/

	echo "gzip -df ${inTilesDIR}/$bn"
	gzip -df ${inTilesDIR}/$bn

done < $list
echo -e "end unzipping.\n\n\n"
###############################



#############################################
#mosaiking
###########################################
echo "upsaclling..."

#change this regarding number of resolutions 
for r in 0 1
do
	outfolder=$OUTDIR/${resName[$r]}/$mod/$year/$doy/
	echo "mkdir -p $outfolder"
	mkdir -p $outfolder

	echo "java -Xms4g -Xmx16g -jar $JARFILE ${resDegX[$r]}  ${resDegY[$r]}  ${resName[$r]} $year $doy $mod $inTilesDIR $outfolder $pathbands  $idxTile $version $nodata  ${div[$r]} $wTile $hTile  $matchFolder/${resName[$r]}/  ${alpha[$r]} ${lamda[$r]} ${highLat[$r]} ${obs[$r]} ${beta[$r]} $transpose ${latMin[$r]} ${latMax[$r]} ${lonMin[$r]} ${lonMax[$r]}"

	java -Xms4g -Xmx16g -jar $JARFILE ${resDegX[$r]}  ${resDegY[$r]}  ${resName[$r]} $year $doy $mod $inTilesDIR $outfolder $pathbands  $idxTile $version $nodata  ${div[$r]} $wTile $hTile  $matchFolder/${resName[$r]}/  ${alpha[$r]} ${lamda[$r]} ${highLat[$r]} ${obs[$r]} ${beta[$r]} $transpose ${latMin[$r]} ${latMax[$r]} ${lonMin[$r]} ${lonMax[$r]}

done
##########################################

##########################
# delete copied brdf files
echo "deleting..."

echo "rm $inTilesDIR/*.nc"
rm $inTilesDIR/*.nc
#########################



echo "Done."

