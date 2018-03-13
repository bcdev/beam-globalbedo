import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides:
###    - Albedo mosaic --> Albedo monthly mosaic
########################################################################

###########################
# set MODIS tile size
tileSize='1200' # MERIS/VGT
###########################

#years = ['1982']
startYear = 1983
endYear = 2016
#endYear = 1982

#snowModes = ['NoSnow']
#snowModes = ['Merge']
snowModes = ['NoSnow','Snow']

degs = ['05']
#degs = ['005']
#degs = ['05','005']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = []
#for year in years:
for iyear in range(startYear, endYear+1):
    year = str(iyear)
    for snowMode in snowModes:
        inputs.append(gaRootDir + '/Mosaic/Albedo/' + snowMode + '/' + year)

#### Monthly mosaics ####

m = PMonitor(inputs, 
             request='ga-l3-albedomosaic-monthly', 
             logdir='log',
             hosts=[('localhost',16)],
	     types=[('ga-l3-albedomosaic-monthly-step.sh',16)])

#for year in years:
for iyear in range(startYear, endYear+1):
    year = str(iyear)     
    for snowMode in snowModes:
        for deg in degs:
            for month in range(1,13):    
            #for month in range(5,6):    
                albedoDailyMosaicDir = gaRootDir + '/Mosaic/Albedo/' + snowMode + '/' + year
                albedoMonthlyMosaicDir = gaRootDir + '/Mosaic/Albedo_monthly/' + snowMode + '/' + year
                m.execute('ga-l3-albedomosaic-monthly-step.sh', [albedoDailyMosaicDir], [albedoMonthlyMosaicDir], parameters=[year,str(month),snowMode,deg,gaRootDir,beamDir])


# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
