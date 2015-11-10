__author__ = 'daniel'

import sys

import featuretools
import warptools
import epr
from   netCDF4 import Dataset
import numpy as np

import matplotlib.pyplot as plt

def find_overlap(ats_prod, mer_prod):
    ats_lat = ats_prod.get_band("latitude").read_as_array()[:,:]
    ats_lon = ats_prod.get_band("longitude").read_as_array()[:,:]
    mer_lat = mer_prod.get_band("latitude").read_as_array()[:,:]
    mer_lon = mer_prod.get_band("longitude").read_as_array()[:,:]

    min_mer_lat = np.min(mer_lat)
    max_mer_lat = np.max(mer_lat)
    min_mer_lon = np.min(mer_lon)
    max_mer_lon = np.max(mer_lon)

    y, x = np.where((ats_lat >= min_mer_lat ) & (ats_lat <= max_mer_lat) &
                    (ats_lon >= min_mer_lon) & (ats_lon <= max_mer_lon))

    return np.min(y), np.max(y)

def write_as_netcdf(image_data):

    root_grp = Dataset('./test.nc', 'w', format='NETCDF4')
    root_grp.description = 'Example coregistration data'

    # dimensions
    xdim = np.shape(image_data)[0]
    ydim = np.shape(image_data)[1]
    root_grp.createDimension('y', ydim)
    root_grp.createDimension('x', xdim)

    # variables
    y = root_grp.createVariable('y', 'f4', ('y',))
    x = root_grp.createVariable('x', 'f4', ('x',))
    reflec = root_grp.createVariable('reflec_nadir_0670', 'f4', ('y', 'x'))

    # data
    ydata =  np.arange(ydim)
    xdata =  np.arange(xdim)
    y[:] = ydata
    x[:] = xdata
    for j in range(ydim):
        for i in range(ydim):
            reflec[j,i] = image_data[j, i]

    root_grp.close()

if __name__ == "__main__":

    sift_exe_path = '/home/daniel/coreg_testing/vlfeat-0.9.20/bin/glnxa64/sift '

    # path = '/home/daniel/coreg_testing/'
    # path_to_ats_data = path + "coreg_data/ATS_TOA_1PRUPA20090220_034445_000065272076_00347_36473_5893.N1"
    # path_to_mer_data = path + "coreg_data/MER_RR__1PNACR20090220_044807_000007882076_00348_36474_0000.N1"
    # temp_dir = path + "temp"
    # out_dir = temp_dir + '/' + path_to_ats_data.split("/")[-1][:-3]+"_coreg.nc"

    path = sys.argv[1]
    merisData = sys.argv[2]
    aatsrData = sys.argv[3]

    path_to_ats_data = path + "AATSR/" + aatsrData
    path_to_mer_data = path + "MERIS/" + merisData
    temp_dir = path + "temp"
    out_dir = temp_dir + '/' + aatsrData + "_coreg.nc"

    #open ATS and MER product files
    ats_product = epr.open(path_to_ats_data)
    mer_product = epr.open(path_to_mer_data)

    #find overlapping geographic subset (will be limited by the MER data)
    start_y, end_y = find_overlap(ats_product, mer_product)

    # load in meris data
    ats_chan = ats_product.get_band("reflec_nadir_0670").read_as_array(512, end_y-start_y,
                                                                       xoffset=0, yoffset=start_y)
    mer_chan = mer_product.get_band("radiance_7").read_as_array()

    ats_lats = ats_product.get_band("latitude").read_as_array(512, end_y-start_y, xoffset=0, yoffset=start_y)
    ats_lons = ats_product.get_band("longitude").read_as_array(512, end_y-start_y, xoffset=0, yoffset=start_y)
    mer_lats = mer_product.get_band("latitude").read_as_array()
    mer_lons = mer_product.get_band("longitude").read_as_array()

    # create objects
    ats_feature_extractor = featuretools.FeatureExtractor(sift_exe_path, temp_dir)
    mer_feature_extractor = featuretools.FeatureExtractor(sift_exe_path, temp_dir)
    feature_matcher = featuretools.FeatureMatcher()
    transform_extractor = warptools.WarpDeriver()
    transform_applier = warptools.Warper()

    # extract features
    ats_feature_extractor.extract(ats_chan, mask=False)
    mer_feature_extractor.extract(mer_chan, mask=False)

    # if there are features then match them
    if ats_feature_extractor.locations.size & \
            mer_feature_extractor.locations.size:
        feature_matcher.match(ats_feature_extractor.locations,
                              ats_feature_extractor.descriptors,
                              mer_feature_extractor.locations,
                              mer_feature_extractor.descriptors,
                              ats_lats,
                              ats_lons,
                              mer_lats,
                              mer_lons)
    else:
        print "no features matched"

    # check if we have any matching features, if yes then derive warp
    if hasattr(feature_matcher, 'matches'):
        if isinstance(feature_matcher.matches, np.ndarray):
            transform_extractor.get_warp(feature_matcher.matching_locs_a, feature_matcher.matching_locs_b)

    # check if we obtained a transformation
    if hasattr(transform_extractor, 'transform'):
        if isinstance(transform_extractor.transform, np.ndarray):
            transform_applier.apply_warp(mer_chan.shape, ats_chan, transform_extractor.transform)

            # pull out the images
            plt.imshow(transform_applier.resampled_image, cmap='gray', interpolation='none')
            plt.show()

            # test: write as netcdf
            # write_as_netcdf(transform_applier.resampled_image)

        else:
            print "could not derive a warp for images provided"



