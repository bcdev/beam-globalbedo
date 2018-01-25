import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

##################################################################################################
### Provides one step:
###    - staging 'nc2browse' --> png files for each band + BHR RGB from Albedo mosaic netcdf files
##################################################################################################

years=['2005']
#years=['2001','2002']

#doys=['017','181']  # test!!

snowModes=['Merge']
resolutions=['005']
projections=['PC']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
stagingListsRootDir = gaRootDir + '/staging/lists'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-nc2browse-spectral', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-staging-nc2browse-step.sh',64)])

### matching for all years, snowModes, res, proj:
for year in years:
    for snowMode in snowModes:
        for res in resolutions:
            albedoMosaicDir = gaRootDir + '/Mosaic/Albedo_spectral/' + snowMode + '/' + year
            for proj in projections:
                #for idoy in range(215,216):
                for idoy in range(120,151):  # May
                #for idoy in range(0,365):
                    doy = str(idoy+1).zfill(3)             
                    stagingNc2browseResultDir = gaRootDir + '/staging/QL/albedo_spectral/' + snowMode + '/' + year + '/' + res + '/' + proj
                    stagingNc2browseFile = albedoMosaicDir + '/Qa4ecv.albedo.spectral.' + snowMode + '.' + res + '.' + year + doy + '.' + proj + '.nc' 

                    m.execute('ga-l3-staging-nc2browse-spectral-step.sh', ['dummy'], [stagingNc2browseResultDir], 
                          parameters=[year,doy,stagingNc2browseFile, stagingNc2browseResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
