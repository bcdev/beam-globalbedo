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

# Antarctica/Greenland Prior BRDF issue:
#tiles = ['h16v00','h17v00','h16v01','h17v01','h15v02','h16v02','h17v02',
#         'h15v15','h19v15','h20v15','h21v15','h22v15','h23v15',
#         'h14v16','h15v16','h16v16','h17v16','h18v16','h19v16','h20v16','h21v16','h22v16','h23v16',
#         'h15v17','h16v17','h17v17','h18v17','h19v17','h20v17']
#tiles = ['h27v08']
#tiles = ['h20v01','h20v02']
#tiles = ['h24v02','h24v03']

startYear = 1994
endYear = 1994

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
