#!/bin/tcsh

# nco cleanup script for BRDF netcdf files according to CEMS requirements.
# Processed product has no more errors or warnings if checked by CF compliance checker
# (http://titania.badc.rl.ac.uk/cgi-bin/cf-checker.pl)

# maybe, you need to add /usr/local/lib to the library path to get the NCO working properly:
# export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib

set NC_INPUT_PATH = $1
set NC_INPUT_DIR = `dirname $NC_INPUT_PATH`
set NC_INPUT_FILE = `basename $NC_INPUT_PATH`
set BEAM_VERSION = /opt/beam-4.11

cp -p $NC_INPUT_PATH $NC_INPUT_PATH.bak    # make a backup just in case...

# convert to netcdf3 - netcdf4 has a bunch of problems :-(
echo "$BEAM_VERSION/bin/gpt.sh ga.passthrough -SsourceProduct=$NC_INPUT_PATH -f NetCDF-BEAM -t $NC_INPUT_DIR/nc3_of_$NC_INPUT_FILE"
$BEAM_VERSION/bin/gpt.sh ga.passthrough -SsourceProduct=$NC_INPUT_PATH -f NetCDF-BEAM -t $NC_INPUT_DIR/nc3_of_$NC_INPUT_FILE
mv $NC_INPUT_DIR/nc3_of_$NC_INPUT_FILE $NC_INPUT_PATH

#remove the metadata variable (content does not make sense in final product):
ncks -x -v metadata $NC_INPUT_PATH $NC_INPUT_DIR/tmp_$NC_INPUT_FILE
mv $NC_INPUT_DIR/tmp_$NC_INPUT_FILE  $NC_INPUT_PATH

#fill long names:
ncatted -a long_name,mean_VIS_f0,o,c,"BRDF model parameter f0 - visible band" $NC_INPUT_PATH
ncatted -a long_name,mean_VIS_f1,o,c,"BRDF model parameter f1 - visible band" $NC_INPUT_PATH
ncatted -a long_name,mean_VIS_f2,o,c,"BRDF model parameter f2 - visible band" $NC_INPUT_PATH
ncatted -a long_name,mean_NIR_f0,o,c,"BRDF model parameter f0 - near infrared band" $NC_INPUT_PATH
ncatted -a long_name,mean_NIR_f1,o,c,"BRDF model parameter f1 - near infrared band" $NC_INPUT_PATH
ncatted -a long_name,mean_NIR_f2,o,c,"BRDF model parameter f2 - near infrared band" $NC_INPUT_PATH
ncatted -a long_name,mean_SW_f0,o,c,"BRDF model parameter f0 - shortwave band" $NC_INPUT_PATH
ncatted -a long_name,mean_SW_f1,o,c,"BRDF model parameter f1 - shortwave band" $NC_INPUT_PATH
ncatted -a long_name,mean_SW_f2,o,c,"BRDF model parameter f2 - shortwave band" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_VIS_f0,o,c,"Covariance of BRDF model parameters VIS_f0/VIS_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_VIS_f1,o,c,"Covariance of BRDF model parameters VIS_f0/VIS_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_VIS_f2,o,c,"Covariance of BRDF model parameters VIS_f0/VIS_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_NIR_f0,o,c,"Covariance of BRDF model parameters VIS_f0/NIR_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_NIR_f1,o,c,"Covariance of BRDF model parameters VIS_f0/NIR_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_NIR_f2,o,c,"Covariance of BRDF model parameters VIS_f0/NIR_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_SW_f0,o,c,"Covariance of BRDF model parameters VIS_f0/SW_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_SW_f1,o,c,"Covariance of BRDF model parameters VIS_f0/SW_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f0_SW_f2,o,c,"Covariance of BRDF model parameters VIS_f0/SW_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f1_VIS_f1,o,c,"Covariance of BRDF model parameters VIS_f1/VIS_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f1_VIS_f2,o,c,"Covariance of BRDF model parameters VIS_f1/VIS_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f1_NIR_f0,o,c,"Covariance of BRDF model parameters VIS_f1/NIR_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f1_NIR_f1,o,c,"Covariance of BRDF model parameters VIS_f1/NIR_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f1_NIR_f2,o,c,"Covariance of BRDF model parameters VIS_f1/NIR_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f1_SW_f0,o,c,"Covariance of BRDF model parameters VIS_f1/SW_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f1_SW_f1,o,c,"Covariance of BRDF model parameters VIS_f1/SW_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f1_SW_f2,o,c,"Covariance of BRDF model parameters VIS_f1/SW_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f2_VIS_f2,o,c,"Covariance of BRDF model parameters VIS_f2/VIS_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f2_NIR_f0,o,c,"Covariance of BRDF model parameters VIS_f2/NIR_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f2_NIR_f1,o,c,"Covariance of BRDF model parameters VIS_f2/NIR_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f2_NIR_f2,o,c,"Covariance of BRDF model parameters VIS_f2/NIR_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f2_SW_f0,o,c,"Covariance of BRDF model parameters VIS_f2/SW_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f2_SW_f1,o,c,"Covariance of BRDF model parameters VIS_f2/SW_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_VIS_f2_SW_f2,o,c,"Covariance of BRDF model parameters VIS_f2/SW_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f0_NIR_f0,o,c,"Covariance of BRDF model parameters NIR_f0/NIR_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f0_NIR_f1,o,c,"Covariance of BRDF model parameters NIR_f0/NIR_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f0_NIR_f2,o,c,"Covariance of BRDF model parameters NIR_f0/NIR_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f0_SW_f0,o,c,"Covariance of BRDF model parameters NIR_f0/SW_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f0_SW_f1,o,c,"Covariance of BRDF model parameters NIR_f0/SW_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f0_SW_f2,o,c,"Covariance of BRDF model parameters NIR_f0/SW_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f1_NIR_f1,o,c,"Covariance of BRDF model parameters NIR_f1/NIR_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f1_NIR_f2,o,c,"Covariance of BRDF model parameters NIR_f1/NIR_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f1_SW_f0,o,c,"Covariance of BRDF model parameters NIR_f1/SW_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f1_SW_f1,o,c,"Covariance of BRDF model parameters NIR_f1/SW_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f1_SW_f2,o,c,"Covariance of BRDF model parameters NIR_f1/SW_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f2_NIR_f2,o,c,"Covariance of BRDF model parameters NIR_f2/NIR_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f2_SW_f0,o,c,"Covariance of BRDF model parameters NIR_f2/SW_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f2_SW_f1,o,c,"Covariance of BRDF model parameters NIR_f2/SW_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_NIR_f2_SW_f2,o,c,"Covariance of BRDF model parameters NIR_f2/SW_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_SW_f0_SW_f0,o,c,"Covariance of BRDF model parameters SW_f0/SW_f0" $NC_INPUT_PATH
ncatted -a long_name,VAR_SW_f0_SW_f1,o,c,"Covariance of BRDF model parameters SW_f0/SW_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_SW_f0_SW_f2,o,c,"Covariance of BRDF model parameters SW_f0/SW_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_SW_f1_SW_f1,o,c,"Covariance of BRDF model parameters SW_f1/SW_f1" $NC_INPUT_PATH
ncatted -a long_name,VAR_SW_f1_SW_f2,o,c,"Covariance of BRDF model parameters SW_f1/SW_f2" $NC_INPUT_PATH
ncatted -a long_name,VAR_SW_f2_SW_f2,o,c,"Covariance of BRDF model parameters SW_f2/SW_f2" $NC_INPUT_PATH

ncatted -a long_name,Entropy,o,c,"Entropy" $NC_INPUT_PATH
ncatted -a long_name,Relative_Entropy,o,c,"Relative entropy" $NC_INPUT_PATH
ncatted -a long_name,Weighted_Number_of_Samples,o,c,"Weighted number of BRDF samples" $NC_INPUT_PATH
ncatted -a long_name,Goodness_of_Fit,o,c,"Goodness of fit" $NC_INPUT_PATH
ncatted -a long_name,Proportion_NSamples,o,c,"Proportion of numbers of snow/noSnow samples" $NC_INPUT_PATH
ncatted -a long_name,'crs',c,c,"Coordinate Reference System" $NC_INPUT_PATH
ncatted -a comment,crs,o,c,"A coordinate reference system (CRS) defines defines how the georeferenced spatial data relates to real locations on the Earth's surface" $NC_INPUT_PATH

#Obviously where any variables are dimensional, "units" attributes should be added accordingly, although the CF 
#convention states that units are not required for dimensionless quantities.  
#Arguably "Days_to_the_Closest_Sample" should be "Time_to_the_Closest_Sample" with a unit of "day".
ncrename -h -O -v Days_to_the_Closest_Sample,Time_to_the_Closest_Sample $NC_INPUT_PATH
ncatted -a units,'Time_to_the_Closest_Sample',c,c,"day" $NC_INPUT_PATH
ncatted -a long_name,Time_to_the_Closest_Sample,o,c,"Time to the closest sample" $NC_INPUT_PATH

# remove 'coordinates' attribute from lat and lon, add 'standard_name' attribute instead
ncatted -O -a coordinates,lat,d,f,"lat lon" $NC_INPUT_PATH
ncatted -O -a coordinates,lon,d,f,"lat lon" $NC_INPUT_PATH
ncatted -a standard_name,lat,o,c,"latitude" $NC_INPUT_PATH
ncatted -a standard_name,lon,o,c,"longitude" $NC_INPUT_PATH

#Add global attributes:

#first delete existing ones:
ncatted -a ,global,d,, $NC_INPUT_PATH

ncatted -O -h -a Conventions,global,o,c,"CF-1.4" $NC_INPUT_PATH
ncatted -O -h -a title,global,o,c,"GlobAlbedo BRDF Product" $NC_INPUT_PATH
ncatted -O -h -a institution,global,o,c,"Mullard Space Science Laboratory, Department of Space and Climate Physics, University College London" $NC_INPUT_PATH
ncatted -O -h -a source,global,o,c,"Satellite observations, BRDF/Albedo Inversion Model" $NC_INPUT_PATH
ncatted -O -h -a references,global,o,c,"GlobAlbedo ATBD V3.1" $NC_INPUT_PATH
ncatted -O -h -a comment,global,o,c,"none" $NC_INPUT_PATH
ncatted -O -h -a history,global,o,c,"GlobAlbedo Processing, 2012-2013 " $NC_INPUT_PATH  # do this as very last command!!

# convert back to netcdf4
nccopy -k 4 $NC_INPUT_PATH $NC_INPUT_DIR/nc4_of_$NC_INPUT_FILE
mv $NC_INPUT_DIR/nc4_of_$NC_INPUT_FILE $NC_INPUT_PATH

echo "Done."
