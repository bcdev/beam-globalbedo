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
#years = ['2001']    #test  
#years = ['2003']    #test  
years = ['2005']    #test  
#snowModes = ['Snow', 'NoSnow', 'Merge']
snowModes = ['Merge']
#resolutions = ['05', '005']
resolutions = ['005']

doys = []
for i in range(46): # one year
#for i in range(45): # test: skip Doy 361
#for i in range(4):   # one month
#for i in range(1):   # one doy
    doy = 8*i + 1
    #doy = 8*i + 121  # May01
    doys.append(str(doy).zfill(3))

#doys = ['001', '121', '361']
#doys = ['121']
#doys = ['361']
#doys = ['001','185','361']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
#beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'
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
             hosts=[('localhost',192)],
	     types=[('ga-l3-brdfmosaic-step.sh',168), ('ga-l3-albedomosaic-step.sh',24)])
     
for year in years:
    for snowMode in snowModes:
        brdfTileDir = gaRootDir + '/Inversion/' + snowMode + '/' + year

        for doy in doys:    
            for resolution in resolutions:
                
                # BRDF tiles --> BRDF mosaic:
                brdfMosaicDir = gaRootDir + '/Mosaic/brdf/' + snowMode + '/' + resolution
                m.execute('ga-l3-brdfmosaic-step.sh', [brdfTileDir], [brdfMosaicDir], parameters=[year,doy,snowMode,resolution,gaRootDir,beamDir])

                ## BRDF mosaic --> Albedo mosaic
                albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + resolution
                m.execute('ga-l3-albedomosaic-step.sh', [brdfMosaicDir], [albedoMosaicDir], parameters=[year,doy,snowMode,resolution,gaRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
