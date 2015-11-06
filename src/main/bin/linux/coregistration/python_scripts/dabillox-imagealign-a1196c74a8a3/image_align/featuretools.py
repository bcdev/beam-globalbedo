__author__ = 'daniel'

import numpy as np
import scipy.linalg as linalg
import scipy.spatial as spatial
from PIL import Image
import subprocess
import os


class FeatureExtractor(object):

    def __init__(self,
                 sift_exe_path,
                 temp_dir,
                 edge=5,
                 peak=2,
                 octave=5):
        self.sift_path = sift_exe_path
        self.temp_dir = temp_dir
        self.edge = edge
        self.peak = peak
        self.octave = octave

        self.mask_warning = "No features masking applied... This could lead to poor results if moving features in scene (e.g. clouds)"

    def _is_grayscale(self):
        if np.min(self.image == 0) and np.max(self.image == 255):
            self.image_gray = self.image
        else:
            self._make_grayscale()

    def _make_grayscale(self):
        minPixelValue = np.min(self.image) * 1.0
        maxPixelValue = np.max(self.image) * 1.0
        self.image_gray = ((self.image + minPixelValue)/maxPixelValue)*255

    def _run_extraction(self):
        os.chdir(self.temp_dir)
        image = Image.fromarray(np.uint8(self.image_gray))
        # image.save('tmp.pgm')
        image.save("tmp.jpg", "JPEG")
        params = "--edge-thresh " + str(self.edge) + " --peak-thresh " + str(self.peak) + " --octaves " + str(self.octave)

        #call sift
        try:
            # p = subprocess.call([self.sift_path + 'tmp.pgm' + ' --output=temp.sift ' + params],
            #                     shell=True)

            p = subprocess.call(['/home/globalbedo/od/globalbedo/seaice/coregistered/vlfeat-0.9.16/bin/glnxa64/sift ' + \
                                 'tmp.pgm' + ' --output=temp.sift' + ' --edge-thresh 5 --peak-thresh 2 --octaves 5'], shell=True)

        except Exception, e:
                print "Sift processing failed with error:", e

    def _read_features(self):
        os.chdir(self.temp_dir)
        try:
            f = np.loadtxt('temp.sift')
            self.locations = f[:, :4]
            self.descriptors = f[:, 4:]
            print len(self.locations), "Features detected..."
        except Exception, e:
            self.locations = False
            self.descriptors = False
            print "Failed to load SIFT file with error:", e

    def _mask_features(self):
        try:
            if not issubclass(self.mask.dtype.type, np.bool_):
                print "Feature mask must be boolean... Current type is:", self.mask.dtype.type
                print self.mask_warning
            else:
                self.locations = self.locations[self.mask[self.locations[:, 1].astype('int'),
                                                          self.locations[:, 0].astype('int')], :]
                self.descriptors = self.descriptors[self.mask[self.locations[:, 1].astype('int'),
                                                              self.locations[:, 0].astype('int')], :]
                print len(self.locations), "Features after masking..."
        except Exception, e:
            print "Feature mask checking failed with error:", e
            print self.mask_warning

    def extract(self, image, mask=False):
        self.image = image
        self.mask = mask
        self._is_grayscale()
        self._run_extraction()
        self._read_features()
        if isinstance(self.mask, np.ndarray):
            try:
                self._mask_features()
            except Exception, e:
                print "Feature mask checking failed with error:", e
                print self.mask_warning
        else:
            print self.mask_warning


class FeatureMatcher(object):
    '''
    Currently only works for geographic based arrays.
    '''
    pass

    def __init__(self,
                 match_threshold=0.6,
                 pixelwise_distance=0.5):
        self.match_threshold = match_threshold
        self.pixelwise_distance = pixelwise_distance

    def _extract_geo(self):
        self.lats_a = self.lats_a_grid[self.locs_a[:, 1].astype('int'), self.locs_a[:, 0].astype('int')]
        self.lons_a = self.lons_a_grid[self.locs_a[:, 1].astype('int'), self.locs_a[:, 0].astype('int')]
        self.lats_b = self.lats_b_grid[self.locs_b[:, 1].astype('int'), self.locs_b[:, 0].astype('int')]
        self.lons_b = self.lons_b_grid[self.locs_b[:, 1].astype('int'), self.locs_b[:, 0].astype('int')]
        # get kdtree now for reducing search space
        self.a_kd_tree = spatial.cKDTree(np.dstack([self.lons_a.ravel(), self.lats_a.ravel()])[0])
        self.b_kd_tree = spatial.cKDTree(np.dstack([self.lons_b.ravel(), self.lats_b.ravel()])[0])

    def _match_two_sided(self):
        self._norm_descriptors()
        try:
            matches_ab = self._match_one_sided(self.norm_desc_a, self.norm_desc_b,
                                               self.lats_a, self.lons_a,
                                               self.b_kd_tree)
            matches_ba = self._match_one_sided(self.norm_desc_b, self.norm_desc_a,
                                               self.lats_b, self.lons_b,
                                               self.a_kd_tree)

            ndx_ab = matches_ab.nonzero()[0]
            for n in ndx_ab:
                if matches_ba[int(matches_ab[n])] != n:
                    matches_ab[n] = 0
            if np.max(matches_ab) == 0:
                print "no matching features found..."
                self.matches = False
            else:
                self.matches = np.squeeze(matches_ab)
        except Exception, e:
            print "Two sided matching failed with error:", e
            self.matches = False

    def _match_one_sided(self,
                         norm_desc_r,
                         norm_desc_c,
                         lats_r,
                         lons_r,
                         c_kd_tree):
        matches = np.zeros((norm_desc_r.shape[0], 1), 'int')
        for i, (lat_r, lon_r) in enumerate(zip(lats_r, lons_r)):

            # reduce the search space to only nearby points
            try:
                c_index = c_kd_tree.query_ball_point([lon_r, lat_r], self.pixelwise_distance)
            except Exception, e:
                print "ball tree query failed for a feature point with error:", e,  " Continuing..."
                continue

            # extract reduce search space and check if there is more than 1 point nearby
            nearby_c_descriptors = norm_desc_c[c_index]
            if nearby_c_descriptors.shape[0] <= 1:
                continue

            # compute dot products for reduced search space
            try:
                dot_prod = np.dot(norm_desc_r[i, :], nearby_c_descriptors.T)
                dot_prod = 0.9999*dot_prod
                sorted_dot_prod = np.argsort(np.arccos(dot_prod))
            except Exception, e:
                print "cost computation failed for a feature point:", e,  " Continuing..."
                continue

            # check if nearest neighbor has angle less than dist_ratio times 2nd
            try:
                if np.arccos(dot_prod)[sorted_dot_prod[0]] < \
                                self.match_threshold * np.arccos(dot_prod)[sorted_dot_prod[1]]:
                    matches[i] = c_index[sorted_dot_prod[0]]
            except Exception, e:
                print "A match assignment failed with error:", e,  " Continuing..."
                continue

        return matches

    def _norm_descriptors(self):
        try:
            self.norm_desc_a = np.array([d/linalg.norm(d) for d in self.desc_a])
        except Exception, e:
                print "Normalisation of the a descriptors failed with the error", e
        try:
            self.norm_desc_b = np.array([d/linalg.norm(d) for d in self.desc_b])
        except Exception, e:
            print "Normalisation of the a descriptors failed with the error", e

    def _extract_match_locations(self):
        match_index = self.matches != 0
        self.matching_locs_a = self.locs_a[match_index]
        self.matching_locs_b = self.locs_b[self.matches[match_index]]

    def match(self,
              locs_a,
              desc_a,
              locs_b,
              desc_b,
              lats_a,
              lons_a,
              lats_b,
              lons_b):
        self.locs_a = locs_a
        self.locs_b = locs_b
        self.desc_a = desc_a
        self.desc_b = desc_b
        self.lats_a_grid = lats_a
        self.lons_a_grid = lons_a
        self.lats_b_grid = lats_b
        self.lons_b_grid = lons_b

        self._extract_geo()
        self._match_two_sided()
        if isinstance(self.matches, np.ndarray):
            self._extract_match_locations()

