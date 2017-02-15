__author__ = 'olafd'

import os
import sys
import subprocess
import commands
import time
import netCDF4

from netCDF4 import Dataset

#if len(sys.argv) != 2:
#    print 'Usage:  python clean_alllakes_per_year.py 2005 <year>'
#    print 'example call:  python clean_alllakes_per_year.py 2005'
#    sys.exit(-1)

infile = sys.argv[1]
outfile = sys.argv[2]

time = 1

with Dataset(infile) as src, Dataset(outfile, 'w', format='NETCDF4') as dst:

    dst.createDimension('time', None)

    #x = len(src.dimensions['x'])
    #y = len(src.dimensions['y'])
    #dst.createDimension('y', y)
    #dst.createDimension('x', x)

    for name, dimension in src.dimensions.iteritems():
        dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)

    for name, variable in src.variables.iteritems():
        dstvar = dst.createVariable(name, variable.datatype, variable.dimensions)
        for attr in variable.ncattrs():
            dstvar.setncattr(attr, getattr(variable, attr))

    for attr in src.ncattrs():
        dst.setncattr(attr, getattr(src, attr))

    for variable in dst.variables:
        print 'variable: ', variable
        dst.variables[variable][:,:] = src.variables[variable][:,:]

    time = dst.createVariable('time', 'i4', ('time'))
    time[:] = 1234


print 'done'

