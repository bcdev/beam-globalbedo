__author__ = 'daniel'

import featuretools
import warptools
import sys
import epr
import numpy as np

import matplotlib.pyplot as plt

if __name__ == "__main__":

    y_lines = 2048
    y_offset = 20000

    #sift_exe_path = '/home/daniel/coreg_testing/vlfeat-0.9.20/bin/glnxa64/sift '
    sift_exe_path = '/home/globalbedo/od/qa4ecv/coregisteration/vlfeat-0.9.16/bin/glnxa64/sift '

    #test_path = '/home/daniel/coreg_testing/'
    #path_to_ats_data = test_path + "coreg_data/ATS_TOA_1PRUPA20090220_034445_000065272076_00347_36473_5893.N1"
    #temp_dir = test_path + "temp"
    #out_dir = temp_dir + '/' + path_to_ats_data.split("/")[-1][:-3]+"_coreg.nc"

    path_to_ats_data = sys.argv[1]
    temp_dir = sys.argv[2]
    print('path_to_ats_data: ' + path_to_ats_data)
    print('temp_dir: ' + temp_dir)


    ats_product = epr.open(path_to_ats_data)
    ats_nadir = ats_product.get_band("reflec_nadir_0870").read_as_array(512, y_lines, xoffset=0, yoffset=y_offset)
    ats_forward = ats_product.get_band("reflec_fward_0870").read_as_array(512, y_lines, xoffset=0, yoffset=y_offset)

    ats_nadir_cm = ats_product.get_band("cloud_flags_nadir").read_as_array(512, y_lines, xoffset=0, yoffset=y_offset)
    ats_forward_cm = ats_product.get_band("cloud_flags_fward").read_as_array(512, y_lines, xoffset=0, yoffset=y_offset)
    ats_nadir_cm = ats_nadir_cm <= 1
    ats_forward_cm = ats_forward_cm <= 1

    plt.imshow(ats_nadir*ats_nadir_cm, cmap='gray', interpolation='none')
    plt.show()
    plt.close()

    plt.imshow(ats_forward*ats_forward_cm, cmap='gray', interpolation='none')
    plt.show()
    plt.close()

    ats_lats = ats_product.get_band("latitude").read_as_array(512, y_lines, xoffset=0, yoffset=y_offset)
    ats_lons = ats_product.get_band("longitude").read_as_array(512, y_lines, xoffset=0, yoffset=y_offset)

    # create objects
    nadir_feature_extractor = featuretools.FeatureExtractor(sift_exe_path, temp_dir)
    forward_feature_extractor = featuretools.FeatureExtractor(sift_exe_path, temp_dir)
    feature_matcher = featuretools.FeatureMatcher()
    transform_extractor = warptools.WarpDeriver()
    transform_applier = warptools.Warper()

    # extract features
    nadir_feature_extractor.extract(ats_nadir, mask=False)
    forward_feature_extractor.extract(ats_forward, mask=False)

    # if there are features then match them
    if nadir_feature_extractor.locations.size & \
            forward_feature_extractor.locations.size:
        feature_matcher.match(nadir_feature_extractor.locations,
                              nadir_feature_extractor.descriptors,
                              forward_feature_extractor.locations,
                              forward_feature_extractor.descriptors,
                              ats_lats,
                              ats_lons,
                              ats_lats,
                              ats_lons)
    else:
        print "no features matched"

    # check if we have any matching features, if yes then derive warp
    if hasattr(feature_matcher, 'matches'):
        if isinstance(feature_matcher.matches, np.ndarray):
            transform_extractor.get_warp(feature_matcher.matching_locs_a, feature_matcher.matching_locs_b)

    # check if we obtained a transformation
    if hasattr(transform_extractor, 'transform'):
        if isinstance(transform_extractor.transform, np.ndarray):
            transform_applier.apply_warp(ats_nadir.shape, [ats_forward], transform_extractor.transform)

            # pull out the images
            plt.imshow(transform_applier.images_to_warp[0], cmap='gray', interpolation='none')
            plt.show()

        else:
            print "could not derive a warp for images provided"




