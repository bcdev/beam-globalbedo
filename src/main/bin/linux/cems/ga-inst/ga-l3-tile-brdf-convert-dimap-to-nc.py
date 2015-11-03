import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#year = sys.argv[1]   # in [1997, 2010]
#years = ['2001']    #test  
#years = ['2003']    #test  
year = '2005'    #test  --> we should run this with  a single year only! todo: make configurable

######################## BBDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
#beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km'

doys = []
for i in range(46): # one year
#for i in range(4):   # one month
#for i in range(1):   # one day
    doy = 8*i + 1
    #doy = 8*i + 129  # May
    #doy = 8*i + 337  # Dec
    doys.append(str(doy).zfill(3))

#doys = ['001', '121', '361'] # test!
#doys = ['121'] # test!

tiles = glob.glob1(priorDir + '/Snow', 'h??v??')
#tiles = ['h18v04']  # test
tiles.sort()

brdfProducts=[]
for tile in tiles:
    for doy in doys:
        brdfDir = gaRootDir + '/Inversion/Merge/' + year + '/' + tile
        brdfProduct = brdfDir + '/GlobAlbedo.brdf.merge.' + year + doy + '.' + tile + '.dim'
	brdfProducts.append(brdfProduct)

m = PMonitor(brdfProducts, 
             request='ga-l3-tile-brdf-convert-dimap-to-nc',
             logdir='log', 
             hosts=[('localhost',192)], # let's try this number...
             types=[ ('ga-l3-tile-brdf-convert-dimap-to-nc-step.sh',192)] )

### per tile: convert BRDF nc to dimap for Mosaicking: 

for tile in tiles:
    brdfDir = gaRootDir + '/Inversion/Merge/' + year + '/' + tile
    for doy in doys:

        ### conversion (with gaPassThrough):

        # brdf product e.g. : /GlobAlbedoTest/Inversion/Merge/2005/h18v04/GlobAlbedo.brdf.merge.2005121.h18v04.nc
        brdfProduct = brdfDir + '/GlobAlbedo.brdf.merge.' + year + doy + '.' + tile + '.dim'
        brdfNcProduct = brdfDir + '/GlobAlbedo.brdf.merge.' + year + doy + '.' + tile + '.nc'

        m.execute('ga-l3-tile-brdf-convert-dimap-to-nc-step.sh', [brdfProduct], [brdfNcProduct], parameters=[tile,year,doy,gaRootDir,beamDir,brdfDir])

# wait for processing to complete
m.wait_for_completion()
