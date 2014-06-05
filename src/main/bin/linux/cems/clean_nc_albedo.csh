#!/bin/tcsh

set NC_INPUT = $1

#ALBEDO file:
cp -p $NC_INPUT $NC_INPUT.bak    # just in case...

#remove the metadata variable (content does not make sense in final product):
ncks -x -v metadata $NC_INPUT tmp_$NC_INPUT
mv tmp_$NC_INPUT $NC_INPUT

#fill long names:
ncatted -a long_name,DHR_VIS,o,c,"Directional Hemisphere Reflectance albedo - visible band" $NC_INPUT
ncatted -a long_name,DHR_NIR,o,c,"Directional Hemisphere Reflectance albedo - near infrared band" $NC_INPUT
ncatted -a long_name,DHR_SW,o,c,"Directional Hemisphere Reflectance albedo - shortwave band" $NC_INPUT
ncatted -a long_name,DHR_sigmaVIS,o,c,"Uncertainty of Directional Hemisphere Reflectance albedo - visible band" $NC_INPUT
ncatted -a long_name,DHR_sigmaNIR,o,c,"Uncertainty of Directional Hemisphere Reflectance albedo - near infrared band" $NC_INPUT
ncatted -a long_name,DHR_sigmaSW,o,c,"Uncertainty of Directional Hemisphere Reflectance albedo - shortwave band" $NC_INPUT

ncatted -a long_name,BHR_VIS,o,c,"Bi-Hemisphere Reflectance albedo - visible band" $NC_INPUT
ncatted -a long_name,BHR_NIR,o,c,"Bi-Hemisphere Reflectance albedo - near infrared band" $NC_INPUT
ncatted -a long_name,BHR_SW,o,c,"Bi-Hemisphere Reflectance albedo - shortwave band" $NC_INPUT
ncatted -a long_name,BHR_sigmaVIS,o,c,"Uncertainty of Bi-Hemisphere Reflectance albedo - visible band" $NC_INPUT
ncatted -a long_name,BHR_sigmaNIR,o,c,"Uncertainty of Bi-Hemisphere Reflectance albedo - near infrared band" $NC_INPUT
ncatted -a long_name,BHR_sigmaSW,o,c,"Uncertainty of Bi-Hemisphere Reflectance albedo - shortwave band" $NC_INPUT

ncatted -a long_name,Weighted_Number_of_Samples,o,c,"Weighted number of albedo samples" $NC_INPUT
ncatted -a long_name,Relative_Entropy,o,c,"Relative entropy" $NC_INPUT
ncatted -a long_name,Goodness_of_Fit,o,c,"Goodness of fit" $NC_INPUT
ncatted -a long_name,Snow_Fraction,o,c,"Snow fraction" $NC_INPUT
ncatted -a long_name,Data_Mask,o,c,"Data mask" $NC_INPUT
ncatted -a long_name,Solar_Zenith_Angle,o,c,"Solar zenith angle" $NC_INPUT
ncatted -a long_name,'crs',c,c,"Coordinate Reference System" $NC_INPUT

#Add units:
ncatted -a units,'Solar_Zenith_Angle',c,c,"degrees" $NC_INPUT

#Add global attributes:

#first delete existing ones:
ncatted -a ,global,d,, $NC_INPUT

ncatted -O -h -a Conventions,global,o,c,"CF-1.4" $NC_INPUT
ncatted -O -h -a title,global,o,c,"GlobAlbedo DHR/BHR Albedo Product" $NC_INPUT
ncatted -O -h -a institution,global,o,c,"Mullard Space Science Laboratory, Department of Space and Climate Physics, University College London" $NC_INPUT
ncatted -O -h -a source,global,o,c,"Satellite observations, BRDF/Albedo Inversion Model" $NC_INPUT
ncatted -O -h -a references,global,o,c,"GlobAlbedo ATBD V3.1" $NC_INPUT
ncatted -O -h -a comment,global,o,c,"none" $NC_INPUT
ncatted -O -h -a history,global,o,c,"GlobAlbedo Processing, 2012-2013" $NC_INPUT  # do this as very last command!!

echo "Done."
