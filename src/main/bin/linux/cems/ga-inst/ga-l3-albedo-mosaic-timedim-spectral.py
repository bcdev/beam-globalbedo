import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

#############################################################################################
# script to add 'time' dimension to 2D albedo products
#
__author__ = 'olafd'
#############################################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
#qa4ecvArchiveRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/qa4ecv_archive/qa4ecv'
qa4ecvArchiveRootDir = '/group_workspaces/cems2/qa4ecv/vol3/olafd/qa4ecv_archive/qa4ecv'
albedoMosaicRootDir = gaRootDir + '/Mosaic/Albedo_spectral'

#years = ['1988','1992','1996','2000','2004','2008','2012','2016']
startYear = 2007
endYear = 2007

#version = 'v0.92'
version = 'v1.0'
#snowModes = ['NoSnow','Snow','Merge']
#snowModes = ['NoSnow']
#snowModes = ['Snow']
snowModes = ['Merge']
#resolutions = ['05']
#resolutions = ['05','005']
resolutions = ['005']

inputs = ['albedos']
m = PMonitor(inputs,
             request='ga-l3-albedo-mosaic-timedim-spectral',
             logdir='log',
             hosts=[('localhost',64)],
             types=[ ('ga-l3-albedo-timedim-spectral-step.sh',64) ] )


##### LOOP over years, snowModes, resolutions...: ####
for res in resolutions:
    for snowMode in snowModes:
        #for iyear in range(startYear, endYear+1):
        for iyear in range(startYear, endYear+1, 4):
            year = str(iyear)
            albedoMosaicSourceDir = albedoMosaicRootDir + '/' + snowMode + '/' + year
            albedoMosaicTimedimDir = qa4ecvArchiveRootDir + '/albedo_spectral/L3_Mosaic_' + snowMode + '/' + version + '/' + year
            startDoy = '001'
            endDoy = '365'

            #startDoy = '121'
            #endDoy = '121'

            postCond = 'albedo_mosaic_timedim_' + res + '_' + snowMode + '_' + year + '_' + startDoy
            m.execute('ga-l3-albedo-mosaic-timedim-spectral-step.sh', ['albedos'], [postCond], 
                       parameters=[year,res,snowMode,startDoy,endDoy,albedoMosaicSourceDir,albedoMosaicTimedimDir])

# wait for processing to complete
m.wait_for_completion()
