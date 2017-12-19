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
#tiles = ['h18v04'] # test
#tiles = ['h20v06','h25v06'] # test
tiles = ['h18v04','h19v09','h22v02','h25v06'] # the 4 GA test tiles

startYear = 2007  # test
endYear = 2007

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
        endDoy = '366'

        postCond = 'albedo_' + year + '_' + tile
        m.execute('ga-l3-tile-albedo-spectral-step.sh',
                     ['brdfs'], 
		     [postCond], 
                     parameters=[tile,year,startDoy,endDoy,gaRootDir,spectralInversionRootDir,spectralAlbedoRootDir,beamDir])

    ### cleanup:
    # do this manually for the moment

# wait for processing to complete
m.wait_for_completion()
