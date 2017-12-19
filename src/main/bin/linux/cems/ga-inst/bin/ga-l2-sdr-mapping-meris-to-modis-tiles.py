import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

########################################################################
#
# SPECTRAL BRDF 
# STEP 1: MERIS SDR mapped to MODIS SDR
#         Input: 1200x1200 SIN tile
#         Output: 1200x1200 SIN tile
# STEP 2: MODIS SDR 1200x1200 tile split into 300x300 SIN tiles
#         Input: 1200x1200 SIN tile
#         Output: 300x300 SIN tiles
#
# --> no longer step 2 (20171212)
#
########################################################################

__author__ = 'olafd'

#years = ['2005']
years = ['2007']

#sensors = ['MERIS'] # start with
#sensors = ['VGT'] # start with
sensors = ['MERIS','VGT'] # final

#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6/stage2/1km' # another change by SK, 20161115. NO LONGER EXISTING, 20170522
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/500m' # this is preliminary, 20170522
# all 326 tiles we have:
#tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories

#tiles = ['h20v06'] # start with
#tiles = ['h18v04'] # start with
#tiles = ['h25v06'] # start with
tiles = ['h18v04','h22v02','h19v08','h25v06'] # final

#subtileStartX = ['0','300','600','900']
#subtileStartY = ['0','300','600','900']

######################## SDR MERIS --> MODIS: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
#m = PMonitor(inputs, 
#             request='ga-l2-sdr-mapping-meris-to-modis-tiles',
#             logdir='log', 
#             hosts=[('localhost',192)],
#             types=[('ga-l2-sdr-mapping-meris-to-modis-tiles-step.sh',64),
#                    ('ga-l2-sdr-modis-tiles-to-subtiles-step.sh',128)])
m = PMonitor(inputs,
             request='ga-l2-sdr-mapping-meris-to-modis-tiles',
             logdir='log',
             hosts=[('localhost',128)],
             types=[('ga-l2-sdr-mapping-meris-to-modis-tiles-step.sh',128)])


for year in years:
    for sensor in sensors:
        for tile in tiles:
            sdrTileDir = gaRootDir + '/SDR/' + sensor + '/' + year + '/' + tile
            sdrModisTileDir = gaRootDir + '/SDR_spectral/' + sensor + '/' + year + '/' + tile
            if os.path.exists(sdrTileDir):
                sdrFiles = os.listdir(sdrTileDir)
                if len(sdrFiles) > 0:
                    for index in range(0, len(sdrFiles)):
                    #for index in range(0, 10):  # test!
                        # we do not want other files than *.nc or *.nc.gz:
	                if sdrFiles[index].endswith(".nc") or sdrFiles[index].endswith(".nc.gz"):
                            sdrPath = sdrTileDir + '/' + sdrFiles[index]
                            mappingPostCond = sdrFiles[index] 
                            m.execute('ga-l2-sdr-mapping-meris-to-modis-tiles-step.sh', ['dummy'], [mappingPostCond], 
                                                                                parameters=[sdrPath,sdrFiles[index],sdrModisTileDir,sensor,gaRootDir,beamDir])

                            #sdrModisFilename = os.path.splitext(sdrFiles[index])[0] + '_mapped.nc'
                            #sdrModisPath = sdrModisTileDir + '/' + sdrModisFilename
                            #m.execute('ga-l2-sdr-modis-tiles-to-subtiles-step.sh',
                            #                  [mappingPostCond], ['dummy2'],
                            #                  parameters=[sdrModisPath,sdrModisFilename,sdrModisTileDir,sensor,gaRootDir,beamDir])

m.wait_for_completion()

