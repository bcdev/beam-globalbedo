import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides two steps:
###    - BRDF tiles --> BRDF mosaic
###    - BRDF mosaic --> Albedo mosaic
########################################################################


#years = ['2003']    #test  
years = ['2005']    #test  
#snowModes = ['Snow', 'NoSnow', 'Merge']
snowModes = ['Merge']
resolutions = ['05', '025', '005']
#resolutions = ['05', '005']
#resolutions = ['05']

doys = []
#for i in range(45): # one year
#for i in range(4):   # one month
for i in range(1):   # one doy
    #doy = 8*i + 1
    doy = 8*i + 121
    doys.append(str(doy).zfill(3))

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
inputs = []
for year in years:
    for snowMode in snowModes:
        inputs.append(gaRootDir + '/Inversion/' + snowMode + '/' + year)

# we have now the input dirs:
#gaRootDir + '/Inversion/' + year + tile + '/merge'
#gaRootDir + '/Inversion/' + year + tile + '/snow'
#gaRootDir + '/Inversion/' + year + tile + '/nosnow'
# --> we got rid of the 'Merge' folder!

#### Upscaling/Mosaicing ####

m = PMonitor(inputs, 
             request='ga-l3-mosaic', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-mosaic-step.sh',32), ('ga-l3-mosaic-brdf-albedo-step.sh',32)])
     
for year in years:
    for snowMode in snowModes:
        brdfTileDir = gaRootDir + '/Inversion/' + snowMode + '/' + year

        for doy in doys:    
            for resolution in resolutions:
                
                # BRDF tiles --> BRDF mosaic:
                brdfMosaicDir = gaRootDir + '/Mosaic/brdf/' + snowMode + '/' + resolution
                m.execute('ga-l3-brdfmosaic-step.sh', [brdfTileDir], [brdfMosaicDir], parameters=[year,doy,snowMode,resolution,gaRootDir,beamDir])
                #m.execute('ga-l3-brdfmosaic-step.sh', [brdfTileDir], [brdfMosaicDir], parameters=[year,doy,snowMode,resolution,'false',gaRootDir,beamDir])
                #m.execute('ga-l3-brdfmosaic-step.sh', [brdfTileDir], [brdfMosaicDir], parameters=[year,doy,snowMode,resolution,'true',gaRootDir,beamDir]) # reprojection to Plate Carree, TODO: fix!
                
for year in years:
    for snowMode in snowModes:
        for doy in doys:
            for resolution in resolutions:

                ## BRDF mosaic --> Albedo mosaic
                brdfMosaicDir = gaRootDir + '/Mosaic/brdf/' + snowMode + '/' + resolution
                albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + resolution
                m.execute('ga-l3-albedomosaic-step.sh', [brdfMosaicDir], [albedoMosaicDir], parameters=[year,doy,snowMode,resolution,gaRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
