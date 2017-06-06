import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

########################################################################
#
# SPECTRAL BRDF 
# STEP 1: MERIS/VGT SDR mapped to MODIS SDR
#         Input: 1200x1200 SIN tile
#         Output: 1200x1200 SIN tile
#
########################################################################

__author__ = 'olafd'

years = ['2005']
months = ['01', '02','03', '04','05', '06','07', '08','09', '10','11', '12']
#months = ['02']

#sensors = ['MERIS'] # start with
sensors = ['VGT'] # start with
#sensors = ['MERIS','VGT'] # final

#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6/stage2/1km' # another change by SK, 20161115. NO LONGER EXISTING, 20170522
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/500m' # this is preliminary, 20170522
# all 326 tiles we have:
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories

#tiles = ['h18v02']
#tiles = ['h30v11']

######################## SDR MERIS/VGT --> MODIS: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs,
             request='ga-l2-sdr-mapping-to-modis-tiles',
             logdir='log',
             hosts=[('localhost',128)],
             types=[('ga-l2-sdr-mapping-to-modis-tiles-step.sh',128)])

# process one month at once (per pmonitor and bjob)
for year in years:
    for month in months:
        subInterval = year + month
        for sensor in sensors:
            for tile in tiles:
                sdrTileDir = gaRootDir + '/SDR/' + sensor + '/' + year + '/' + tile
                sdrModisTileDir = gaRootDir + '/SDR_spectral/' + sensor + '/' + year + '/' + tile
                if os.path.exists(sdrTileDir):
                    #sdrFiles = os.listdir(sdrTileDir)
                    sdrFileFilter = '*' + subInterval + '*'
                    sdrFiles = glob.glob1(sdrTileDir, sdrFileFilter)
                    if len(sdrFiles) > 0:
                        allSdrPaths = ''
                        for index in range(0, len(sdrFiles)):
                            # we do not want other files than *.nc in given month
	                    if subInterval in sdrFiles[index] and sdrFiles[index].endswith(".nc"):
                                sdrPath = sdrTileDir + '/' + sdrFiles[index] + ','  # NOTE: this works with any separator (we use a comma), but NOT with a semicolon!!
                                allSdrPaths += sdrPath
                                
                        mappingPostCond = sdrFiles[len(sdrFiles)-1] 
                        #print 'len(sdrFiles): ', str(len(sdrFiles))
                        #print 'PYTHON allSdrPaths: ', allSdrPaths
			#print 'sdrFiles[0]: ', sdrFiles[0]
                        m.execute('ga-l2-sdr-mapping-to-modis-tiles-step.sh', ['dummy'], [mappingPostCond], 
                                                                                parameters=[allSdrPaths,sdrModisTileDir,tile,subInterval,sensor,gaRootDir,beamDir])

m.wait_for_completion()

