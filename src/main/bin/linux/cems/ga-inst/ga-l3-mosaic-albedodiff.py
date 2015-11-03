import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides one steps:
###    - Albedo differences |old - new|: tiles --> mosaic
########################################################################


years = ['2005']    #test  
resolutions = ['005']

doys = []
for i in range(46): # one year
#for i in range(45): # test: skip Doy 361
#for i in range(4):   # one month
#for i in range(1):   # one doy
    doy = 8*i + 1
    #doy = 8*i + 121  # May01
    doys.append(str(doy).zfill(3))

#doys = ['001', '121']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
# albedo diff products are e.g. in /group_workspaces/cems2/qa4ecv/vol1/diff_oldnew_albedo/Albedo/2005/h18v04/GlobAlbedo.albedo.2005121.h18v04.nc
albedodiffRootDir = '/group_workspaces/cems2/qa4ecv/vol1/diff_oldnew_albedo/'
#beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'
inputs = []
for year in years:
    inputs.append(albedodiffRootDir + '/Albedo/' + year)

#### Upscaling/Mosaicing ####

m = PMonitor(inputs, 
             request='ga-l3-mosaic-albedodiff', 
             logdir='log',
             hosts=[('localhost',16)],
	     types=[('ga-l3-albedodiff-mosaic-step.sh',16)])
     
for year in years:
    albedodiffSrcDir = albedodiffRootDir + '/Albedo/' + year
    for doy in doys:    
        # Albedodiff tiles --> mosaic:
        albedodiffMosaicDir = gaRootDir + '/Mosaic/albedodiff/005/'
        m.execute('ga-l3-albedodiff-mosaic-step.sh', [albedodiffSrcDir], [albedodiffMosaicDir], parameters=[year,doy,gaRootDir,albedodiffRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
