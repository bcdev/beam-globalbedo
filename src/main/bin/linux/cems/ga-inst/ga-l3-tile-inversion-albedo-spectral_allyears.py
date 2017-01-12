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

tiles = ['h25v06'] # test

startYear = 2005  # test
endYear = 2005

subStartX = ['0', '300', '600', '900']
subStartY = ['0', '300', '600', '900']

inputs = ['sdrs']

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-albedo-spectral_allyears',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-dailyacc-spectral-step.sh',64),
                     ('ga-l3-tile-inversion-albedo-spectral-step.sh',64)] )

##### LOOP over tiles and years: ####
for tile in tiles:

    for startX in subStartX:
        for startY in subStartY:

            ### daily accumulation for all years:
            allDailyAccPostConds = []
            for iyear in range(startYear, endYear+1):
                year = str(iyear)
                startDoy = '000'
                endDoy = '361'
                postCond = 'daily_accs_' + year + '_' + tile + '_' + startX + '_' + startY
                allDailyAccPostConds.append(postCond)
                m.execute('ga-l3-tile-inversion-dailyacc-spectral-step.sh', 
                         ['sdrs'], 
		         [postCond], 
                         parameters=[tile,rightyear,startDoy,endDoy,'8',startX,startY,gaRootDir,spectralSdrRootDir,beamDir])			     

            ### now full accumulation, inversion and albedo:
            allAlbedoPostConds = []
            for iyear in range(startYear, endYear+1):
                year = str(iyear)
                spectraAlbedoDir = spectraAlbedoRootDir + '/' + year + '/' + tile + '_' + startX + '_' + startY
                startDoy = '001'
                endDoy = '365'

                # this will be executed when all accumulation jobs completed successfully.
                # However, as one of those PMonitor jobs initiates several LSF accumulation jobs, it is not guaranteed with the current
                # setup that all of those LSB jobs are finished at this time, i.e. that all binariy accumulator files have been written.
                # This must be checked in a waiting loop in ga-l3-tile-inversion-albedo-avhrrgeo_test-step.sh before inversion/albedo jobs are started.

                postCond = 'albedo_' + year + '_' + tile + '_' + startX + '_' + startY
                allAlbedoPostConds.append(postCond)
                m.execute('ga-l3-tile-inversion-albedo-spectral-step.sh', 
                         allDailyAccPostConds, 
		         [postCond], 
                         parameters=[tile,year,startDoy,endDoy,startX,startY,gaRootDir,spectralSdrRootDir,spectralInversionRootDir,beamDir,spectralAlbedoDir])

            ### cleanup:
            # do this manually for the moment

# wait for processing to complete
m.wait_for_completion()
