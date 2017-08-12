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
#step = '8'  # always!
step = '24'  # check this!
#######
sensorID = 'avh_geo' # must be one of: 'avh', 'geo', 'avh_geo'
#######

#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6.v2/stage2/1km' # latest version by SK, 20161011
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6/stage2/1km' # another change by SK, 20161115
#priorDir = '/group_workspaces/cems2/qa4ecv/vol1/prior.c6/stage2/1km' # next version provided by SK, 20170531

###
priorDir = gaRootDir + '/Priors/200x200' # own, preprocessed version (downscaled, band subset!) with Said's latest Priors as input
###

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

# all 326 tiles we have:
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
#tiles = glob.glob1(priorDir, 'h1?v0?') # test, 77 tiles, done
#tiles = glob.glob1(priorDir, 'h1?v1?') # test, 38 tiles
#tiles = glob.glob1(priorDir, 'h2?v1?') # test, 38 tiles, done
#tiles = glob.glob1(priorDir, 'h3?v??') # test
tiles.sort()

#tiles = ['h11v08','h11v09','h12v08','h12v09']
#tiles = ['h17v17']
#tiles = ['h18v04']
#tiles = ['h18v03']
#tiles = ['h16v00','h17v00']
#tiles = ['h18v04','h20v06','h22v05','h19v08']

# Antarctica:
#tiles =          ['h15v15','h19v15','h20v15','h21v15','h22v15',
#         'h14v16','h15v16','h16v16','h17v16','h18v16','h19v16','h20v16','h21v16','h22v16','h23v16',
#                  'h15v17','h16v17','h17v17','h18v17','h19v17','h20v17'
#]

# Africa:
#tiles =          ['h17v06','h18v06','h19v06','h20v06','h21v06',
#                  'h17v07','h18v07','h19v07','h20v07','h21v07',
#                  'h17v08','h18v08','h19v08','h20v08','h21v08',
#                                    'h19v09','h20v09','h21v09',
#                                    'h19v10','h20v10','h21v10'
#]

# parts of South America, Canada, Greenland (2004 bad data fix, 20170210):
#tiles =          ['h12v08','h12v09','h13v09','h12v10','h13v10',
#                  'h12v02','h13v02','h12v03',
#                  'h16v01','h16v02']

#startYear = 1998
#endYear = 2014
### processed 2016/12, 2017/01: 2012,2011,2010,2009,2008,2007,2006,2003,2002,2001,2000,1999,1998
### processed 2017/04: 2000-2010
### processed 2017/06: 2001, 2002, 2003, 2004, 2006 
startYear = 1989
endYear = 1989

inputs = ['bbdrs']
m = PMonitor(inputs,
             request='ga-l3-tile-inversion-albedo-avhrrgeo_allyears',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-dailyacc-avhrrgeo-step.sh',64),
                     #('ga-l3-tile-inversion-albedo-avhrrgeo-step.sh',64),
                     ('ga-l3-tile-inversion-albedo-avhrrgeo-step_2.sh',64),
                     ('ga-l3-tile-inversion-cleanup-step.sh',2) ] )


##### LOOP over tiles and years: ####
for tile in tiles:

    ### daily accumulation for all years:
    allDailyAccPostConds = []

    # left wing
    year = str(startYear-1)
    startDoy = '273'
    endDoy = '361'
    postCond = 'daily_accs_' + year + '_' + tile
    allDailyAccPostConds.append(postCond)
    m.execute('ga-l3-tile-inversion-dailyacc-avhrrgeo-step.sh', ['bbdrs'], [postCond], parameters=[tile,year,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

    # years to process
    for iyear in range(startYear, endYear+1):
        year = str(iyear)
        startDoy = '000'
        endDoy = '361'
        postCond = 'daily_accs_' + year + '_' + tile 
        allDailyAccPostConds.append(postCond)
        m.execute('ga-l3-tile-inversion-dailyacc-avhrrgeo-step.sh', ['bbdrs'], [postCond], parameters=[tile,year,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

    # right wing
    year = str(endYear+1)
    startDoy = '001'
    endDoy = '089'
    postCond = 'daily_accs_' + year + '_' + tile
    allDailyAccPostConds.append(postCond)
    m.execute('ga-l3-tile-inversion-dailyacc-avhrrgeo-step.sh', ['bbdrs'], [postCond], parameters=[tile,year,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])


    ### now full accumulation, inversion and albedo:
    allAlbedoPostConds = []
    for iyear in range(startYear, endYear+1):
        year = str(iyear)
        #albedoDir = gaRootDir + '/Albedo/' + sensorID + '/' + year + '/' + tile
        startDoy = '001'
        endDoy = '365'

        # this will be executed when all accumulation jobs completed successfully.
        # However, as one of those PMonitor jobs initiates several LSF accumulation jobs, it is not guaranteed with the current
        # setup that all of those LSB jobs are finished at this time, i.e. that all binariy accumulator files have been written.
        # This must be checked in a waiting loop in ga-l3-tile-inversion-albedo-avhrrgeo-step.sh before inversion/albedo jobs are started.

        postCond = 'albedo_' + year + '_' + tile
        allAlbedoPostConds.append(postCond)
        #m.execute('ga-l3-tile-inversion-albedo-avhrrgeo-step.sh', allDailyAccPostConds, [postCond], parameters=[sensorID,tile,year,startDoy,endDoy,gaRootDir,bbdrRootDir,inversionRootDir,usePrior,priorDir,beamDir,modisTileScaleFactor])
        m.execute('ga-l3-tile-inversion-albedo-avhrrgeo-step_2.sh', allDailyAccPostConds, [postCond], parameters=[sensorID,tile,year,startDoy,endDoy,gaRootDir,bbdrRootDir,inversionRootDir,usePrior,priorDir,beamDir,modisTileScaleFactor])

        #########################################################################################################################

    ### cleanup (i.e. daily acc binary files):
    #for iyear in range(startYear, endYear+1):
    #    year = str(iyear)
    #    m.execute('ga-l3-tile-inversion-cleanup-step.sh', allAlbedoPostConds, ['dummy'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
