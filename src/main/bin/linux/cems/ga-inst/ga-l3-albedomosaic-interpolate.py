import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides merge of AVHRR/GEO albedo snow and nosnow mosaics
########################################################################

#######
sensorID = 'avh_geo'
#######

years = ['1984','1988','1992','1996','2000','2004','2008','2012']  
#years = ['2004']  

snowModes = ['Merge']

resolutions = ['05', '005']
#resolutions = ['005']
#resolutions = ['05']

#projections = ['SIN', 'PC']
#projections = ['SIN']
projections = ['PC']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#### Upscaling/Mosaicing ####

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l3-albedomosaic-interpolate', 
             logdir='log',
             hosts=[('localhost',32)],
	     types=[('ga-l3-albedomosaic-interpolate-step.sh',64)])
     
for year in years:
    for resolution in resolutions:
        for proj in projections:
            albedoMosaicDir = gaRootDir + '/Mosaic/Albedo'
            m.execute('ga-l3-albedomosaic-interpolate-step.sh', 
                      ['dummy'], [albedoMosaicDir], 
                      parameters=[year,resolution,proj,gaRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
