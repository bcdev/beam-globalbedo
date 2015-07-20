import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#year = sys.argv[1]   # in [1997, 2010]
years = ['2005']    #test  
sensors = ['MERIS']

######################## BBDR --> BRDF tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
#noSnowPriorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km/NoSnow'
#snowPriorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km/Snow'
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km'
beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'

doys = []
#for i in range(45): # one year
for i in range(4):   # one month
#for i in range(1):   # one day
    #doy = 8*i + 1
    doy = 8*i + 121  # May
    #doy = 8*i + 337  # Dec
    doys.append(str(doy).zfill(3))

tiles = glob.glob1(priorDir + '/Snow', 'h??v??') # we have same number (326) of snow and noSnow prior directories
#tiles = ['h18v04']  # test
#tiles = ['h16v01','h18v04','h18v06','h18v07',]  # test
tiles.sort()

bbdrDirs = []
for sensor in sensors:
    for year in years:
        for tile in tiles:
            bbdrDirs.append(gaRootDir + '/BBDR/' + sensor + '/' + year + '/' + tile)

m = PMonitor(bbdrDirs, 
             request='ga-l3-tile-inversion-albedo',
             logdir='log', 
             hosts=[('localhost',192)], # let's try this number...
             types=[('ga-l3-tile-inversion-albedo-step.sh',192)])

for year in years:
    for tile in tiles:
        bbdrDir = gaRootDir + '/BBDR/' + sensor + '/' + year + '/' + tile
        for doy in doys:

            ## ALL in ONE step: snow/nosnow daily accumulation, full accumulation, inversion --> BRDF --> Merge --> Albedo
            ## --> better to have larger, but fewer jobs to submit to LSB!
            albedoDir = gaRootDir + '/Albedo/' + year + '/' + tile
            m.execute('ga-l3-tile-inversion-albedo-step.sh', [bbdrDir], [albedoDir], parameters=[tile,year,doy,gaRootDir,priorDir,beamDir,albedoDir])

# wait for processing to complete
m.wait_for_completion()
