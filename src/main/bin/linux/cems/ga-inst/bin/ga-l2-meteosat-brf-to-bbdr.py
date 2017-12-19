import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

#################################################################
# Converts GEO/MVIRI BRF (tiles) to GlobAlbedo conform BBDR (tiles)
#
__author__ = 'olafd'
#
#################################################################

#years = ['1982','1983']    #test  
years = ['2004','2006']    #test  
#years = ['2008','2009','2010','2011','2012','2013','2014']  

######################## BRF --> BBDR: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'


priorDir = '/group_workspaces/cems2/qa4ecv/vol3/prior.mcd43a.c5.broadband/1km' # moved by SK, 20160930?!

# all 326 tiles we have:
#tiles = glob.glob1(priorDir + '/Snow', 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles = glob.glob1(priorDir, 'h??v??') # we have same number (326) of snow and noSnow prior directories
tiles.sort()

tiles = ['h17v08']

sensors = ['MVIRI', 'SEVIRI']
#sensors = ['MVIRI']

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-meteosat-brf-to-bbdr',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-meteosat-brf-to-bbdr-step.sh',192)])

for sensor in sensors:
    for year in years:
        brfYearDir = gaRootDir + '/BRF/' + sensor + '/' + year
        if os.path.exists(brfYearDir):
            meteosatTiles = os.listdir(brfYearDir)
            if len(meteosatTiles) > 0:
                for tile in tiles:
                    for index in range(0, len(meteosatTiles)):
                        if tile == meteosatTiles[index]:
                            brfTileDir = brfYearDir + '/' + tile
                            bbdrTileDir = gaRootDir + '/BBDR/' + sensor + '/' + year + '/' + tile
                            brfFiles = os.listdir(brfTileDir)
                            if len(brfFiles) > 0:
                                for index in range(0, len(brfFiles)):
                                    # we do not want other files than *.nc or *.nc.gz:
                                    if brfFiles[index].endswith(".nc") or brfFiles[index].endswith(".nc.gz"):
                                        brfPath = brfTileDir + '/' + brfFiles[index]
                                        m.execute('ga-l2-meteosat-brf-to-bbdr-step.sh', ['dummy'], [bbdrTileDir], parameters=[brfPath,brfFiles[index],bbdrTileDir,sensor,gaRootDir,beamDir])

m.wait_for_completion()

