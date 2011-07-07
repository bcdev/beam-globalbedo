#!/bin/csh

set tile = $1
set SRCDIR = "$HOME/GlobAlbedo/src"

set OUTPUTDIR = "/disk/Globalbedo63/GlobAlbedo/MonthlyAlbedo/$tile"
mkdir -p $OUTPUTDIR
cd $OUTPUTDIR

set months = (01 02 03 04 05 06 07 08 09 10 11 12)

set NumberOfMonths = $#months

set i = 1
while ( $i <= $NumberOfMonths )

	echo $SRCDIR/MonthlyAlbedoFrom8day.py $tile $months[$i]
	$SRCDIR/MonthlyAlbedoFrom8day.py $tile $months[$i]

	@ i = $i + 1

end

#Create QLs
$SRCDIR/QL/create_RGB_Albedo_741.sh
