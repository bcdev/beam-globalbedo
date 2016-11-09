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
# - daily accumulation
# - full accumulation, inversion, BRDF to albedo conversion
# - cleanup (i.e. of daily accumulators)
# TEST VERSION for ALLYEARS: 
# - now outer loop over tiles
# - now inner loop over years 1998-2014
# - per PMonitor execute of dailyacc and inversion steps, many LSF jobs are initiated.
#
__author__ = 'olafd'
#############################################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
inversionRootDir = gaRootDir + '/Inversion'

#######
modisTileScaleFactor = '6.0'   # for AVHRR+GEO
#######
usePrior = 'true'
#######
step = '8'  # always!
#######

priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6.v2/stage2/1km' # latest version by SK, 20161011
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

# all 326 tiles we have:
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

#tiles = ['h17v03']
#tiles = ['h20v06']
tiles = ['h20v08']
#tiles = ['h18v04','h20v06','h22v05','h19v08']
#tiles = glob.glob1(priorDir, 'h??v05') # we have same number (326) of snow and noSnow prior directories
#tiles = ['h18v03','h21v06']

inputs = ['bbdrs']
m = PMonitor(inputs,
             request='ga-l3-tile-inversion-albedo-avhrrgeo_allyears',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh',64),
                     ('ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh',64),
                     ('ga-l3-tile-inversion-cleanup-step.sh',2) ] )


##### LOOP over tiles and years: ####
for tile in tiles:

    ### daily accumulation for all years:
    allDailyAccPostConds = []
    for iyear in range(1998, 2015):
#    for iyear in range(1998, 2000):
        year = str(iyear)

        startDoy = '000'
        endDoy = '361'
        postCond = 'daily_accs_' + year 
        allDailyAccPostConds.append(postCond)
        m.execute('ga-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh', ['bbdrs'], [postCond], parameters=[tile,year,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

    ### now full accumulation, inversion and albedo:
    allAlbedoPostConds = []
    for iyear in range(1998, 2015):
#    for iyear in range(1998, 2000):
        year = str(iyear)
        albedoDir = gaRootDir + '/Albedo/' + year + '/' + tile
        startDoy = '001'
        endDoy = '365'

        # this will be executed when all accumulation jobs completed successfully.
        # However, as one of those PMonitor jobs initiates several LSF accumulation jobs, it is not guaranteed with the current
        # setup that all of those LSB jobs are finished at this time, i.e. that all binariy accumulator files have been written.
        # This must be checked in a waiting loop in ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh before inversion/albedo jobs are started.

        postCond = 'albedo_' + year + '_' + tile
        allAlbedoPostConds.append(postCond)
        m.execute('ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh', allDailyAccPostConds, [postCond], parameters=[tile,year,startDoy,endDoy,gaRootDir,bbdrRootDir,inversionRootDir,usePrior,priorDir,beamDir,modisTileScaleFactor,albedoDir])

        #########################################################################################################################

    ### cleanup (i.e. daily acc binary files):
    for iyear in range(1998, 2015):
#    for iyear in range(1998, 2000):
        year = str(iyear)
        m.execute('ga-l3-tile-inversion-cleanup-step.sh', allAlbedoPostConds, ['dummy'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
