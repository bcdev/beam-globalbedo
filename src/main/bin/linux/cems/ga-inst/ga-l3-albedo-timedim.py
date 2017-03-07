import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

#############################################################################################
# script to add 'time' dimension to 2D albedo products
#
__author__ = 'olafd'
#############################################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
albedoRootDir = gaRootDir + '/Albedo'

#######
sensorID = 'avh_geo' # must be one of: 'avh', 'geo', 'avh_geo'
#sensorID = 'avhrrgeo'  # old
#######

# all 326 tiles we have:
priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.c6/stage2/1km' # another change by SK, 20161115
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
#tiles = glob.glob1(priorDir, 'h3?v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

tiles = ['h18v04']

#startYear = 1998
#endYear = 2014
startYear = 1983
endYear = 1983

inputs = ['albedos']
m = PMonitor(inputs,
             request='ga-l3-albedo-timedim',
             logdir='log',
             hosts=[('localhost',64)],
             types=[ ('ga-l3-albedo-timedim-step.sh',64) ] )


##### LOOP over tiles and years: ####
for tile in tiles:

    for iyear in range(startYear, endYear+1):
        year = str(iyear)
        albedoSourceDir = albedoRootDir + '/' + sensorID + '/' + year + '/' + tile
        albedoTimedimDir = gaRootDir + '/Albedo_3D/' + sensorID + '/' + year + '/' + tile
        startDoy = '001'
        endDoy = '365'

        postCond = 'albedo_timedim_' + year + '_' + tile + '_' + startDoy
        m.execute('ga-l3-albedo-timedim-step.sh', ['albedos'], [postCond], parameters=[sensorID,tile,year,startDoy,endDoy,albedoSourceDir,albedoTimedimDir])

# wait for processing to complete
m.wait_for_completion()
