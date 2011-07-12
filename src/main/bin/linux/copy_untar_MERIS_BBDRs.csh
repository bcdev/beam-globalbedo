#!/bin/tcsh

set init_time = `date`

set SENSOR = MERIS
#set WORKDIR = "$HOME/GlobAlbedo/src"
set WORKDIR = "/data/GlobAlbedo/src"

set localhost = `hostname -s`

#set NumberOfTiles = `cat $WORKDIR/MODIS_ValidationTileList.txt | wc -l`
set NumberOfTiles = 1

set monthToUntar = "01"

set i = 1
while ($i <= $NumberOfTiles)

#    set TILE = `awk 'NR==TileNumber {print $1}' TileNumber=$i $WORKDIR/MODIS_ValidationTileList.txt`
    set TILE = h18v04
#    set SENSOR_TO_PROCESS = `awk 'NR==TileNumber {print $2}' TileNumber=$i $WORKDIR/MODIS_ValidationTileList.txt`

	set BBDR_DIR = "/data/GlobAlbedo/h18v04/MERIS_targz"

	echo "BBDR_DIR: " $BBDR_DIR

	if ( -d $BBDR_DIR ) then
		#set DIR_SIZE = `du -s $BBDR_DIR | cut -f1`
		#if ($DIR_SIZE < 100000) then
			#@ i = $i + 1
	                #continue
		#endif

#		foreach year (2004 2005 2006)
		foreach year (2005)
			set LOCAL_DATADIR = "/data/GlobAlbedo/BBDR/$SENSOR/$year/$TILE"

                        #Check if file is already in local datadir
#                        foreach BBDR ($BBDR_DIR/*$year????_??????*gz)
                        foreach BBDR ($BBDR_DIR/*$year$monthToUntar??_??????*gz)
				set filename = `basename $BBDR .tar.gz`

			        set YearMonthDay = `echo $filename | cut -d"_" -f5 | cut -c7-14`
			        set Time = `echo $filename | cut -d"_" -f6`

			        set month = `echo $YearMonthDay | cut -c5-6`
			        set day = `echo $YearMonthDay | cut -c7-8`

			        set JulianDay = `$WORKDIR/convert_to_julian_day.csh $year $month $day`
				set BBDR_local_filename = $year${JulianDay}_$Time

#				if (-d $LOCAL_DATADIR/$BBDR_local_filename) then
				if (-d $LOCAL_DATADIR/bla$BBDR_local_filename) then
					echo "$filename already uncompressed in local directory"
				else
					echo "Copying $BBDR to $LOCAL_DATADIR..."
					cp $BBDR $LOCAL_DATADIR

					echo "Unpacking $filename..."
					cd $LOCAL_DATADIR
					tar -zxvf $LOCAL_DATADIR/$filename.tar.gz

					# Create symlink only if the BBDR is gt 129MB
					set BBDR_SIZE = `du -s $filename.data | cut -f1`
					if ($BBDR_SIZE > 130000) then
						ln -s $filename.data $year${JulianDay}_$Time
					else
#						/bin/rm -rf $filename.data $filename.dim
					endif

					echo "Removing tar.gz files..."
#					/bin/rm -rf $LOCAL_DATADIR/$filename.tar.gz
				endif
			end
		end
	endif
    endif

    cd $WORKDIR
    @ i = $i + 1
end

echo "Tile $TILE completed."
echo " Started  at $init_time"
echo " Finished at "`date`
