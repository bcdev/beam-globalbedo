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

mosaicMode = 'simple' # always!
#mosaicMode = 'default'

###########################
# set MODIS tile size
tileSize='200' # AVHRR/GEO
###########################

#years = ['1993']    #test  
years = ['1996','1997','1998','1999','2000']    # priority test, 20170103  

snowModes = ['NoSnow'] # usually for AVHRRGEO

#resolutions = ['05', '005']
resolutions = ['005']
#resolutions = ['05']

#projections = ['SIN', 'PC']
#projections = ['SIN']
projections = ['PC']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = []
for year in years:
    if mosaicMode == 'simple':
        inputs.append(gaRootDir + '/Albedo/' + year)
    else:
        for snowMode in snowModes:
            inputs.append(gaRootDir + '/Inversion/' + snowMode + '/' + year)

#### Upscaling/Mosaicing ####

m = PMonitor(inputs, 
             request='ga-l3-mosaic-avhrrgeo_alldays', 
             logdir='log',
             hosts=[('localhost',16)],
	     types=[('ga-l3-albedomosaic-avhrrgeo_alldays-step.sh',16)])
     
startDoy = '001'
#endDoy = '365'
endDoy = '031'

for year in years:
    for snowMode in snowModes:
        for resolution in resolutions:
                for proj in projections:

                    ### the simplified way: Albedo tiles --> Albedo mosaic
                    albedoTileDir = gaRootDir + '/Albedo/' + year
                    albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + year + '/' + resolution
                    m.execute('ga-l3-albedomosaic-avhrrgeo_alldays-step.sh', [albedoTileDir], [albedoMosaicDir], parameters=[year,startDoy,endDoy,snowMode,resolution,proj,tileSize,gaRootDir,beamDir])

        #for idoy in range(0,365):    
        #for idoy in range(0,5):    
        #    doy = str(idoy+1).zfill(3) # daily
        #    for resolution in resolutions:
        #        for proj in projections:
        #  
        #            ### the simplified way: Albedo tiles --> Albedo mosaic, no alpha/sigma output
        #            albedoTileDir = gaRootDir + '/Albedo/' + year
        #            albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + year + '/' + resolution
        #            m.execute('ga-l3-albedomosaic-simple-avhrrgeo-step.sh', [albedoTileDir], [albedoMosaicDir], parameters=[year,doy,snowMode,resolution,proj,tileSize,gaRootDir,beamDir])


# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
