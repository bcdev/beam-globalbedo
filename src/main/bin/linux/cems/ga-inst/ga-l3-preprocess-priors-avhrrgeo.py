import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

#############################################################################################
# MODIS Priors preprocessing script for AVHRRGEO albedo retrieval.
# Invokes steps:
# - downscaling from 1200x1200 to 200x200 products
# - band subsetting: target products contain only broadband bands (vis, nir, shortwave)
#
__author__ = 'olafd'
#############################################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
preprocessedPriorDir = gaRootDir + '/Priors'

#######
scaleFactor = '6.0'   # for AVHRR+GEO
#######

priorDir = '/group_workspaces/cems2/qa4ecv/vol1/prior.c6/stage2/1km' # next version provided by SK, 20170531

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

# all 326 tiles we have:
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

#tiles = ['h18v04'] # test
#tiles = ['h18v03'] # test

inputs = ['priors']
m = PMonitor(inputs,
             request='ga-l3-preprocess-priors-avhrrgeo',
             logdir='log',
             hosts=[('localhost',128)],
             types=[('ga-l3-preprocess-priors-avhrrgeo-step.sh',128)] )


##### LOOP over tiles and doys: ####
for tile in tiles:
    for idoy in range(0,365):
        doy = str(idoy+1).zfill(3) # daily
        postCond = 'prior_preprocess_' + doy + '_' + tile 
        m.execute('ga-l3-preprocess-priors-avhrrgeo-step.sh', ['priors'], 
                                                              [postCond], 
                                                              parameters=[tile,doy,scaleFactor,gaRootDir,priorDir,beamDir])

# wait for processing to complete
m.wait_for_completion()
