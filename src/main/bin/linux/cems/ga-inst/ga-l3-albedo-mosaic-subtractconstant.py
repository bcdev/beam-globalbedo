import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

#############################################################################################
# script to subtract constant from DHR/BHR albedos in mosaic products
#
__author__ = 'olafd'
#############################################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
qa4ecvArchiveRootDir = '/group_workspaces/cems2/qa4ecv/vol3/olafd/qa4ecv_archive/qa4ecv'
albedoMosaicRootDir = gaRootDir + '/Mosaic/Albedo'

#years = ['1988','1992','1996','2000','2004','2008','2012','2016']
startYear = 1982
endYear = 1982

#version = 'v0.92'
src_version = 'v1.0'
target_version = 'v1.2'
sensorID = 'avh_geo'
snowModes = ['NoSnow','Snow','Merge']
#snowModes = ['NoSnow']
#snowModes = ['Snow']
#snowModes = ['Merge']
resolutions = ['05']
#resolutions = ['05','005']
#resolutions = ['005']

inputs = ['albedos']
m = PMonitor(inputs,
             request='ga-l3-albedo-mosaic-subtractconstant',
             logdir='log',
             hosts=[('localhost',64)],
             types=[ ('ga-l3-albedo-subtractconstant-step.sh',64) ] )


##### LOOP over years, snowModes, resolutions...: ####
for res in resolutions:
    for snowMode in snowModes:
        for iyear in range(startYear, endYear+1):
        #for iyear in range(startYear, endYear+1, 4):
            year = str(iyear)
            albedoMosaicSourceDir     = qa4ecvArchiveRootDir + '/albedo/L3_Mosaic_' + snowMode + '/' + src_version + '/' + year
            albedoMosaicSubtractedDir = qa4ecvArchiveRootDir + '/albedo/L3_Mosaic_' + snowMode + '/' + target_version + '/' + year
            startDoy = '001'
            endDoy = '365'

            #startDoy = '366'
            #endDoy = '366'

            postCond = 'albedo_mosaic_subtractconstant_' + res + '_' + snowMode + '_' + year + '_' + startDoy
            m.execute('ga-l3-albedo-mosaic-subtractconstant-step.sh', ['albedos'], [postCond], parameters=[sensorID,year,res,snowMode,startDoy,endDoy,albedoMosaicSourceDir,albedoMosaicSubtractedDir])

# wait for processing to complete
m.wait_for_completion()
