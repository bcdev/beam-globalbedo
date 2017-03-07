__author__ = 'olafd'

# Adds 'time' as 3rd dimension in 2D NetCDF input raster file. On request of AL, 20170215
# ga-l3-albedo-timedim-python.py ${sensorID} ${tile} ${year} ${iDoy} ${albedoSourceDir} ${albedoTimedimDir}

import sys
import time
import netCDF4

from netCDF4 import Dataset

if len(sys.argv) != 7:
    print 'Usage:  python ga-l3-albedo-timedim-python.py <sensor_id> <tile> <year> <doy> <albedo_source_dir> <albedo_timedim_dir>'
    print 'example call:  python ga-l3-albedo-timedim-python.py avh_geo h18v04 2005 121 /group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest/Albedo/2005/h18v04 /group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest/Albedo_3D/2005/h18v04'
    sys.exit(-1)

sensor_id = sys.argv[1]
tile = sys.argv[2]
year = sys.argv[3]
doy = sys.argv[4]
albedo_source_dir = sys.argv[5]
albedo_timedim_dir = sys.argv[6]

print 'sensor_id: ', sensor_id
print 'tile: ', tile
print 'year: ', year
print 'doy: ', doy
print 'albedo_source_dir: ', albedo_source_dir
print 'albedo_timedim_dir: ', albedo_timedim_dir

infile = albedo_source_dir + '/Qa4ecv.albedo.' + sensor_id  + '.' + year + doy + '.' + tile + '.nc'
outfile = albedo_timedim_dir + '/Qa4ecv.albedo.' + sensor_id  + '.' + year + doy + '.' + tile + '.nc'
timeval = year + doy

print 'infile: ', infile
print 'outfile: ', outfile
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
        dstvar = dst.createVariable(name, variable.datatype, variable.dimensions)
        for attr in variable.ncattrs():
            dstvar.setncattr(attr, getattr(variable, attr))

    # set variable data from src
    for variable in dst.variables:
        print 'variable: ', variable
        dst.variables[variable][:,:] = src.variables[variable][:,:]

    # set time data
    time = dst.createVariable('time', 'i4', ('time'))
    time.setncattr('long_name', 'Product dataset time given as yyyyDOY')
    time.setncattr('standard_name', 'time')
    time[:] = int(timeval) # TODO: discuss, could be yyyyDDD for purpose of sorting with CDO etc.

print 'done'
