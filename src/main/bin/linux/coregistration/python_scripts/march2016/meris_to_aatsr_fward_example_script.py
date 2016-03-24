__author__ = 'daniel'

import featuretools
import warptools
import sys
import epr
import numpy as np

import matplotlib.pyplot as plt

def find_overlap(ats_prod, mer_prod,  mer_start_y, mer_end_y):
    ats_lat = ats_prod.get_band("latitude").read_as_array()
    ats_lon = ats_prod.get_band("longitude").read_as_array()
    mer_lat = mer_prod.get_band("latitude").read_as_array(1120, mer_end_y-mer_start_y, xoffset=0, yoffset=mer_start_y)
    mer_lon = mer_prod.get_band("longitude").read_as_array(1120, mer_end_y-mer_start_y, xoffset=0, yoffset=mer_start_y)

    min_mer_lat = np.min(mer_lat)
    max_mer_lat = np.max(mer_lat)
    min_mer_lon = np.min(mer_lon)
    max_mer_lon = np.max(mer_lon)

    y, x = np.where((ats_lat >= min_mer_lat ) & (ats_lat <= max_mer_lat) &
                    (ats_lon >= min_mer_lon) & (ats_lon <= max_mer_lon))

    return np.min(y), np.max(y)


if __name__ == "__main__":

    #sift_exe_path = '/home/daniel/coreg_testing/vlfeat-0.9.20/bin/glnxa64/sift '
    sift_exe_path = '/home/globalbedo/od/qa4ecv/coregisteration/vlfeat-0.9.16/bin/glnxa64/sift '

    #path = '/home/daniel/coreg_testing/'
    #path_to_ats_data = path + "coreg_data/ATS_TOA_1PRUPA20050501_090404_000065272036_00479_16565_3602.N1"
    #path_to_mer_data = path + "coreg_data/MER_RR__1PRACR20050501_092849_000026372036_00480_16566_0000.N1"
    #temp_dir = path + "temp"
    #out_dir = temp_dir + '/' + path_to_ats_data.split("/")[-1][:-3]+"_coreg.nc"

    path_to_ats_data = sys.argv[1]
    path_to_mer_data = sys.argv[2]
    temp_dir = sys.argv[3]
    print('path_to_ats_data: ' + path_to_ats_data)
    print('path_to_mer_data: ' + path_to_mer_data)
    print('temp_dir: ' + temp_dir)


    #open ATS and MER product files
    ats_product = epr.open(path_to_ats_data)
    mer_product = epr.open(path_to_mer_data)

    # mer start/stop
    mer_start_y, mer_end_y = 5000, 6000

    #find overlapping geographic subset (will be limited by the MER data)
    start_y, end_y = find_overlap(ats_product, mer_product, mer_start_y, mer_end_y)

    # load in meris data
    ats_chan = ats_product.get_band("reflec_fward_0670").read_as_array(512, end_y-start_y,
                                                                       xoffset=0, yoffset=start_y)
    mer_chan = mer_product.get_band("radiance_7").read_as_array(1120, mer_end_y-mer_start_y, xoffset=0, yoffset=mer_start_y)

    ats_lats = ats_product.get_band("latitude").read_as_array(512, end_y-start_y, xoffset=0, yoffset=start_y)
    ats_lons = ats_product.get_band("longitude").read_as_array(512, end_y-start_y, xoffset=0, yoffset=start_y)
    mer_lats = mer_product.get_band("latitude").read_as_array(1120, mer_end_y-mer_start_y, xoffset=0, yoffset=mer_start_y)
    mer_lons = mer_product.get_band("longitude").read_as_array(1120, mer_end_y-mer_start_y, xoffset=0, yoffset=mer_start_y)

    # create objects
    ats_feature_extractor = featuretools.FeatureExtractor(sift_exe_path, temp_dir)
    mer_feature_extractor = featuretools.FeatureExtractor(sift_exe_path, temp_dir)
    feature_matcher = featuretools.FeatureMatcher()
    transform_extractor = warptools.WarpDeriver()
    transform_applier = warptools.Warper()

    # extract features (NOTE: for AATSR you should use and Cloud mask and a DEM mask,
    # using only low elevation features <500m to prevent parallax effects)
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
            print "transform derived succesfully"
            print "Warp RMSE:", transform_extractor.cp_rmse_warped

            # plt.imshow(transform_applier.resampled_image, cmap='gray', interpolation='none')
            # plt.show()

            # Write/return warped images from transform_applier.resampled_image

        else:
            print "could not derive a warp for images provided"



