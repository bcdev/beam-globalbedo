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

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

doys = []
for i in range(46): # one year
    doy = 8*i + 1
    doys.append(str(doy).zfill(3))

doys = ['161'] # test!

#tiles = ['h18v04','h22v02','h25v06','h19v08']
#tiles = ['h19v08']
tiles = ['h20v06','h25v06']

subStartX = ['0', '300', '600', '900']
subStartY = ['0', '300', '600', '900']

inputs = ['bbdrs']
left_accs_name = 'dailyaccs_left'
center_accs_name = 'dailyaccs_center'
right_accs_name = 'dailyaccs_right'

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-albedo-spectral',
             logdir='log',
             hosts=[('localhost',96)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-dailyacc-spectral-step.sh',72),
                     ('ga-l3-tile-inversion-albedo-spectral-step.sh',24)] )

years = ['2005']

for year in years:
    leftyear = str(int(year)-1)
    rightyear = str(int(year)+1)

    ### daily accumulation for processing year and left/right wings

    for tile in tiles:

        for startX in subStartX:
            for startY in subStartY:

                # left wing year daily accs:
                startDoy='273'
                endDoy = '361'
                m.execute('ga-l3-tile-inversion-dailyacc-spectral-step.sh', 
                          ['bbdrs'], [left_accs_name], 
                          parameters=[tile,leftyear,startDoy,endDoy,'8',startX,startY,gaRootDir,spectralSdrRootDir,beamDir])

                # right wing year daily accs:
                startDoy='000'
                endDoy = '097'
                m.execute('ga-l3-tile-inversion-dailyacc-spectral-step.sh', 
                          ['bbdrs'], [right_accs_name], 
                          parameters=[tile,rightyear,startDoy,endDoy,'8',startX,startY,gaRootDir,spectralSdrRootDir,beamDir])

                # center year daily accs (after completion of wings):
                startDoy = '000'
                endDoy = '361'
                m.execute('ga-l3-tile-inversion-dailyacc-spectral-step.sh', 
                          ['bbdrs'], [center_accs_name], 
                          parameters=[tile,year,startDoy,endDoy,'8',startX,startY,gaRootDir,spectralSdrRootDir,beamDir])

                all_accs_names = [left_accs_name, center_accs_name, right_accs_name]

                ### full accumulation, inversion and albedo now in one step:

                spectralAlbedoDir = gaRootDir + '/Albedo_spectral/' + year + '/' + tile

                #########################################################################################################################

                startDoy = '001'
                endDoy = '365' 

                # this will be executed when all three accumulation jobs (left, center, right year) completed successfully.
                # However, as one of those PMonitor jobs initiates several LSF accumulation jobs, it is not guaranteed with the current
                # setup that all of those LSB jobs are finished at this time, i.e. that all binariy accumulator files have been written.
                # This must be checked in a waiting loop in ga-l3-tile-inversion-albedo-spectral-step.sh before inversion/albedo jobs are started.

                m.execute('ga-l3-tile-inversion-albedo-spectral-step.sh', 
                          all_accs_names, ['dummy1'], 
                          parameters=[tile,year,startDoy,endDoy,startX,startY,gaRootDir,spectralSdrRootDir,spectralInversionRootDir,beamDir,spectralAlbedoDir])

                #########################################################################################################################

                # cleanup:
                # do this manually for the moment
                # m.execute('ga-l3-tile-inversion-cleanup-step.sh', ['dummy1'], ['dummy2'], parameters=[tile,year,gaRootDir])

# wait for processing to complete
m.wait_for_completion()
