__author__ = 'olafd'

# Adds 'time' as 3rd dimension in 2D NetCDF input raster file. On request of AL, 20170215

import os
import sys
import time
import netCDF4

from netCDF4 import Dataset

if len(sys.argv) != 4:
    print 'Usage:  python add_timedim_to_nc.py <nc_input> <nc_result> <timestring>'
    print 'example call:  python add_timedim_to_nc.py Qa4ecv.albedo.avhrrgeo.NoSnow.005.2001327.PC.nc 2001327'
    sys.exit(-1)

infile = sys.argv[1]
outfile = sys.argv[2]
timeval = sys.argv[3]

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

