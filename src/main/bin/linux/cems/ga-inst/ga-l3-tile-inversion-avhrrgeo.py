import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

#############################################################################################
#BBDR --> BRDF --> Albedo startup script for AVHRR and GEO instruments (AVHRR, MVIRI, SEVIRI)
# Invokes steps:
# - full accumulation, inversion, BRDF to albedo conversion
#
__author__ = 'olafd'
#############################################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
#bbdrRootDir = gaRootDir + '/BBDR'
dailyAccRootDir = '/group_workspaces/cems2/qa4ecv/vol3/olafd/GlobAlbedoTest/DailyAccumulators'
inversionRootDir = gaRootDir + '/Inversion'

#######
modisTileScaleFactor = '6.0'   # for AVHRR+GEO
#######
usePrior = 'true'
#######
sensorID = 'avh_geo' # must be one of: 'avh', 'geo', 'avh_geo'
#######

#priorDir = '/group_workspaces/cems2/qa4ecv/vol1/prior.c6/stage2/1km' # next version provided by SK, 20170531
priorDir = gaRootDir + '/Priors/200x200' # own, preprocessed version (downscaled, band subset!) with Said's latest Priors as input

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

# all 326 tiles we have:
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

tiles = ['h25v03','h25v09','h26v07']
#tiles = ['h18v04']

startYear = 2010
endYear = 2010

inputs = ['dailyaccs']
m = PMonitor(inputs,
             request='ga-l3-tile-inversion-avhrrgeo',
             logdir='log',
             hosts=[('localhost',64)],
             types=[ ('ga-l3-tile-inversion-avhrrgeo-step.sh',64) ] )

##### LOOP over tiles and years: ####
for tile in tiles:

    ### full accumulation, inversion and albedo:
    allAlbedoPostConds = []
    for iyear in range(startYear, endYear+1):
        year = str(iyear)
        startDoy = '001'
        endDoy = '365'

        #startDoy = '121'  # test
        #endDoy = '122'

        postCond = 'albedo_' + year + '_' + tile
        allAlbedoPostConds.append(postCond)
        m.execute('ga-l3-tile-inversion-avhrrgeo-step.sh', 
                  ['dailyaccs'], 
                  [postCond], 
                  parameters=[sensorID,tile,year,startDoy,endDoy,gaRootDir,dailyAccRootDir,inversionRootDir,usePrior,priorDir,beamDir,modisTileScaleFactor])

# wait for processing to complete
m.wait_for_completion()
