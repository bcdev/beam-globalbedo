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

priorDir = '/group_workspaces/cems2/qa4ecv/vol1/prior.c6/stage2/1km'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#######
#sensorID = 'mer' # must be one of: 'mer', 'vgt', 'mer_vgt'
sensorID = 'mer_vgt' # must be one of: 'mer', 'vgt', 'mer_vgt'
#######

#tiles = ['h17v04'] # test
#tiles = ['h18v04'] # test
#tiles = ['h18v04','h19v09','h22v02','h25v06'] # the 4 GA test tiles
#tiles = ['h17v02','h18v02','h19v02','h20v02',
#         'h17v03','h18v03','h19v03','h20v03',
#         'h17v04','h18v04','h19v04','h20v04',
#         'h17v05','h18v05','h19v05','h20v05'] # Europe

tiles = ['h17v05','h18v05','h19v05','h20v05']

startYear = 1998
endYear = 2000

bandIndices = ['1', '2', '3', '4', '5', '6', '7']
#bandIndices = ['3', '4']
#bandIndices = ['3']

inputs = ['dailyaccs']

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-spectral',
             logdir='log',
             hosts=[('localhost',32)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-spectral-step.sh',32)] )

##### LOOP over tiles and years: ####
for tile in tiles:
    for bandIndex in bandIndices:

        ### full accumulation, inversion and albedo:
        for iyear in range(startYear, endYear+1):

            year = str(iyear)
            startDoy = '001'
            endDoy = '366'

            #startDoy = '121'
            #endDoy = '121'

            postCond = 'albedo_' + year + '_' + tile + '_' + bandIndex
            m.execute('ga-l3-tile-inversion-spectral-step.sh',
                         ['dailyaccs'], 
		         [postCond], 
                         parameters=[sensorID,bandIndex,tile,year,startDoy,endDoy,gaRootDir,spectralInversionRootDir,spectralDailyAccRootDir,priorDir,beamDir])

        ### cleanup:
        # do this manually for the moment

# wait for processing to complete
m.wait_for_completion()
