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
#snowModes=['Merge']
snowModes=['NoSnow']
#resolutions=['005','05']
resolutions=['005']
#projections=['PC','SIN']
#projections=['SIN']
projections=['PC']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
stagingListsRootDir = gaRootDir + '/staging/lists'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-nc2browse', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-staging-nc2browse-step.sh',64)])

### matching for all years, snowModes, res, proj:
for year in years:
    for snowMode in snowModes:
        for res in resolutions:
            for proj in projections:
                stagingNc2browseListFile = gaRootDir + '/staging/lists/nc2browse/list_albedo_mosaics_' + snowMode + '_' + year + '_' + res + '_' + proj + '.txt'
                stagingNc2browseResultDir = gaRootDir + '/staging/QL/albedo/' + snowMode + '/' + year + '/' + res + '/' + proj
                m.execute('ga-l3-staging-nc2browse-step.sh', ['dummy'], [stagingNc2browseResultDir], 
                          parameters=[year,snowMode,res,proj,stagingNc2browseListFile, stagingNc2browseResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
