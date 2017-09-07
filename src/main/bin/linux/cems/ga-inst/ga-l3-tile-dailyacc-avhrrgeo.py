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
# - daily accumulation ONLY
#
__author__ = 'olafd'
#############################################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
inversionRootDir = gaRootDir + '/Inversion'
dailyAccRootDir = '/group_workspaces/cems2/qa4ecv/vol3/olafd/GlobAlbedoTest/DailyAccumulators'


#######
modisTileScaleFactor = '6.0'   # for AVHRR+GEO
#######
sensorID = 'avh_geo' # must be one of: 'avh', 'geo', 'avh_geo'
#######

#priorDir = '/group_workspaces/cems2/qa4ecv/vol1/prior.c6/stage2/1km' # next version provided by SK, 20170531
priorDir = gaRootDir + '/Priors/200x200' # own, preprocessed version (downscaled, band subset!) with Said's latest Priors as input

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

# all 326 tiles we have:
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

#tiles = ['h11v08','h11v09','h12v08','h12v09']
#tiles = ['h19v08']

startYear = 2006
endYear = 2010

inputs = ['bbdrs']
m = PMonitor(inputs,
             request='ga-l3-tile-dailyacc-avhrrgeo',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[('ga-l3-tile-dailyacc-avhrrgeo-step.sh',64)] )

##### LOOP over tiles and years: ####
for tile in tiles:

    ### daily accumulation for all years:
    allDailyAccPostConds = []

    # left wing
    year = str(startYear-1)
    startDoy = '273'
    endDoy = '365'
    postCond = 'daily_accs_' + year + '_' + tile
    allDailyAccPostConds.append(postCond)
    m.execute('ga-l3-tile-dailyacc-avhrrgeo-step.sh', ['bbdrs'], [postCond], parameters=[tile,year,startDoy,endDoy,modisTileScaleFactor,gaRootDir,bbdrRootDir,dailyAccRootDir,beamDir])

    # years to process
    for iyear in range(startYear, endYear+1):
        year = str(iyear)
        startDoy = '000'
        endDoy = '365'
        postCond = 'daily_accs_' + year + '_' + tile 
        allDailyAccPostConds.append(postCond)
        m.execute('ga-l3-tile-dailyacc-avhrrgeo-step.sh', ['bbdrs'], [postCond], parameters=[tile,year,startDoy,endDoy,modisTileScaleFactor,gaRootDir,bbdrRootDir,dailyAccRootDir,beamDir])

    # right wing
    year = str(endYear+1)
    startDoy = '001'
    endDoy = '089'
    postCond = 'daily_accs_' + year + '_' + tile
    allDailyAccPostConds.append(postCond)
    m.execute('ga-l3-tile-dailyacc-avhrrgeo-step.sh', ['bbdrs'], [postCond], parameters=[tile,year,startDoy,endDoy,modisTileScaleFactor,gaRootDir,bbdrRootDir,dailyAccRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()
