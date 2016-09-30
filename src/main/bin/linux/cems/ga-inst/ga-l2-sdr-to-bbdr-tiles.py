import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#sensors = ['MERIS','VGT']
#sensors = ['MERIS']
#sensors = ['VGT']
sensors = ['PROBAV']
years = ['2015']    #test  
#years = ['2008','2009','2010','2011','2012','2013','2014']  

######################## SDR --> BBDR: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

priorDir = '/group_workspaces/cems2/qa4ecv/vol3/newPrior_broadband/1km'
# all 326 tiles we have:
tiles = glob.glob1(priorDir + '/Snow', 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

#tiles = ['h16v02','h17v02','h18v02']
#tiles = ['h18v03']
tiles = ['h18v04']

# e.g. ../GlobAlbedoTest/SDR/VGT/2001/h18v04/*.nc:
#inputs = []
#for year in years:
#    for sensor in sensors:
#        for tile in tiles:
#            sdrTileDir = gaRootDir + '/SDR/' + sensor + '/' + year + '/' + tile
#            if os.path.exists(sdrTileDir):
#                sdrFiles = os.listdir(sdrTileDir)
#                if len(sdrFiles) > 0:
#                    # we do not want other files than *.nc or *.nc.gz:
#                    for index in range(0, len(sdrFiles)):
#                        if sdrFiles[index].endswith(".nc") or sdrFiles[index].endswith(".nc.gz"):
#                            inputs.append(sdrTileDir + '/' + sdrFiles[index])

#print 'inputs: ', inputs
inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-sdr-to-bbdr-tiles',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-sdr-to-bbdr-step.sh',192)])

for year in years:
    for sensor in sensors:
        for tile in tiles:
            sdrTileDir = gaRootDir + '/SDR/' + sensor + '/' + year + '/' + tile
            bbdrTileDir = gaRootDir + '/BBDR/' + sensor + '/' + year + '/' + tile
            if os.path.exists(sdrTileDir):
                sdrFiles = os.listdir(sdrTileDir)
                if len(sdrFiles) > 0:
                    for index in range(0, len(sdrFiles)):
                        # we do not want other files than *.nc or *.nc.gz:
		        if sdrFiles[index].endswith(".nc") or sdrFiles[index].endswith(".nc.gz"):
                            sdrPath = sdrTileDir + '/' + sdrFiles[index]
                            m.execute('ga-l2-sdr-to-bbdr-step.sh', ['dummy'], [bbdrTileDir], parameters=[sdrPath,sdrFiles[index],bbdrTileDir,sensor,gaRootDir,beamDir])

m.wait_for_completion()

