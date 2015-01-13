import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#year = sys.argv[1]   # in [1997, 2010]
year = '2005'    #test  

######################## BBDR --> BRDF tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
bbdrRootDir = gaRootDir + '/BBDR'
noSnowPriorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km/NoSnow'
snowPriorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km/Snow'
beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'

doys = []
#for i in range(44):
#for i in range(3):   # test
for i in range(1):   # test
    doy = 8*i + 1
    doys.append(str(doy).zfill(3))

m = PMonitor([bbdrRootDir], 
             request='ga-l3-tile',
             logdir='log', 
             hosts=[('localhost',4)])

#tiles = glob.glob1(bbdrTileDir+'/'+sensor+'/'+year, 'h??v??')
#tiles = glob.glob1(bbdrTileDir, 'h??v??')
#tiles = ['h18v03', 'h18v04']  # test
tiles = ['h18v04']  # test
tiles.sort()

for tile in tiles:
    for doy in doys:
        ## daily accumulation, full accumulation, inversion, merge, albedo

        albedoDir = gaRootDir + '/Albedo/' + tile
        m.execute('ga-l3-tile-step.sh', [bbdrRootDir], [albedoDir], parameters=[tile,year,doy,'false',gaRootDir,noSnowPriorDir,snowPriorDir,beamDir])
        m.execute('ga-l3-tile-step.sh', [bbdrRootDir], [albedoDir], parameters=[tile,year,doy,'true',gaRootDir,snowPriorDir,snowPriorDir,beamDir])

# wait for processing to complete
m.wait_for_completion()
