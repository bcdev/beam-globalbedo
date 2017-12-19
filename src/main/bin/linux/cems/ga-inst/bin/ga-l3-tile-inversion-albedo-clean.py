import glob
import os
import os.path
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

year = '2015'

######## This script invokes a cleanup after BBDR --> BRDF/albedo inversion processing ##################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km'

# all 326 tiles we have:
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l3-tile-inversion-albedo-clean',
             logdir='log', 
             hosts=[('localhost',250)],
             types=[ ('ga-l3-tile-inversion-cleanup-step.sh',250)] )

### daily accumulation for processing year and left/right wings

for tile in tiles:
    # just the cleanup per tile and year:
    m.execute('ga-l3-tile-inversion-cleanup-step.sh', ['dummy'], ['dummy2'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
