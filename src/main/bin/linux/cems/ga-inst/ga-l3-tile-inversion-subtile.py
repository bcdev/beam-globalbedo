import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

year = '2005'
sensors = ['MERIS','VGT']
#sensors = ['MERIS']
#sensors = ['VGT']

######################## BBDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
bbdrRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest/BBDR'
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km'
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km'
#beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

doys = []
for i in range(46): # one year
#for i in range(4):   # one month
#for i in range(1):   # one day
    doy = 8*i + 1
    #doy = 8*i + 129  # May
    #doy = 8*i + 337  # Dec
    doys.append(str(doy).zfill(3))

doys = ['121'] # test!

startXVals = []
startYVals = []
for i in range(40): 
    start = 30*i
    startXVals.append(str(start).zfill(4))
    startYVals.append(str(start).zfill(5))

startXVals = ['1080'] # test!
startYVals = ['480'] # test!

tiles = ['h18v04']

bbdrDirs = []
for sensor in sensors:
    leftyear = str(int(year)-1)
    rightyear = str(int(year)+1)
    for tile in tiles:
        bbdrDirs.append(bbdrRootDir + sensor + '/' + year + '/' + tile)
        bbdrDirs.append(bbdrRootDir + sensor + '/' + leftyear + '/' + tile)
        bbdrDirs.append(bbdrRootDir + sensor + '/' + rightyear + '/' + tile)

inputs = ['bbdrs']
m = PMonitor(inputs, 
             request='ga-l3-tile-inversion-subtile',
             logdir='log', 
             hosts=[('localhost',250)], # let's try this number...
             types=[ ('ga-l3-inversion-subtile-step.sh',250)] ) 


for tile in tiles:

    ### inversion:

    inversionProducts = []
    for doy in doys:
        
        ### inversion:

	for startX in startXVals:
	    for startY in startYVals:
                end = int(startX) + 29
                endX = str(end).zfill(4)
                end = int(startY) + 29
                endY = str(end).zfill(4)
                inversionDir = gaRootDir + '/Inversion_subtile/' + year + '/' + tile
                inversionProduct = inversionDir + '/GlobAlbedo.brdf.' + year + doy + '.' + tile + '.' + startX + '.' + startY + '.' + endX + '.' + endY + '.subtile.nc'
                inversionProducts.append(inversionProduct)

                m.execute('ga-l3-inversion-subtile-step.sh', inputs, inversionProducts, 
                          parameters=[tile,year,doy,startX,startY,endX,endY,gaRootDir,bbdrRootDir,priorDir,beamDir,inversionDir])

# wait for processing to complete
m.wait_for_completion()
