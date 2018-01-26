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
# - daily accumulation
# - full accumulation, inversion, BRDF to albedo conversion
#
# NOTE: per PMonitor execute of ga-l3-tile-inversion-dailyacc-spectral-step.sh and
# ga-l3-tile-inversion-albedo-spectral-step.sh, many LSF jobs are initiated.
#
__author__ = 'olafd'
###########################################################################################


######################## spectral SDR --> BRDF tiles: daily accumulation and inversion ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

spectralSdrRootDir = gaRootDir + '/SDR_spectral'
spectralInversionRootDir = gaRootDir + '/Inversion_spectral'
spectralDailyAccRootDir = '/group_workspaces/cems2/qa4ecv/vol3/olafd/GlobAlbedoTest/SpectralDailyAccumulators'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#######
#sensorID = 'mer' # must be one of: 'mer', 'vgt', 'mer_vgt'
sensorID = 'mer_vgt' # must be one of: 'mer', 'vgt', 'mer_vgt'
#######

#tiles = ['h20v06','h25v06'] # test
#tiles = ['h19v07','h30v11'] # test
tiles = ['h17v04'] # test
#tiles = ['h18v04'] # test
#tiles = ['h18v04','h19v09','h22v02','h25v06'] # the 4 GA test tiles
#tiles = ['h17v02','h18v02','h19v02','h20v02',
#         'h17v03','h18v03','h19v03','h20v03',
#         'h17v04','h18v04','h19v04','h20v04',
#         'h17v05','h18v05','h19v05','h20v05'] # Europe


startYear = 2005
endYear = 2005

bandIndices = ['1', '2', '3', '4', '5', '6', '7']
#bandIndices = ['3', '4']
#bandIndices = ['3']

inputs = ['sdrs']

m = PMonitor(inputs,
             request='ga-l3-tile-dailyacc-spectral',
             logdir='log',
             hosts=[('localhost',64)], # let's try this number...
             types=[ ('ga-l3-tile-dailyacc-spectral-step.sh',64)] )

##### LOOP over tiles and years: ####
for tile in tiles:
    for bandIndex in bandIndices:

        ### daily accumulation for all years:
        allDailyAccPostConds = []

        # left wing

        year = str(startYear-1)
        startDoy = '273'
        endDoy = '361'
        postCond = 'daily_accs_' + year + '_' + tile + '_' + bandIndex
        allDailyAccPostConds.append(postCond)
        m.execute('ga-l3-tile-dailyacc-spectral-step.sh',
                         ['sdrs'],
                         [postCond],
                         parameters=[bandIndex,tile,year,startDoy,endDoy,gaRootDir,spectralSdrRootDir,spectralDailyAccRootDir,beamDir])

        for iyear in range(startYear, endYear+1):
            year = str(iyear)
            startDoy = '000'
            endDoy = '361'
            postCond = 'daily_accs_' + year + '_' + tile + '_' + bandIndex
            allDailyAccPostConds.append(postCond)
            m.execute('ga-l3-tile-dailyacc-spectral-step.sh', 
                         ['sdrs'], 
                         [postCond], 
                         parameters=[bandIndex,tile,year,startDoy,endDoy,gaRootDir,spectralSdrRootDir,spectralDailyAccRootDir,beamDir])			     

        # right wing
        year = str(endYear+1)
        startDoy = '001'
        endDoy = '089'
        postCond = 'daily_accs_' + year + '_' + tile + '_' + bandIndex
        allDailyAccPostConds.append(postCond)
        m.execute('ga-l3-tile-dailyacc-spectral-step.sh',
                         ['sdrs'],
                         [postCond],
                         parameters=[bandIndex,tile,year,startDoy,endDoy,gaRootDir,spectralSdrRootDir,spectralDailyAccRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()
