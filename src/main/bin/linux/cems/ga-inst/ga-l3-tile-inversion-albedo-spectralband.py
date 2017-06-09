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
spectralAlbedoRootDir = gaRootDir + '/Albedo_spectral'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#######
#sensorID = 'mer' # must be one of: 'mer', 'vgt', 'mer_vgt'
sensorID = 'mer_vgt' # must be one of: 'mer', 'vgt', 'mer_vgt'
#######

#tiles = ['h20v06','h25v06'] # test
tiles = ['h19v07','h30v11'] # test
#tiles = ['h20v06'] # test
#tiles = ['h18v04'] # test
#tiles = ['h20v06','h25v06'] # test

startYear = 2005  # test
endYear = 2005

inputs = ['sdrs']

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-albedo-spectralband',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-dailyacc-spectralband-step.sh',64),
                     ('ga-l3-tile-inversion-albedo-spectralband-step.sh',64)] )

##### LOOP over tiles and years: ####
for tile in tiles:

    ### daily accumulation for all years:
    allDailyAccPostConds = []

    # left wing

    year = str(startYear-1)
    startDoy = '273'
    endDoy = '361'
    postCond = 'daily_accs_' + year + '_' + tile
    allDailyAccPostConds.append(postCond)
    m.execute('ga-l3-tile-inversion-dailyacc-spectralband-step.sh',
                         ['sdrs'],
                         [postCond],
                         parameters=[tile,year,startDoy,endDoy,'8',gaRootDir,spectralSdrRootDir,beamDir])

    for iyear in range(startYear, endYear+1):
        year = str(iyear)
        startDoy = '000'
        endDoy = '361'
        postCond = 'daily_accs_' + year + '_' + tile
        allDailyAccPostConds.append(postCond)
        m.execute('ga-l3-tile-inversion-dailyacc-spectralband-step.sh', 
                         ['sdrs'], 
                         [postCond], 
                         parameters=[tile,year,startDoy,endDoy,'8',gaRootDir,spectralSdrRootDir,beamDir])			     

    # right wing
    year = str(endYear+1)
    startDoy = '001'
    endDoy = '089'
    postCond = 'daily_accs_' + year + '_' + tile
    allDailyAccPostConds.append(postCond)
    m.execute('ga-l3-tile-inversion-dailyacc-spectralband-step.sh',
                         ['sdrs'],
                         [postCond],
                         parameters=[tile,year,startDoy,endDoy,'8',gaRootDir,spectralSdrRootDir,beamDir])

    ### now full accumulation, inversion and albedo:
    allAlbedoPostConds = []
    for iyear in range(startYear, endYear+1):
        year = str(iyear)
        spectralAlbedoDir = spectralAlbedoRootDir + '/' + sensorID + '/' + year + '/' + tile
        startDoy = '001'
        endDoy = '365'

        # this will be executed when all accumulation jobs completed successfully.
        # However, as one of those PMonitor jobs initiates several LSF accumulation jobs, it is not guaranteed with the current
        # setup that all of those LSB jobs are finished at this time, i.e. that all binariy accumulator files have been written.
        # This must be checked in a waiting loop in ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh before inversion/albedo jobs are started.

        postCond = 'albedo_' + year + '_' + tile
        allAlbedoPostConds.append(postCond)
        m.execute('ga-l3-tile-inversion-albedo-spectralband-step.sh', 
                         allDailyAccPostConds, 
		         [postCond], 
                         parameters=[sensorID,tile,year,startDoy,endDoy,gaRootDir,spectralSdrRootDir,spectralInversionRootDir,beamDir,spectralAlbedoDir])

    ### cleanup:
    # do this manually for the moment

# wait for processing to complete
m.wait_for_completion()
