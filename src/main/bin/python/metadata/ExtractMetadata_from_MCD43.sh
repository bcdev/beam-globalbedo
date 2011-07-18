#!/bin/bash


for file in `ls /disk/Globalbedo01/MCD43A1/2010/041/*hdf`
do

	echo $file

	tile=`basename $file | cut -d. -f3`

	resample -h $file

	ULC=`cat TmpHdr.hdr | grep UL_CORNER_LATLON | cut -d" " -f4-5`
 	URC=`cat TmpHdr.hdr | grep UR_CORNER_LATLON | cut -d" " -f4-5`
	LLC=`cat TmpHdr.hdr | grep LL_CORNER_LATLON | cut -d" " -f4-5`
	LRC=`cat TmpHdr.hdr | grep LR_CORNER_LATLON | cut -d" " -f4-5`

	echo $tile $ULC $URC $LLC $LRC >> TileCorners_dd.txt


done


