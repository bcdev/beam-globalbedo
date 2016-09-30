import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

### same as ga-l3-tile-inversion-albedo.py, but executes only daily accumulation step!


##################################################################################
def isPolarTile(tile):
    extensions = ['v00','v01','v02','v15','v16','v17']

    for extension in extensions:
        if tile.endswith(extension):
            return True

    return False
##################################################################################


#year = sys.argv[1]   # in [1997, 2010]
#years = ['2001']    #test  
#years = ['2003']    #test  
year = '2005'    #test  --> we should run this with  a single year only! todo: make configurable
sensors = ['MERIS','VGT']
#sensors = ['MERIS']
#sensors = ['VGT']

leftyear = str(int(year)-1)
rightyear = str(int(year)+1)

######################## BBDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km'
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

doys = []
for i in range(46): # one year
#for i in range(4):   # one month
#for i in range(1):   # one day
    doy = 8*i + 1
    #doy = 8*i + 129  # May
    #doy = 8*i + 337  # Dec
    doys.append(str(doy).zfill(3))

doys = ['169'] # test!

# all 326 tiles we have:
tiles = glob.glob1(priorDir + '/Snow', 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

tiles = ['h18v05']

bbdrDirs = []
for sensor in sensors:
    leftyear = str(int(year)-1)
    rightyear = str(int(year)+1)
    for tile in tiles:
        bbdrDirs.append(gaRootDir + '/BBDR/' + sensor + '/' + year + '/' + tile)
        bbdrDirs.append(gaRootDir + '/BBDR/' + sensor + '/' + leftyear + '/' + tile)
        bbdrDirs.append(gaRootDir + '/BBDR/' + sensor + '/' + rightyear + '/' + tile)

inputs = ['bbdrs']
m = PMonitor(inputs, 
             request='ga-l3-tile-inversion-dailyacc',
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
        noSnowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + leftyear + '/' + tile + '/NoSnow/PROCESSED_ALL_' + doy
        snowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + leftyear + '/' + tile + '/Snow/PROCESSED_ALL_' + doy
        dailyAccs.append(noSnowDailyAcc)
        dailyAccs.append(snowDailyAcc)

        # --> TODO: provide list of sensors as parameter!
        m.execute('ga-l3-tile-inversion-dailyacc-step.sh', ['bbdrs'], dailyAccs, parameters=[tile,leftyear,doy,gaRootDir,bbdrRootDir,beamDir])

    # center year daily accs:
    centerdoys = []
    for i in range(46):
        doy = 8*i + 1
        centerdoys.append(str(doy).zfill(3))
    for doy in centerdoys:
        noSnowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/NoSnow/PROCESSED_ALL_' + doy
        snowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + year + '/' + tile + '/Snow/PROCESSED_ALL_' + doy
        dailyAccs.append(noSnowDailyAcc)
        dailyAccs.append(snowDailyAcc)

        m.execute('ga-l3-tile-inversion-dailyacc-step.sh', ['bbdrs'], dailyAccs, parameters=[tile,year,doy,gaRootDir,bbdrRootDir,beamDir])

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
        noSnowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + rightyear + '/' + tile + '/NoSnow/PROCESSED_ALL_' + doy
        snowDailyAcc = gaRootDir + '/BBDR/DailyAcc/' + rightyear + '/' + tile + '/Snow/PROCESSED_ALL_' + doy
        dailyAccs.append(noSnowDailyAcc)
        dailyAccs.append(snowDailyAcc)

        m.execute('ga-l3-tile-inversion-dailyacc-step.sh', ['bbdrs'], dailyAccs, parameters=[tile,rightyear,doy,gaRootDir,bbdrRootDir,beamDir])


    # cleanup:
#    m.execute('ga-l3-tile-inversion-cleanup-step.sh', albedoProducts, ['dummy'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
