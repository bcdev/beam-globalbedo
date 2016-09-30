#!/bin/bash

dayDir=$1
year=$2
month=$3
month02=$4
day=$5
day02=$6
version=$7

if [ ! -d "$dayDir" ]
then
   mkdir -p $dayDir
fi

rootUrl=http://www.vito-eodata.be/PDF/datapool/Free_Data/PROBA-V_1km/S1_TOA_-_1_km/
urlSuffix=${year}/${month}/${day}/PV_S1_TOA-${year}${month02}${day02}_1KM_V${version}/
echo "time wget -P ${dayDir}/ -r -A '*.HDF5' -e robots=off -r --no-parent -nH --cut-dirs=9 --user=oda666 --password=oda666_at_bc ${rootUrl}${urlSuffix}"
time wget -P ${dayDir}/ -r -A '*.HDF5' -e robots=off -r --no-parent -nH --cut-dirs=9 --user=oda666 --password=oda666_at_bc ${rootUrl}${urlSuffix}
