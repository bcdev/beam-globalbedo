import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides merge of AVHRR/GEO albedo snow and nosnow mosaics
########################################################################

#years = ['2005']  
years = ['1999']  
#years = ['1998','1999','2000']  

#doys = ['016', '121']
#doys = ['001', '009', '033', '041', '063', '069', '094', '130']

snowModes = ['Merge']

resolutions = ['005']

#projections = ['SIN', 'PC']
projections = ['PC']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#### Upscaling/Mosaicing ####

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l3-albedomosaic-spectral-merge', 
             logdir='log',
             hosts=[('localhost',32)],
	     types=[('ga-l3-albedomosaic-spectral-merge-step.sh',64)])
     
for year in years:
    #for doy in doys:
    for idoy in range(0,365):    
    #for idoy in range(166,167):    
    #for idoy in range(120,151):    
        doy = str(idoy+1).zfill(3) # daily
        for resolution in resolutions:
            for proj in projections:
                albedoMosaicDir = gaRootDir + '/Mosaic/Albedo_spectral/Merge/' + year
                m.execute('ga-l3-albedomosaic-spectral-merge-step.sh', 
                          ['dummy'], [albedoMosaicDir], 
                          parameters=[year,doy,gaRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
