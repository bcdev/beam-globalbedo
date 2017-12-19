import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

###########################################################################################
#BBDR --> BRDF --> Albedo startup script for spectral approach (mapped MERIS --> MODIS)
# Invokes steps:
# - full accumulation, inversion, BRDF to albedo conversion
#
__author__ = 'olafd'
###########################################################################################


######################## spectral SDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

spectralInversionRootDir = gaRootDir + '/Inversion_spectral'
spectralAlbedoRootDir = gaRootDir + '/Albedo_spectral'
spectralDailyAccRootDir = '/group_workspaces/cems2/qa4ecv/vol3/olafd/GlobAlbedoTest/SpectralDailyAccumulators'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#######
#sensorID = 'mer' # must be one of: 'mer', 'vgt', 'mer_vgt'
sensorID = 'mer_vgt' # must be one of: 'mer', 'vgt', 'mer_vgt'
#######

#tiles = ['h22v02'] # test
#tiles = ['h18v04'] # test
#tiles = ['h20v06','h25v06'] # test
tiles = ['h18v04','h19v09','h22v02','h25v06'] # the 4 GA test tiles

startYear = 2007  # test
endYear = 2007

bandIndices = ['1', '2', '3', '4', '5', '6', '7']
#bandIndices = ['3', '4']
#bandIndices = ['3']

inputs = ['dailyaccs']

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-spectral',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-spectral-step.sh',64)] )

##### LOOP over tiles and years: ####
for tile in tiles:
    for bandIndex in bandIndices:

        ### full accumulation, inversion and albedo:
        for iyear in range(startYear, endYear+1):

            year = str(iyear)
            startDoy = '001'
            endDoy = '366'

            postCond = 'albedo_' + year + '_' + tile + '_' + bandIndex
            m.execute('ga-l3-tile-inversion-spectral-step.sh',
                         ['dailyaccs'], 
		         [postCond], 
                         parameters=[sensorID,bandIndex,tile,year,startDoy,endDoy,gaRootDir,spectralInversionRootDir,spectralDailyAccRootDir,beamDir])

        ### cleanup:
        # do this manually for the moment

# wait for processing to complete
m.wait_for_completion()
