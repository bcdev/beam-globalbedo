import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

###########################################################################################
#BBDR --> BRDF --> Albedo startup script for LEO instruments (MERIS, VGT, PROBA-V [, AATSR])
# Invokes steps:
# - daily accumulation
# - full accumulation, inversion, BRDF to albedo conversion
# - cleanup (i.e. of daily accumulators)
#
# TEST VERSION: per PMonitor execute of ga-l3-tile-inversion-dailyacc-leo-step.sh,
# ga-l3-tile-inversion-albedo-leo-step.sh, many LSF jobs are initiated.
#
# --> TODO: we need a mechanism to do this in subsequent subsets of tiles (e.g. 10)
# which must be completed and daily acc binary files deleted before next subset starts,
# otherwise the binary files kill our disk space in case of 1200x1200 LEO tiles
#
# --> TODO 2: prepare a version which runs over all LEO years (1998-2014), but per single tile
# In this case we should have a maximum of ~15TB of temporary binary files which should be feasible
#
__author__ = 'olafd'
###########################################################################################


######################## BBDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
inversionRootDir = gaRootDir + '/Inversion_leo'

#######
modisTileScaleFactor = '1.0'  # for LEO
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
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6.v2/stage2/1km' # latest version by SK, 20161011
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6/stage2/1km' # another change by SK, 20161115
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
#doys = ['161'] # test!

# all 326 tiles we have:
#tiles = glob.glob1(priorDir + '/Snow', 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

tiles = ['h19v08']

inputs = ['bbdrs']
left_accs_name = 'dailyaccs_left'
center_accs_name = 'dailyaccs_center'
right_accs_name = 'dailyaccs_right'

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-albedo-leo',
             logdir='log',
             hosts=[('localhost',96)], # let's try this number for 1200x1200 LEO...
             types=[ ('ga-l3-tile-inversion-dailyacc-leo-step.sh',72),
                     ('ga-l3-tile-inversion-albedo-leo-step.sh',24)] )


#years = ['2014'] # VGT input Jan-May
#years = ['2014'] # PROBA-V input only

years = ['2011']

for year in years:
    leftyear = str(int(year)-1)
    rightyear = str(int(year)+1)


    ### daily accumulation for processing year and left/right wings

    for tile in tiles:

        # left wing year daily accs:
        startDoy='273'
        endDoy = '361'
        m.execute('ga-l3-tile-inversion-dailyacc-leo-step.sh', ['bbdrs'], [left_accs_name], parameters=[tile,leftyear,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

        # right wing year daily accs:
        startDoy='000'
        endDoy = '097'
        m.execute('ga-l3-tile-inversion-dailyacc-leo-step.sh', ['bbdrs'], [right_accs_name], parameters=[tile,rightyear,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

        # center year daily accs (after completion of wings):
        startDoy = '000'
        endDoy = '361'
        wing_accs_names = [left_accs_name, right_accs_name]
        m.execute('ga-l3-tile-inversion-dailyacc-leo-step.sh', ['bbdrs'], [center_accs_name], parameters=[tile,year,startDoy,endDoy,step,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

        all_accs_names = [left_accs_name, center_accs_name, right_accs_name]

        ### full accumulation, inversion and albedo now in one step:

        albedoDir = gaRootDir + '/Albedo_leo/' + year + '/' + tile

        #########################################################################################################################
        if mode == '8DAY':
            # CLASSIC setup (8-DAY albedos)
            endDoy = '361' 

        if mode == 'DAILY':
            # NEW setup: DAILY albedos for whole year (we have Priors now for each single day) 
            endDoy = '365' 

        startDoy = '001'

        # this will be executed when all three accumulation jobs (left, center, right year) completed successfully.
        # However, as one of those PMonitor jobs initiates several LSF accumulation jobs, it is not guaranteed with the current
        # setup that all of those LSB jobs are finished at this time, i.e. that all binariy accumulator files have been written.
        # This must be checked in a waiting loop in ga-l3-tile-inversion-albedo-leo-step.sh before inversion/albedo jobs are started.

        m.execute('ga-l3-tile-inversion-albedo-leo-step.sh', all_accs_names, ['dummy1'], parameters=[tile,year,startDoy,endDoy,gaRootDir,bbdrRootDir,inversionRootDir,usePrior,priorDir,beamDir,modisTileScaleFactor,albedoDir])

        #########################################################################################################################

        # cleanup:
        m.execute('ga-l3-tile-inversion-cleanup-step.sh', ['dummy1'], ['dummy2'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
