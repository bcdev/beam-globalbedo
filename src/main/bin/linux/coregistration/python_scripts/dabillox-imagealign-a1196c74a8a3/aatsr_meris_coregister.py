#!/usr/bin/env python

import os, sys
import CoregisterImages
import numpy as np
import epr

def main():
    
    #set paths 
    #aatsrDir = sys.argv[1]
    #aatsrData = sys.argv[2] 
    #merisDir = sys.argv[3]
    #merisData = sys.argv[4] 
    #tempDir = sys.argv[5] 
    #outDir = sys.argv[6] + aatsrData[:-3] + '_warped.nc'
    
    path_to_ats_data = "/media/INTENSO/Data/AATSR/RAL_project/coreg_data/ATS_TOA_1PRUPA20090220_034445_000065272076_00347_36473_5893.N1" 
    path_to_mer_data = "/media/INTENSO/Data/AATSR/RAL_project/coreg_data/MER_RR__1PNACR20090220_044807_000007882076_00348_36474_0000.N1"
    temp_dir = "/home/daniel/orac/coreg_images/temp"
    cloud_masking=False
    out_dir = temp_dir + '/' + path_to_ats_data.split("/")[-1][:-3]+"_coreg.nc"
    print out_dir
    
    #open ATS and MER product files
    ats_product = epr.open(path_to_ats_data)
    mer_product = epr.open(path_to_mer_data)
    
    #find overlapping geographic subset (will be limited by the MER data)
    start_y, end_y = CoregisterImages.find_overlap(ats_product, mer_product)
    
    #get the ATS and MER data to be warping
    ats_chan = ats_product.get_band("reflec_nadir_0670").read_as_array(512, end_y-start_y,
                                                              xoffset=0, yoffset=start_y)
    mer_chan = mer_product.get_band("radiance_7").read_as_array()
    
    #convert to to be warped images to 0-255 range
    ats_8_bit = CoregisterImages.convert_to_grayscale(ats_chan)
    mer_8_bit = CoregisterImages.convert_to_grayscale(mer_chan)
    
    #change into temp directory
    os.chdir(temp_dir)
    
    #extract features
    #CoregisterImages.process_image(ats_8_bit,'aatsr.sift')
    #CoregisterImages.process_image(mer_8_bit,'meris.sift')
    
    #load features
    l1,d1 = CoregisterImages.read_features_from_file('aatsr.sift')
    l2,d2 = CoregisterImages.read_features_from_file('meris.sift')
    
    #cloud mask both feature sets 
    if cloud_masking:
        try: 
            l1,d1 = CoregisterImages.cloud_mask(l1,d1, ats_product.get_band("cloud_flags_nadir").read_as_array(512, end_y-start_y,
                                                                                                               xoffset=0, yoffset=start_y))
        except:
            print "cloud masking failed (try running without cloud masking - accuracy will be decreased), exiting"
            return
            
        if l1 == None:
            print "No tie-points detected, exiting"
            return
        
        if l1.shape[0] < 10:
            print "Less than 10 tie-point detected, exiting"
            return 
    
    #match the feature sets
    print "looking for matches across", d2.shape[0],"locations, this may take a while"
    matches = CoregisterImages.match_twosided(d1,d2)
    
    #extract the matching locations
    matches = np.squeeze(matches)
    matchIndex = matches != 0
    print "total of",np.sum(matchIndex),"matching locations detected"
    locs1 = l1[matchIndex,0:2]
    locs2 = l2[matches[matchIndex], 0:2]
    
    #derive the warp
    print "deriving warp"
    warpParams = CoregisterImages.get_warp(locs1, locs2)
    
    #warp and write out
    print "warping and writing out"
    CoregisterImages.warp_and_write(out_dir, ats_product, mer_product, warpParams, start_y, end_y)
    
    print 'processing completed succesfully'

if __name__ == '__main__':
    sys.exit(main())
