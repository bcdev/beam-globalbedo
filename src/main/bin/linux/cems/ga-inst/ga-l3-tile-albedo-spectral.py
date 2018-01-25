import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

###########################################################################################
# BRDF --> Albedo startup script for spectral approach (mapped MERIS --> MODIS)
# Invokes steps:
# - BRDF to albedo conversion 
# - snow/nosnow merge
#
__author__ = 'olafd'
###########################################################################################


######################## spectral SDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

spectralInversionRootDir = gaRootDir + '/Inversion_spectral'
spectralAlbedoRootDir = gaRootDir + '/Albedo_spectral'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#tiles = ['h22v02'] # test
tiles = ['h17v04'] # test
#tiles = ['h20v06','h25v06'] # test
#tiles = ['h18v04','h19v09','h22v02','h25v06'] # the 4 GA test tiles
#tiles = ['h17v02','h18v02','h19v02','h20v02',
#         'h17v03','h18v03','h19v03','h20v03',
#         'h17v02','h18v04','h19v04','h20v04',
#         'h17v05','h18v05','h19v05','h20v05'] # Europe


startYear = 2005  # test
endYear = 2005

inputs = ['brdfs']

m = PMonitor(inputs,
             request='ga-l3-tile-albedo-spectral',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[ ('ga-l3-tile-albedo-spectral-step.sh',64)] )

##### LOOP over tiles and years: ####
for tile in tiles:

    ### BRDF --> albedo, snow/nosnow merge:
    for iyear in range(startYear, endYear+1):

        year = str(iyear)
        startDoy = '001'
        endDoy = '365'

        postCond = 'albedo_' + year + '_' + tile
        m.execute('ga-l3-tile-albedo-spectral-step.sh',
                     ['brdfs'], 
		     [postCond], 
                     parameters=[tile,year,startDoy,endDoy,gaRootDir,spectralInversionRootDir,spectralAlbedoRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()
