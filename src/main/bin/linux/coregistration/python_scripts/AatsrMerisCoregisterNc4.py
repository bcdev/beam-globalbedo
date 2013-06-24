#!/usr/bin/env python

import os, sys
import CoregisterImagesNc4
#import matplotlib.pyplot as plt
#import NPT.NonParametricTransform
#import NPT.NonParametricMatcher
import numpy as np
#import esa

from netCDF4 import Dataset
#from Scientific.IO.NetCDF import NetCDFFile


def main():
    #set paths
    aatsrDir = sys.argv[1]
    aatsrData = sys.argv[2]
    merisDir = sys.argv[3]
    merisData = sys.argv[4]
    workingDir = sys.argv[5]
    outDir = sys.argv[6] + aatsrData[:-3] + '_warped.nc'

    print 'aatsrDir: ', aatsrDir
    print 'merisDir: ', merisDir
    print 'aatsrData: ', aatsrData
    print 'merisData: ', merisData
    print 'workingDir: ', workingDir
    print 'outDir: ', outDir

    #read in the nc files
    aatsrDS = Dataset(aatsrDir + aatsrData, 'r')
    merisDS = Dataset(merisDir + merisData, 'r')

    #get the warping data (some negative meris values - not sure why)
    aatsr = aatsrDS.variables['reflec_nadir_0670'][:, :]
    #    meris = np.abs(merisDS.variables['reflec_7'][:,:])
    meris = np.abs(merisDS.variables['radiance_7'][:, :])

    #convert to to be warped images to 0-255 range
    aatsr8bit = CoregisterImagesNc4.convert_to_grayscale(aatsr)
    meris8bit = CoregisterImagesNc4.convert_to_grayscale(meris)

    #change into working directory
    os.chdir(workingDir)

    #get the features
    print '#get the features...'
    p1 = CoregisterImagesNc4.process_image(aatsr8bit, 'aatsr.sift')
    p2 = CoregisterImagesNc4.process_image(meris8bit, 'meris.sift')

    print '#reading features...'
    #read in the features
    l1, d1 = CoregisterImagesNc4.read_features_from_file('aatsr.sift')
    l2, d2 = CoregisterImagesNc4.read_features_from_file('meris.sift')

    #mask off any non-ice features points 
    print '#mask off any non-ice features points...'
    l1, d1, icemask = CoregisterImagesNc4.ice_mask(l1, d1, aatsrDS)

    #match the features
    print '#match the features...'
    matches = CoregisterImagesNc4.match_twosided(d1, d2)

    #extract the matching locations
    print '#extract the matching locations...'
    matches = np.squeeze(matches)
    matchIndex = matches != 0
    locs1 = l1[matchIndex, 0:2]
    locs2 = l2[matches[matchIndex], 0:2]

    #derive the warp
    print '#derive the warp...'
    warpParams = CoregisterImagesNc4.get_warp(locs1, locs2)

    #warp and write out
    print '#warp and write out...'
    CoregisterImagesNc4.warp_and_write(outDir, aatsrDS, merisDS, warpParams)

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
