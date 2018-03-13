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
#albedoMosaicRootDir = gaRootDir + '/Mosaic/Albedo'
albedoMosaicRootDir = gaRootDir + '/Mosaic/Albedo_monthly'   # monthly 05 products

#years = ['1988','1992','1996','2000','2004','2008','2012','2016']
startYear = 1983
endYear = 2016

#version = 'v0.92'
version = 'v1.0'
sensorID = 'avh_geo'
snowModes = ['NoSnow','Snow']
#snowModes = ['NoSnow']
#snowModes = ['Snow']
#snowModes = ['Merge']
resolutions = ['05']
#resolutions = ['05','005']
#resolutions = ['005']

inputs = ['albedos']
m = PMonitor(inputs,
             request='ga-l3-albedo-mosaic-timedim-monthly',
             logdir='log',
             hosts=[('localhost',64)],
             types=[ ('ga-l3-albedo-timedim-monthly-step.sh',64) ] )


##### LOOP over years, snowModes, resolutions...: ####
for res in resolutions:
    for snowMode in snowModes:
        for iyear in range(startYear, endYear+1):
        #for iyear in range(startYear, endYear+1, 4):
            year = str(iyear)
            albedoMosaicSourceDir = albedoMosaicRootDir + '/' + snowMode + '/' + year
            albedoMosaicTimedimDir = qa4ecvArchiveRootDir + '/albedo_monthly/L3_Mosaic_' + snowMode + '/' + version + '/' + year
            #startDoy = '001'
            #endDoy = '365'

            startMonth = '1'
            endMonth = '12'

            postCond = 'albedo_mosaic_timedim_monthly' + res + '_' + snowMode + '_' + year + '_' + startMonth
            m.execute('ga-l3-albedo-mosaic-timedim-monthly-step.sh', ['albedos'], [postCond], parameters=[sensorID,year,res,snowMode,startMonth,endMonth,albedoMosaicSourceDir,albedoMosaicTimedimDir])

# wait for processing to complete
m.wait_for_completion()
