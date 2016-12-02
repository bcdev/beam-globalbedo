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

mosaicMode = 'simple'
#mosaicMode = 'default'

###########################
# set MODIS tile size
tileSize='200' # AVHRR/GEO
###########################

years = ['2011']    #test  
#years = ['2001']    #test  
#years = ['2003']    #test  
#years = ['2014']    #test  
#years = ['2005']    #test  
#years = ['2004','2005']    #test  

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

# we have now the input dirs:
#gaRootDir + '/Inversion/' + year + tile + '/merge'
#gaRootDir + '/Inversion/' + year + tile + '/snow'
#gaRootDir + '/Inversion/' + year + tile + '/nosnow'
# --> we got rid of the 'Merge' folder!

#### Upscaling/Mosaicing ####

m = PMonitor(inputs, 
             request='ga-l3-mosaic-avhrrgeo', 
             logdir='log',
             hosts=[('localhost',32)],
	     types=[('ga-l3-brdfmosaic-avhrrgeo-step.sh', 32), ('ga-l3-albedomosaic-avhrrgeo-step.sh',32), ('ga-l3-albedomosaic-simple-avhrrgeo-step.sh',32)])
     
for year in years:
    for snowMode in snowModes:

        for idoy in range(0,365):    
        #for idoy in range(0,5):    
        #for idoy in range(121,153):    
        #for idoy in range(0,4):    
        #for idoy in range(139,365):    
            doy = str(idoy+1).zfill(3) # daily
            #doy = str(8*idoy+1).zfill(3)   # 8-day
            for resolution in resolutions:
                for proj in projections:

                    if mosaicMode == 'simple':
                        ### the simplified way: Albedo tiles --> Albedo mosaic, no alpha/sigma output
                        albedoTileDir = gaRootDir + '/Albedo/' + year
                        albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + resolution
                        m.execute('ga-l3-albedomosaic-simple-avhrrgeo-step.sh', [albedoTileDir], [albedoMosaicDir], parameters=[year,doy,snowMode,resolution,proj,tileSize,gaRootDir,beamDir])
                    else:
                        ### the Alex Loew energy conservation way (as requested in GA and more precise, but slower: double number of jobs)                
                        # BRDF tiles --> BRDF mosaic:
                        brdfTileDir = gaRootDir + '/Inversion/' + snowMode + '/' + year
                        brdfMosaicDir = gaRootDir + '/Mosaic/brdf/' + snowMode + '/' + resolution
                        m.execute('ga-l3-brdfmosaic-avhrrgeo-step.sh', [brdfTileDir], [brdfMosaicDir], parameters=[year,doy,snowMode,resolution,proj,tileSize,gaRootDir,beamDir])

                        ## BRDF mosaic --> Albedo mosaic
                        albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + resolution
                        m.execute('ga-l3-albedomosaic-avhrrgeo-step.sh', [brdfMosaicDir], [albedoMosaicDir], parameters=[year,doy,snowMode,resolution,proj,gaRootDir,beamDir])


# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
