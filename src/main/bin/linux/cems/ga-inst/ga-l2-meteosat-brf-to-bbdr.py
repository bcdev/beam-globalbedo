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

years = ['1982','1983']    #test  
#years = ['2005']    #test  
#years = ['2008','2009','2010','2011','2012','2013','2014']  

######################## BRF --> BBDR: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#sensors = ['MVIRI', 'SEVIRI']
sensors = ['MVIRI']

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-meteosat-brf-to-bbdr',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-meteosat-brf-to-bbdr-step.sh',192)])

for sensor in sensors:
    for year in years:
        brfYearDir = gaRootDir + '/BRF/' + sensor + '/' + year
        tiles = os.listdir(brfYearDir)
        if len(tiles) > 0:
            for index in range(0, len(tiles)):
                tile = tiles[index]
                brfTileDir = gaRootDir + '/BRF/' + sensor + '/' + year + '/' + tile
                bbdrTileDir = gaRootDir + '/BBDR/' + sensor + '/' + year + '/' + tile
                if os.path.exists(brfTileDir):
                    brfFiles = os.listdir(brfTileDir)
                    if len(brfFiles) > 0:
                        for index in range(0, len(brfFiles)):
                            # we do not want other files than *.nc or *.nc.gz:
                            if brfFiles[index].endswith(".nc") or brfFiles[index].endswith(".nc.gz"):
                                brfPath = brfTileDir + '/' + brfFiles[index]
                                m.execute('ga-l2-meteosat-brf-to-bbdr-step.sh', ['dummy'], [bbdrTileDir], parameters=[brfPath,brfFiles[index],bbdrTileDir,sensor,gaRootDir,beamDir])

m.wait_for_completion()

