import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides one step:
###    - staging 'mosaicking' --> brdf mosaic for a list of tiles
########################################################################


gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
stagingMosaickingListDir = gaRootDir + '/staging/lists/mosaicking'
stagingMosaickingResultDir = gaRootDir + '/staging/Mosaics/brdf'

year = '2005'

doys = []
for i in range(46): # one year
    doy = 8*i + 1
    doys.append(str(doy).zfill(3))

doys = ['121'] # test!

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-mosaicking', 
             logdir='log',
             hosts=[('localhost',192)],
	     types=[('ga-l3-staging-mosaicking-step.sh',192)])

### mosaicking for all doys

for doy in doys:
    list = stagingMosaickingListDir + '/' + 'list.' + year + doy + '.merge.txt' 
    #list = stagingMosaickingListDir + '/' + 'list.' + year + doy + '.merge.2tiles.txt' # test! 
    m.execute('ga-l3-staging-mosaicking-step.sh', ['dummy'], [stagingMosaickingResultDir], parameters=[year,doy,list,stagingMosaickingResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
