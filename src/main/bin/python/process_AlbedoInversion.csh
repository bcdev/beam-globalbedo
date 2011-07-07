#!/bin/tcsh

set FUNC = process_AlbedoInversion
set init_time = `date`

echo ""
echo ${FUNC}: Checking command line arguments

#if ($#argv != 1) then
#  echo ""
#  echo Usage: $FUNC TILE_TO_PROCESS
#  exit 1
#endif

echo `date`

set tile = $1
#set tile = h18v04
set year = $2
#set year = 2005

set sensor = ALL
#set wings = 540 # One year plus 3 months wings
#set wings = 8 # test!!
set wings = $3
set Outliers = 1

set DATADIR = "/bcserver12-data/GlobAlbedo/Priors/$tile/background/processed.p1.0.618034.p2.1.00000"
set SRCDIR = "/bcserver12-data/GlobAlbedo/src"
set OUTDIR = "/bcserver12-data/GlobAlbedo/inversion_py/$tile"

# To use 8 cores -> 6 runs
#set StartDoY = (001 065 129 193 257 321)
#set EndDoY = (057 121 185 249 313 361)

# To use 16 cores -> 3 runs
#set StartDoY = (001 129 257)
#set StartDoY = (121 129 137)
#set StartDoY = (129)
set StartDoY = $4
#set EndDoY = (121 249 361)
#set EndDoY = (121 129 137)
#set EndDoY = (129)
set EndDoY = $StartDoY

foreach EightDayTimePeriod (`seq 1 1 $#StartDoY`)

	echo "Processing DoY: $StartDoY[$EightDayTimePeriod] to $EndDoY[$EightDayTimePeriod]"
	# For each 8-day time period in MODIS prior...
	foreach DoY (`ls $DATADIR/Kernels.???.005.$tile.backGround.NoSnow.bin | cut -d/ -f8 | cut -d. -f2 | sort | uniq`)

        	if ( `echo $DoY + 0 | bc` >= `echo $StartDoY[$EightDayTimePeriod] + 0 | bc` && `echo $DoY + 0 | bc` <= `echo $EndDoY[$EightDayTimePeriod] + 0 | bc`) then
                	#Check if DoY already exists
	                if (-e $OUTDIR/GlobAlbedo.$year$DoY.$tile.NoSnow.bin) then
        	                echo "Doy: $DoY already processed."
	                else
        	                #Process Albedo inversion for DoY
				echo "$SRCDIR/AlbedoInversion_work.py $tile $year $wings $sensor 30 0"
                        	/opt/epd-7.0-2-rh5-x86_64/bin/python $SRCDIR/AlbedoInversion_work.py $tile $year $wings $sensor 30 0 $DoY $OUTDIR
	                endif
	        endif
	end
	foreach DoY (`ls $DATADIR/Kernels.???.005.$tile.backGround.Snow.bin | cut -d/ -f8 | cut -d. -f2 | sort | uniq`)

                if ( `echo $DoY + 0 | bc` >= `echo $StartDoY[$EightDayTimePeriod] + 0 | bc` && `echo $DoY + 0 | bc` <= `echo $EndDoY[$EightDayTimePeriod] + 0 | bc`) then
                        #Check if DoY already exists
                        if (-e $OUTDIR/GlobAlbedo.$year$DoY.$tile.Snow.bin) then
                                echo "Doy: $DoY already processed."
                        else
                                #Process Albedo inversion for DoY
                                echo "$SRCDIR/AlbedoInversion_work.py $tile $year $wings $sensor 30 1"
                                /opt/epd-7.0-2-rh5-x86_64/bin/python $SRCDIR/AlbedoInversion_work.py $tile $year $wings $sensor 30 1 $DoY $OUTDIR
                        endif
                endif
        end

	# Wait for ALL 16 processes to finish
	wait
end

# Create fcc f0 SW,NIR,VIS (RGB) for all BRDF model inversion output products
#$SRCDIR/QL/create_RGB_AlbedoInversion_741.sh

echo "Tile $tile completed."
echo `date`
