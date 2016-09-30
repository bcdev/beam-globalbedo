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


year = '2005'    #test  --> we should run this with  a single year only! todo: make configurable

leftyear = str(int(year)-1)
rightyear = str(int(year)+1)

######################## BBDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
#gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#tiles = ['h19v06','h25v06','h12v09']
#tiles = ['h18v03']
tiles = ['h25v06']

leftyear = str(int(year)-1)
rightyear = str(int(year)+1)

inputs = ['bbdrs']
m = PMonitor(inputs, 
             request='ga-l3-tile-inversion2-albedo',
             logdir='log', 
             hosts=[('localhost',250)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-dailyacc-step.sh',250), 
                     ('ga-l3-tile-inversion-fullacc-step.sh',250), 
                     ('ga-l3-tile-inversion2-albedo-step.sh',250)] )

### daily accumulation for processing year and left/right wings

for tile in tiles:
    dailyAccs = []

    doys=['097','105','113']
    # center year daily accs:
    for doy in doys:
        noSnowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/NoSnow/PROCESSED_ALL_' + doy
        snowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/Snow/PROCESSED_ALL_' + doy
        dailyAccs.append(noSnowDailyAcc)
        dailyAccs.append(snowDailyAcc)

        noSnowBinFile =  gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/NoSnow/matrices_' + year + doy + '.bin'
        snowBinFile =  gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/Snow/matrices_' + year + doy + '.bin'

        #if (not os.path.exists(noSnowBinFile) or not os.path.exists(snowBinFile)):
        m.execute('ga-l3-tile-inversion-dailyacc-step.sh', ['bbdrs'], dailyAccs, parameters=[tile,year,doy,gaRootDir,bbdrRootDir,beamDir])

    ### full accumulation and inversion now in one step:

    albedoProducts = []
    albedoDir = gaRootDir + '/Albedo/' + year + '/' + tile

    # classic setup (8-day albedos)
    doys=['105']
    for doy in doys:
        # albedo product e.g.: GlobAlbedoTest/Albedo/2005/h18v04/GlobAlbedo.albedo.2005097.h18v04.nc
        albedoProduct = gaRootDir + '/Albedo/' + year + '/' + tile + '/GlobAlbedo.albedo.' + year + doy + '.' + tile + '.nc'
        albedoProducts.append(albedoProduct)
        #m.execute('ga-l3-tile-inversion2-albedo-step.sh', ['dummy'], albedoProducts, parameters=[tile,year,doy,gaRootDir,priorDir,beamDir,albedoDir]) ## test!!!
        m.execute('ga-l3-tile-inversion2-albedo-step.sh', dailyAccs, albedoProducts, parameters=[tile,year,doy,gaRootDir,priorDir,beamDir,albedoDir])

    # cleanup:
    m.execute('ga-l3-tile-inversion-cleanup-step.sh', albedoProducts, ['dummy'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
