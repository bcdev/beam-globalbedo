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

#mosaicMode = 'simple'
mosaicMode = 'default'

###########################
# set MODIS tile size
#tileSize='1200' # classic
tileSize='200' # AVHRR/GEO
###########################

#years = ['2003']    #test  
#years = ['2001']    #test  
#years = ['2003']    #test  
years = ['2005']    #test  
#snowModes = ['Snow', 'NoSnow', 'Merge']
snowModes = ['NoSnow']
#resolutions = ['05', '005']
resolutions = ['005']
#resolutions = ['05']

doys = []
for i in range(46): # one year
#for i in range(4):   # one month
#for i in range(1):   # one doy
    doy = 8*i + 1
    #doy = 8*i + 121  # May01
    doys.append(str(doy).zfill(3))

#doys = ['001', '121', '361']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
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
             hosts=[('localhost',128)],
	     types=[('ga-l3-brdfmosaic-step.sh', 64), ('ga-l3-albedomosaic-step.sh',64)])
     
for year in years:
    for snowMode in snowModes:
        brdfTileDir = gaRootDir + '/Inversion/' + snowMode + '/' + year

        #for doy in doys:    
        for idoy in range(0,365):    
        #for idoy in range(0,5):    
            doy = str(idoy+1).zfill(3)
            for resolution in resolutions:

                if mosaicMode == 'simple':
                    ### the simplified way: Albedo tiles --> Albedo mosaic, no alpha/sigma output
                    albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + resolution
                    ### TODO: set correct input dir! we have no BRDF mosaic dir
                    #m.execute('ga-l3-albedomosaic-simple-step.sh', [brdfMosaicDir], [albedoMosaicDir], parameters=[year,doy,snowMode,resolution,gaRootDir,beamDir])
                else:
                    ### the Alex Loew energy conservation way (as requested in GA and more precise, but slower: double number of jobs)                
                    # BRDF tiles --> BRDF mosaic:
                    brdfMosaicDir = gaRootDir + '/Mosaic/brdf/' + snowMode + '/' + resolution
                    m.execute('ga-l3-brdfmosaic-step.sh', [brdfTileDir], [brdfMosaicDir], parameters=[year,doy,snowMode,resolution,tileSize,gaRootDir,beamDir])

                    ## BRDF mosaic --> Albedo mosaic
                    albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + resolution
                    m.execute('ga-l3-albedomosaic-step.sh', [brdfMosaicDir], [albedoMosaicDir], parameters=[year,doy,snowMode,resolution,gaRootDir,beamDir])


# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
