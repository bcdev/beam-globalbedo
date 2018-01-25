import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides two steps:
###    - BRDF tiles --> BRDF mosaic
###    - BRDF mosaic --> Albedo mosaic
### or in 'simple' mode, see below:
###    - Albedo tiles --> Albedo mosaic with reduced output
########################################################################

###########################
# set MODIS tile size
tileSize='1200' # MERIS/VGT
###########################

years = ['2005']

#snowModes = ['NoSnow']
#snowModes = ['Snow']
snowModes = ['NoSnow','Snow']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

# noSnow AND snow, e.g. ../GlobAlbedoTest/Albedo/NoSnow/avh_geo/1989/h18v04
inputs = []
for year in years:
    for snowMode in snowModes:
        inputs.append(gaRootDir + '/Albedo_spectral/' + snowMode + '/' + year)

#### Upscaling/Mosaicing ####

m = PMonitor(inputs, 
             request='ga-l3-mosaic-spectral', 
             logdir='log',
             hosts=[('localhost',16)],
	     types=[('ga-l3-albedomosaic-spectral-step.sh',16)])
     
for year in years:
    for snowMode in snowModes:

        for idoy in range(0,365):    
        #for idoy in range(12,17):    
        #for idoy in range(0,16):    
        #for idoy in range(120,121):    
        #for idoy in range(120,152):  # May    
        #for idoy in range(365,366):    
            doy = str(idoy+1).zfill(3) # daily
            #####doy = str(8*idoy+1).zfill(3)   # 8-day
            # noSnow AND snow, e.g. ../GlobAlbedoTest/Albedo/NoSnow/avh_geo/1989/h18v04
            albedoTileDir = gaRootDir + '/Albedo_spectral/' + snowMode + '/' + year
            albedoMosaicDir = gaRootDir + '/Mosaic/Albedo_spectral/' + snowMode + '/' + year
            m.execute('ga-l3-albedomosaic-spectral-step.sh', [albedoTileDir], [albedoMosaicDir], parameters=[year,doy,snowMode,gaRootDir,beamDir])


# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
