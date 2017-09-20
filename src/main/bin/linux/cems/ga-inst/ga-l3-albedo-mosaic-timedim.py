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
qa4ecvArchiveRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/qa4ecv_archive/qa4ecv'
albedoMosaicRootDir = gaRootDir + '/Mosaic/Albedo'

startYear = 2008
endYear = 2010

version = 'v0.9'
sensorID = 'avh_geo'
snowModes = ['NoSnow','Snow']
#snowModes = ['NoSnow']
resolutions = ['05']
#resolutions = ['005']

inputs = ['albedos']
m = PMonitor(inputs,
             request='ga-l3-albedo-mosaic-timedim',
             logdir='log',
             hosts=[('localhost',64)],
             types=[ ('ga-l3-albedo-timedim-step.sh',64) ] )


##### LOOP over years, snowModes, resolutions...: ####
for res in resolutions:
    for snowMode in snowModes:
        for iyear in range(startYear, endYear+1):
            year = str(iyear)
            albedoMosaicSourceDir = albedoMosaicRootDir + '/' + snowMode + '/' + sensorID + '/' + year + '/' + res
            albedoMosaicTimedimDir = qa4ecvArchiveRootDir + '/albedo/L3_Mosaic_' + snowMode + '/' + version + '/' + year
            startDoy = '001'
            #endDoy = '003'
            endDoy = '365'

            postCond = 'albedo_mosaic_timedim_' + res + '_' + snowMode + '_' + year + '_' + startDoy
            m.execute('ga-l3-albedo-mosaic-timedim-step.sh', ['albedos'], [postCond], parameters=[sensorID,year,res,snowMode,startDoy,endDoy,albedoMosaicSourceDir,albedoMosaicTimedimDir])

# wait for processing to complete
m.wait_for_completion()
