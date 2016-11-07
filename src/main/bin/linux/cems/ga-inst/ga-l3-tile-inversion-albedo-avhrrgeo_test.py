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
# TEST VERSION: per PMonitor execute of ga-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh,
# ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh, many LSF jobs are initiated.
#
__author__ = 'olafd'
#############################################################################################

##################################################################################
def isPolarTile(tile):
    extensions = ['v00','v01','v02','v15','v16','v17']

    for extension in extensions:
        if tile.endswith(extension):
            return True

    return False
######################## BBDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
#gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
inversionRootDir = gaRootDir + '/Inversion'

#######
modisTileScaleFactor = '6.0'   # for AVHRR+GEO
#######

usePrior = 'true'

#######
mode = 'DAILY'  # new QA4ECVsetup, time and disk space consuming!
#mode = '8DAY'  # classic GA setup

step = '8'  # always!
#######

#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km'
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km'
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.mcd43a.c5.broadband/1km' # moved by SK, 20160930?!
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6/stage2/snownosnow/tile' # first version of new C6 daily priors
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6.v2/stage2/1km' # latest version by SK, 20161011, does not yet work
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

doys = []
for i in range(46): # one year
#for i in range(4):   # one month
#for i in range(1):   # one day
    doy = 8*i + 1
    #doy = 8*i + 97    # April
    #doy = 8*i + 129  # May
    #doy = 8*i + 337  # Dec
    doys.append(str(doy).zfill(3))

#doys = ['001', '180', '365'] # test!
#doys = ['121','129'] # test!
#doys = ['121'] # test!

# all 326 tiles we have:
#tiles = glob.glob1(priorDir + '/Snow', 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

#tiles = ['h18v08','h19v08','h20v08','h18v03','h22v06','h22v07']
#tiles = ['h18v03']
#tiles = ['h18v04','h16v02','h17v16','h19v08']
#tiles = ['h19v02']

years = ['2005']

inputs = ['bbdrs']

left_accs_name = 'dailyaccs_left'
center_accs_name = 'dailyaccs_center'
right_accs_name = 'dailyaccs_right'

#m = PMonitor(inputs, 
#             request='ga-l3-tile-inversion-albedo-avhrrgeo_test',
#             logdir='log', 
#             hosts=[('localhost',192)], # let's try this number...
#             types=[ ('ga-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh',192), 
#                     ('ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh',192)] )

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-albedo-avhrrgeo_test',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh',64),
                     ('ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh',64)] )


for year in years:
    leftyear = str(int(year)-1)
    rightyear = str(int(year)+1)


    ### daily accumulation for processing year and left/right wings

    for tile in tiles:

        # left wing year daily accs:
        endDoy = '361'
        if isPolarTile(tile):
            startDoy = '177'
        else:
            startDoy='273'
        m.execute('ga-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh', ['bbdrs'], [left_accs_name], parameters=[tile,leftyear,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

        # right wing year daily accs:
        startDoy='000'
        if isPolarTile(tile):
            endDoy = '193'
        else:
            endDoy = '097'
        m.execute('ga-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh', ['bbdrs'], [right_accs_name], parameters=[tile,rightyear,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

        # center year daily accs (after completion of wings):
        startDoy = '000'
        endDoy = '361'
        m.execute('ga-l3-tile-inversion-dailyacc-avhrrgeo_test-step.sh', ['bbdrs'], [center_accs_name], parameters=[tile,year,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

        all_accs_names = [left_accs_name, center_accs_name, right_accs_name]

        ### full accumulation, inversion and albedo now in one step:

        albedoDir = gaRootDir + '/Albedo/' + year + '/' + tile

        #########################################################################################################################
        if mode == '8DAY':
            # CLASSIC setup (8-DAY albedos)
            endDoy = '365'

        if mode == 'DAILY':
            # NEW setup: DAILY albedos for whole year (we have Priors now for each single day)
            endDoy = '365'

        startDoy = '001'

        # this will be executed when all three accumulation jobs (left, center, right year) completed successfully.
        # However, as one of those PMonitor jobs initiates several LSF accumulation jobs, it is not guaranteed with the current
        # setup that all of those LSB jobs are finished at this time, i.e. that all binariy accumulator files have been written.
        # This must be checked in a waiting loop in ga-l3-tile-inversion-albedo-leo_test-step.sh before inversion/albedo jobs are started.

        albedo_name = 'albedo_' + year + '_' + tile
        m.execute('ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh', all_accs_names, [albedo_name], parameters=[tile,year,startDoy,endDoy,gaRootDir,bbdrRootDir,inversionRootDir,usePrior,priorDir,beamDir,modisTileScaleFactor,albedoDir])

        #########################################################################################################################

        # cleanup:
        m.execute('ga-l3-tile-inversion-cleanup-step.sh', [albedo_name], ['dummy'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
