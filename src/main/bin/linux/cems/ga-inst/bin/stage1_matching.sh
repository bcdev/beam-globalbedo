#!/bin/bash
#BSUB -o %J.o
#BSUB -e %J.e
#BSUB -q lotus

##################################################################################################
#This program create text file for a target tile that matches SinXY-1km pixels to  lat/lon grid pixels
#for each tile (eg. h18v03), mosaic subdivision number d (eg. 02) and resolution res (005) it creates text file: tile.res.d.txt (eg h18v03.005.02.txt)
#The line in resulted files is inserted like that: index_tilePixel:index_gridPixel1,centroidsDistance1,ratioCommonArea1;index_gridPixel2,centroidDistance2,ratioCommonArea2;...       
#author: Said Kharbouche, MSSL.UCL(2014) 
##################################################################################################




################################# INPUTs #########################################################
##################################################################################################
#target tile (h00v08, h00v09,...)
tile=$1
OUTDIR=$2
##################################################################################################
##################################################################################################



# first resolution...
#X-axis resolution
resDegX[0]=0.05
#X-axis resolution
resDegY[0]=0.05
# resolution name
resName[0]=005
# Number of mosaic subdivisions 
div[0]=4
# if true the pixels in two extreme altitudes will be fused (required by TM mosaics)
polRegion[0]=false
latMin[0]=-90
latMax[0]=90
lonMin[0]=-180
lonMax[0]=180

# second resolutions...
resDegX[1]=0.5
resDegY[1]=0.5
resName[1]=05
div[1]=1
polRegion[1]=false
latMin[1]=-90
latMax[1]=90
lonMin[1]=-180
lonMax[1]=180


#outdir directoty
#OUTDIR=/group_workspaces/cems/globalalbedo/public/tmp/match

#if true coordinate in mosaic will represent pixel center otherwise upper left corner
pixCentre=true

#JARFILE
#JARFILE=/home/users/saidkharbouche/jar2/matching.jar
JARFILE=/group_workspaces/cems/globalalbedo/scripts/jar/matching.jar
#JARFILE=/group_workspaces/cems/globalalbedo/soft/beam-5.0.1/modules/beam-globalbedo-upscaling-1.3-SNAPSHOT.jar
#BEAM_LIB=/group_workspaces/cems/globalalbedo/soft/beam-5.0.1/lib
#BEAM4_LIB=/group_workspaces/cems/globalalbedo/soft/beam-4.11/lib
###################################################################################################


#process...
echo -e "-------------Tile:  h${HI}v${VI} ------------------"

echo "upsaclling..."

for r in 1 0
do
	outfolder=$OUTDIR/${resName[$r]}/
	echo "mkdir -p $outfolder"
	mkdir -p $outfolder

	echo "java -Xms2g -Xmx8g -jar $JARFILE ${resDegX[$r]}  ${resDegY[$r]}  ${resName[$r]}  ${div[$r]}  ${polRegion[$r]} $outfolder  $tile $pixCentre ${latMin[$r]} ${latMax[$r]} ${lonMin[$r]} ${lonMax[$r]}"
	java -Xms2g -Xmx8g -jar $JARFILE ${resDegX[$r]}  ${resDegY[$r]}  ${resName[$r]}  ${div[$r]}  ${polRegion[$r]} $outfolder  $tile $pixCentre ${latMin[$r]} ${latMax[$r]} ${lonMin[$r]} ${lonMax[$r]}
	/group_workspaces/cems/globalalbedo/soft/jdk1.7.0_60/bin/java -Xms2g -Xmx8g -jar $JARFILE ${resDegX[$r]}  ${resDegY[$r]}  ${resName[$r]}  ${div[$r]}  ${polRegion[$r]} $outfolder  $tile $pixCentre ${latMin[$r]} ${latMax[$r]} ${lonMin[$r]} ${lonMax[$r]}


	#echo "java -Xms2g -Xmx8g -classpath $JARFILE org.esa.beam.globalbedo.staging.Matching ${resDegX[$r]}  ${resDegY[$r]}  ${resName[$r]}  ${div[$r]}  ${polRegion[$r]} $outfolder  $tile $pixCentre ${latMin[$r]} ${latMax[$r]} ${lonMin[$r]} ${lonMax[$r]}"
	#/group_workspaces/cems/globalalbedo/soft/jdk1.7.0_60/bin/java -Xms2g -Xmx8g -classpath "$JARFILE:$BEAM4_LIB/*" org.esa.beam.globalbedo.staging.Matching ${resDegX[$r]}  ${resDegY[$r]}  ${resName[$r]}  ${div[$r]}  ${polRegion[$r]} $outfolder  $tile $pixCentre ${latMin[$r]} ${latMax[$r]} ${lonMin[$r]} ${lonMax[$r]}


done

echo "Done."

