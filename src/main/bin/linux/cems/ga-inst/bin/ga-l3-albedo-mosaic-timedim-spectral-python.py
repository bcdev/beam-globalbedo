__author__ = 'olafd'

# Adds 'time' as 3rd dimension in 2D NetCDF input raster file. On request of AL, 20170215
# ga-l3-albedo-mosaic-timedim-python.py ${year} ${iDoy} ${res} ${snowMode} ${albedoSourceDir} ${albedoTimedimDir}

import sys
import time
import datetime
import netCDF4

from netCDF4 import Dataset

if len(sys.argv) != 7:
    print 'Usage:  python ga-l3-albedo-mosaic-timedim-spectral-python.py <year> <doy> <res> <snowMode> <albedo_source_dir> <albedo_timedim_dir>'
    print 'example call:  python ga-l3-albedo-mosaic-timedim-spectral-python.py 2005 121 05 NoSnow /group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest/Mosaic/Albedo_spectral/NoSnow/2005 /group_workspaces/cems2/qa4ecv/vol4/olafd/qa4ecv_archive/qa4ecv/albedo_spectral/L3_Mosaic_NoSnow/v0.9/2005'
    sys.exit(-1)

year = sys.argv[1]
doy = sys.argv[2]
res = sys.argv[3]
snowMode = sys.argv[4]
albedo_source_dir = sys.argv[5]
albedo_timedim_dir = sys.argv[6]

print 'year: ', year
print 'doy: ', doy
print 'res: ', res
print 'snowMode: ', snowMode
print 'albedo_source_dir: ', albedo_source_dir
print 'albedo_timedim_dir: ', albedo_timedim_dir

infile = albedo_source_dir + '/Qa4ecv.albedo.spectral.' + snowMode + '.' + res + '.' + year + doy + '.' + 'PC.nc'
outfile = albedo_timedim_dir + '/QA4ECV-L3-Mosaic-Albedo-' + snowMode  + '-' + res + '-' + year + doy + '.nc'

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
    time = 1
    dst.createDimension('time', None)

    # set dimensions from src:
    for name, dimension in src.dimensions.iteritems():
        dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)

    # set global attributes from src
    for attr in src.ncattrs():
        dst.setncattr(attr, getattr(src, attr))

    # set variable attributes from src
    for name, variable in src.variables.iteritems():
        if name != "metadata" and name != "crs":
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            for attr in variable.ncattrs():
                dstvar.setncattr(attr, getattr(variable, attr))

    # set variable data from src
    for variable in dst.variables:
        print 'variable: ', variable
        if variable == "lat" or variable == "lon":
            dst.variables[variable][:] = src.variables[variable][:]
        else:
            dst.variables[variable][:,:] = src.variables[variable][:,:]

    # set time data
    time = dst.createVariable('time', 'i4', ('time'), zlib=True)
    time.setncattr('long_name', 'Product dataset time given as days since 1970-01-01')
    time.setncattr('standard_name', 'time')
    time.setncattr('units', 'days since 1970-01-01')
    time[:] = int(timeval)

print 'done'
