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

# Antarctica/Greenland Prior BRDF issue:
#tiles = ['h16v00','h17v00','h16v01','h17v01','h15v02','h16v02','h17v02',
#         'h15v15','h19v15','h20v15','h21v15','h22v15','h23v15',
#         'h14v16','h15v16','h16v16','h17v16','h18v16','h19v16','h20v16','h21v16','h22v16','h23v16',
#         'h15v17','h16v17','h17v17','h18v17','h19v17','h20v17']
#tiles = ['h17v16']
#tiles = ['h17v01']
#tiles = ['h24v02','h24v03']
tiles = ['h03v10','h03v06','h19v04','h11v07','h07v05','h08v07','h11v02','h17v13','h13v08','h25v02']  # reprocess incomplete jobs

startYear = 1994
endYear = 1994

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
        endDoy = '366'

        #startDoy = '129'  # test, gap filling
        #endDoy = '192'

        postCond = 'albedo_' + year + '_' + tile
        allAlbedoPostConds.append(postCond)
        m.execute('ga-l3-tile-inversion-avhrrgeo-step.sh', 
                  ['dailyaccs'], 
                  [postCond], 
                  parameters=[sensorID,tile,year,startDoy,endDoy,gaRootDir,dailyAccRootDir,inversionRootDir,usePrior,priorDir,beamDir,modisTileScaleFactor])

# wait for processing to complete
m.wait_for_completion()
