import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

###########################################################################################
#BBDR --> BRDF-Uncertainties startup script for spectral approach (mapped MERIS --> MODIS)
# Invokes steps:
# - full accumulation, inversion, BRDF-uncertainties
# - writes covariance terms to target product
#
__author__ = 'olafd'
###########################################################################################


######################## spectral SDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

spectralInversionRootDir = gaRootDir + '/Inversion_spectral'
spectralAlbedoRootDir = gaRootDir + '/Albedo_spectral'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

tiles = ['h17v03'] # test
#tiles = ['h18v04'] # test
#tiles = ['h18v04','h19v09','h22v02','h25v06'] # the 4 GA test tiles
#tiles = ['h17v02','h18v02','h19v02','h20v02',
#         'h17v03','h18v03','h19v03','h20v03',
#         'h17v04','h18v04','h19v04','h20v04',
#         'h17v05','h18v05','h19v05','h20v05'] # Europe

tiles = ['h17v05','h18v05','h19v05','h20v05']

startYear = 1998
endYear = 2000

inputs = ['uncerts']

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-uncertainties-brdf2albedo-spectral',
             logdir='log',
             hosts=[('localhost',32)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-uncertainties-brdf2albedo-spectral-step.sh',32)] )

##### LOOP over tiles and years: ####
for tile in tiles:
    ### full accumulation, inversion and albedo:
    for iyear in range(startYear, endYear+1):

        year = str(iyear)
        startDoy = '001'
        endDoy = '366'

        #startDoy = '121'
        #endDoy = '121'

        postCond = 'uncertainties_' + year + '_' + tile
        m.execute('ga-l3-tile-inversion-uncertainties-brdf2albedo-spectral-step.sh',
                     ['uncerts'], 
                     [postCond], 
                     parameters=[tile,year,startDoy,endDoy,gaRootDir,spectralInversionRootDir,spectralAlbedoRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()
