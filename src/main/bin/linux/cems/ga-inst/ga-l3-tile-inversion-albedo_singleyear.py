import glob
import os
import os.path
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

##################################################################################
def isPolarTile(tile):
    extensions = ['v00','v01','v02','v15','v16','v17']

    for extension in extensions:
        if tile.endswith(extension):
            return True

    return False
##################################################################################


#year = '2005'
#year = '2006'
#year = '2014' # VGT input Jan-May
year = '2015' # PROBA-V input only
#year = '2014' # PROBA-V input only

leftyear = str(int(year)-1)
rightyear = str(int(year)+1)

######################## BBDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
#gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
inversionRootDir = gaRootDir + '/Inversion'
modisTileScaleFactor = '1.0'  # for LEO
#modisTileScaleFactor = '6.0'   # for GEO
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km'
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km'
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

#doys = ['001', '121', '129', '361'] # test!
#doys = ['121','129'] # test!
#doys = ['161'] # test!
#doys = ['121'] # test!
#doys = ['193'] # test!
#doys = ['185'] # test!
#doys = ['097'] # test!
#doys = ['001'] # test!

# all 326 tiles we have:
#tiles = glob.glob1(priorDir + '/Snow', 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

#tiles = ['h18v06','h19v08']
#tiles = ['h18v03']
#tiles = ['h18v04']
tiles = ['h19v06']
#tiles = ['h20v06']
#tiles = ['h25v06']
#tiles = ['h00v08']

leftyear = str(int(year)-1)
rightyear = str(int(year)+1)

inputs = ['bbdrs']
m = PMonitor(inputs, 
             request='ga-l3-tile-inversion-albedo',
             logdir='log', 
             hosts=[('localhost',250)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-dailyacc-step.sh',250), 
                     ('ga-l3-tile-inversion-fullacc-step.sh',250), 
                     ('ga-l3-tile-inversion-albedo-step.sh',250)] )

### daily accumulation for processing year and left/right wings

for tile in tiles:
    dailyAccs = []

    # left wing year daily accs:
    leftdoys = []
    if isPolarTile(tile):
        for i in range(24):
            doy = 177 + 8*i  # Jul,Aug,Sep,Oct,Nov,Dec
            leftdoys.append(str(doy).zfill(3))
    else:
        for i in range(12):
            doy = 273 + 8*i  # Oct,Nov,Dec, sufficient in this case
            leftdoys.append(str(doy).zfill(3))

    for doy in leftdoys:
        #noSnowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + leftyear + '/' + tile + '/NoSnow/PROCESSED_ALL_' + doy
        #snowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + leftyear + '/' + tile + '/Snow/PROCESSED_ALL_' + doy
        #dailyAccs.append(noSnowDailyAcc)
        #dailyAccs.append(snowDailyAcc)

        dailyAccs = [gaRootDir + '/BBDR/DailyAcc/' + leftyear + '/' + tile + '/NoSnow/PROCESSED_ALL',
                     gaRootDir + '/BBDR/DailyAcc/' + leftyear + '/' + tile + '/Snow/PROCESSED_ALL']

        # binary files e.g.:
        # /group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest/BBDR/DailyAcc/2005/h18v04/NoSnow/matrices_2005259.bin
        # /group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest/BBDR/DailyAcc/2005/h18v04/Snow/matrices_2005259.bin
        # --> gaRootDir + '/BBDR/DailyAcc/ + leftyear + '/' + tile + '/NoSnow/matrices_' + leftyear + doy + '.bin'
        # --> gaRootDir + '/BBDR/DailyAcc/ + leftyear + '/' + tile + '/Snow/matrices_' + leftyear + doy + '.bin'
        # TODO: if both files exist, do not execute!
        noSnowBinFile =  gaRootDir + '/BBDR/DailyAcc/' + leftyear + '/' + tile + '/NoSnow/matrices_' + leftyear + doy + '.bin'
        snowBinFile =  gaRootDir + '/BBDR/DailyAcc/' + leftyear + '/' + tile + '/Snow/matrices_' + leftyear + doy + '.bin'

        #if (not os.path.exists(noSnowBinFile) or not os.path.exists(snowBinFile)):
        m.execute('ga-l3-tile-inversion-dailyacc-step.sh', ['bbdrs'], dailyAccs, parameters=[tile,leftyear,doy,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

    # center year daily accs:
    centerdoys = []
    for i in range(46):
        doy = 8*i + 1
        centerdoys.append(str(doy).zfill(3))
    for doy in centerdoys:
        #noSnowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/NoSnow/PROCESSED_ALL_' + doy
        #snowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/Snow/PROCESSED_ALL_' + doy
        #dailyAccs.append(noSnowDailyAcc)
        #dailyAccs.append(snowDailyAcc)

        dailyAccs = [gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/NoSnow/PROCESSED_ALL',
                     gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/Snow/PROCESSED_ALL']

        noSnowBinFile =  gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/NoSnow/matrices_' + year + doy + '.bin'
        snowBinFile =  gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/Snow/matrices_' + year + doy + '.bin'

        #if (not os.path.exists(noSnowBinFile) or not os.path.exists(snowBinFile)):
        m.execute('ga-l3-tile-inversion-dailyacc-step.sh', ['bbdrs'], dailyAccs, parameters=[tile,year,doy,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])

    # right wing year daily accs:
    rightdoys = []
    if isPolarTile(tile):
        for i in range(24):
            doy = 8*i + 1  # Jan,Feb,Mar,Apr,May,Jun
            rightdoys.append(str(doy).zfill(3))
    else:
        for i in range(12):
            doy = 8*i + 1  # Jan,Feb,Mar, sufficient in this case
            rightdoys.append(str(doy).zfill(3))

    for doy in rightdoys:
        #noSnowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + rightyear + '/' + tile + '/NoSnow/PROCESSED_ALL_' + doy
        #snowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + rightyear + '/' + tile + '/Snow/PROCESSED_ALL_' + doy
        #dailyAccs.append(noSnowDailyAcc)
        #dailyAccs.append(snowDailyAcc)

        dailyAccs = [gaRootDir + '/BBDR/DailyAcc/' + rightyear + '/' + tile + '/NoSnow/PROCESSED_ALL',
                     gaRootDir + '/BBDR/DailyAcc/' + rightyear + '/' + tile + '/Snow/PROCESSED_ALL']

        noSnowBinFile =  gaRootDir + '/BBDR/DailyAcc/' + rightyear + '/' + tile + '/NoSnow/matrices_' + rightyear + doy + '.bin'
        snowBinFile =  gaRootDir + '/BBDR/DailyAcc/' + rightyear + '/' + tile + '/Snow/matrices_' + rightyear + doy + '.bin'

        #if (not os.path.exists(noSnowBinFile) or not os.path.exists(snowBinFile)):
        m.execute('ga-l3-tile-inversion-dailyacc-step.sh', ['bbdrs'], dailyAccs, parameters=[tile,rightyear,doy,modisTileScaleFactor,gaRootDir,bbdrRootDir,beamDir])


    ### full accumulation and inversion now in one step:

    albedoProducts = []
    albedoDir = gaRootDir + '/Albedo/' + year + '/' + tile

    # CLASSIC setup (8-DAY albedos)
    for doy in doys:
        # albedo product e.g.: GlobAlbedoTest/Albedo/2005/h18v04/GlobAlbedo.albedo.2005097.h18v04.nc
        albedoProduct = gaRootDir + '/Albedo/' + year + '/' + tile + '/GlobAlbedo.albedo.' + year + doy + '.' + tile + '.nc'
        albedoProducts.append(albedoProduct)

    for doy in doys:
        m.execute('ga-l3-tile-inversion-albedo-step.sh', dailyAccs, albedoProducts, parameters=[tile,year,doy,gaRootDir,bbdrRootDir,inversionRootDir,priorDir,beamDir,modisTileScaleFactor,albedoDir])
    # end CLASSIC setup

    # NEW setup: DAILY albedos for whole year (for the moment we assume that 8-day Priors are representative for each single day) 
    #for doy in range(364):
    #    # albedo product e.g.: GlobAlbedoTest/Albedo/2005/h18v04/GlobAlbedo.albedo.2005097.h18v04.nc
    #    doystring = str(doy+1).zfill(3)
    #    albedoProduct = gaRootDir + '/Albedo/' + year + '/' + tile + '/GlobAlbedo.albedo.' + year + doystring + '.' + tile + '.nc'
    #    albedoProducts.append(albedoProduct)

    #for doy in range(364):
    #    doystring = str(doy+1).zfill(3)
    #    m.execute('ga-l3-tile-inversion-albedo-step.sh', dailyAccs, albedoProducts, parameters=[tile,year,doystring,gaRootDir,bbdrRootDir,inversionRootDir,priorDir,beamDir,modisTileScaleFactor,albedoDir])
    # end NEW setup

    # cleanup:
    m.execute('ga-l3-tile-inversion-cleanup-step.sh', albedoProducts, ['dummy'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
