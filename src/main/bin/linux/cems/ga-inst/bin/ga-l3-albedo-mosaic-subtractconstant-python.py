__author__ = 'olafd'

# Subtracts constant value from DHR/BHR albedos. On request of JPM, 20180306
# ga-l3-albedo-mosaic-subtractconstant-python.py ${sensorID} ${year} ${iDoy} ${res} ${snowMode} ${albedoSourceDir} ${albedoSubtractedDir}

import sys
import time
import datetime
import netCDF4

from netCDF4 import Dataset

if len(sys.argv) != 8:
    print 'Usage:  python ga-l3-albedo-mosaic-subtractconstant-python.py <sensor_id> <year> <doy> <res> <snowMode> <albedo_source_dir> <albedo_subtracted_dir>'
    print 'example call:  python ga-l3-albedo-mosaic-subtractconstant-python.py avh_geo 2005 121 05 Merge /group_workspaces/cems2/qa4ecv/vol4/olafd/qa4ecv_archive/qa4ecv/albedo/L3_Mosaic_Merge/v1.1/2005 /group_workspaces/cems2/qa4ecv/vol4/olafd/qa4ecv_archive/qa4ecv/albedo/L3_Mosaic_Merge/v1.2/2005'
    sys.exit(-1)

constant = 0.12 # test !!

sensor_id = sys.argv[1]
year = sys.argv[2]
doy = sys.argv[3]
res = sys.argv[4]
snowMode = sys.argv[5]
albedo_source_dir = sys.argv[6]
albedo_subtracted_dir = sys.argv[7]

print 'sensor_id: ', sensor_id
print 'year: ', year
print 'doy: ', doy
print 'res: ', res
print 'snowMode: ', snowMode
print 'albedo_source_dir: ', albedo_source_dir
print 'albedo_subtracted_dir: ', albedo_subtracted_dir

infile = albedo_source_dir + '/QA4ECV-L3-Mosaic-Albedo-' + snowMode  + '-' + res + '-' + year + doy + '.nc'
outfile = albedo_subtracted_dir + '/QA4ECV-L3-Mosaic-Albedo-' + snowMode  + '-' + res + '-' + year + doy + '.nc'

print 'infile: ', infile
print 'outfile: ', outfile
print 'year: ', year
print 'doy: ', doy
#timeval = year + doy

# use days since 1970-01-01 instead:
productDate = datetime.datetime(int(year), 1, 1) + datetime.timedelta(int(doy) - 1)
month = productDate.month
day = productDate.day
timeval = (datetime.datetime(int(year),month,day)  - datetime.datetime(1970,1,1)).days

print 'timeval: ', timeval

with Dataset(infile) as src, Dataset(outfile, 'w', format='NETCDF4') as dst:

    # set new time dimension:
    #time = 1
    #dst.createDimension('time', None)

    # set dimensions from src:
    for name, dimension in src.dimensions.iteritems():
        dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)

    # set global attributes from src
    for attr in src.ncattrs():
        dst.setncattr(attr, getattr(src, attr))

    # set variable attributes from src
    for name, variable in src.variables.iteritems():
        if name != 'time':
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            for attr in variable.ncattrs():
                dstvar.setncattr(attr, getattr(variable, attr))

    # set variable data from src
    for variable in dst.variables:
        print 'variable: ', variable
        if variable == "BHR_VIS" or variable == "BHR_NIR" or variable == "BHR_SW" or variable == "DHR_VIS" or variable == "DHR_NIR" or variable == "DHR_SW":
            dst.variables[variable][:,:] = src.variables[variable][:,:] - constant
        else:
            dst.variables[variable][:,:] = src.variables[variable][:,:]

    # set time data
    time = dst.createVariable('time', 'i4', ('time'), zlib=True)
    time.setncattr('long_name', 'Product dataset time given as days since 1970-01-01')
    time.setncattr('standard_name', 'time')
    time.setncattr('units', 'days since 1970-01-01')
    time[:] = int(timeval)

print 'done'
