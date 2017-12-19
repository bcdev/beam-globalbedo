import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides one step:
###    - staging 'matching' --> txt files for each tile
########################################################################


gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
#priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_allBands/1km'
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km'
#stagingMatchingResultDir = gaRootDir + '/staging/matching'
stagingMatchingResultDir = gaRootDir + '/staging/lists/matching'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-matching', 
             logdir='log',
             hosts=[('localhost',192)],
	     types=[('ga-l3-staging-matching-step.sh',192)])

tiles = glob.glob1(priorDir + '/Snow', 'h??v??')
#tiles = ['h18v04', 'h29v11']  # test
tiles.sort()

### matching for all tiles

for tile in tiles:
    m.execute('ga-l3-staging-matching-step.sh', ['dummy'], [stagingMatchingResultDir], parameters=[tile,stagingMatchingResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
