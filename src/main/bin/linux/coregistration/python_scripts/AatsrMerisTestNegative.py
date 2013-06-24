#!/usr/bin/env python

import os, sys
import numpy as np

from netCDF4 import Dataset
#from Scientific.IO.NetCDF import NetCDFFile

def nowarp_and_write(outDir, merisDS):

    # contains adaptations for netcdf writing with Scientific.IO.NetCDF

    #Generate the output nc dataset
    rootgrp = Dataset(outDir, 'w')
    #    print 'shape: ', merisDS.variables['latitude'][:,:].shape[0]
    y = merisDS.variables['latitude'][:, :].shape[0]
    x = merisDS.variables['latitude'][:, :].shape[1]
    rootgrp.createDimension('y', y)
    rootgrp.createDimension('x', x)
    latitudes = rootgrp.createVariable('latitude_S','f4',('y','x'), zlib=True, complevel=1)
    latitudes[:,:] = merisDS.variables['latitude'][:,:]
    longitudes = rootgrp.createVariable('longitude_S','f4',('y','x'), zlib=True, complevel=1)
    longitudes[:,:] = merisDS.variables['longitude'][:,:]

    if hasattr(merisDS.variables['radiance_9'], 'scale_factor'):
        print 'bla'

    print 'radiance_9: ', merisDS.variables['radiance_9']
    scaleFactor = merisDS.variables['radiance_9'].getncattr('scale_factor')
    print 'radiance_9 scaleFactor: ', scaleFactor
    print 'radiance_9 attrs: ', merisDS.variables['radiance_9'].ncattrs()
    print 'radiance_9 value: ', ((merisDS.variables['radiance_9'][1679,420] / scaleFactor) + 65536.0) * scaleFactor

    #place the meris data into the file
    for variable in merisDS.variables:
        # print 'MERIS variable: ', variable
        if variable == "radiance_9":
            #variable.getncattr('scale_factor')
            scaleFactor = merisDS.variables[variable].getncattr('scale_factor')
            print 'scaleFactor: ', scaleFactor
            myarray = np.zeros(shape=(y,x))
            temp = rootgrp.createVariable(variable + '_M','f4',('y','x'), zlib=True, complevel=1)
            temp[:,:] = merisDS.variables[variable][:,:] / scaleFactor
            myarray[:,:] = temp[:,:]
            myarray[myarray<=0.0] += 65536.0
            temp[:,:] = myarray[:,:] * scaleFactor;

    #closet the output
    rootgrp.close()

def main():
    #set paths
    aatsrDir = sys.argv[1]
    aatsrData = sys.argv[2]
    merisDir = sys.argv[3]
    merisData = sys.argv[4]
    workingDir = sys.argv[5]
    outDir = sys.argv[6] + aatsrData[:-3] + '_test_negative.nc'

    #read in the nc files
    merisDS = Dataset(merisDir + merisData, 'r')

    #warp and write out
    print '#write out...'
    nowarp_and_write(outDir, merisDS)

    # cleanup...
    print '#cleanup...'
    if os.path.exists('meris.sift'):
        os.remove('meris.sift')
    if os.path.exists('aatsr.sift'):
        os.remove('aatsr.sift')
    if os.path.exists('tmp.pgm'):
        os.remove('tmp.pgm')

    print '--> processing completed succesfully.'
    return

if __name__ == '__main__':
    sys.exit(main())
