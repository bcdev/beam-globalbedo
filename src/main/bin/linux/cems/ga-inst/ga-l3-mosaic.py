import glob
import os
import datetime
from pmonitor import PMonitor
#from pmonitor_ga import PMonitor

__author__ = 'olafd'

years = ['2003']    #test  
doys = ['65']
snowModes = ['snow', 'nosnow', 'merge']
#scalings = ['6', '30', '60']
resolutions = ['05', '025', '005']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'

brdfMosaicDir = gaRootDir + '/Mosaic/brdf'

#### Upscaling/Mosaicing ####

m = PMonitor([gaRootDir], 
             request='ga-l3-mosaic', 
             logdir='log',
             hosts=[('localhost',16)],
	     types=[('ga-l3-mosaic-step.sh',16)])

## BRDF mosaicking
     
for year in years:
    for doy in doys:
        for snowMode in snowModes:
            for resolution in resolutions:
                brdfMosaicDir = gaRootDir + '/Mosaic/brdf/' + snowMode + '/' + resolution
                m.execute('ga-l3-mosaic-step.sh', [gaRootDir], [brdfMosaicDir], parameters=[year,doy,snowMode,resolution,gaRootDir,beamDir])


## BRDF mosaic --> Albedo mosaic
# TODO

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
