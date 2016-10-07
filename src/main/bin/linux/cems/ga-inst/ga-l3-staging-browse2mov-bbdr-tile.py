import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#############################################################################################################################
### Provides one step:
###    - staging 'browse2mov' --> 1-year movies from png browse files for each band + BHR RGB from Albedo mosaic netcdf files
#############################################################################################################################

years=['2005']
#snowModes=['Merge']
snowModes=['NoSnow']
#resolutions=['005','05']
resolutions=['005']
projections=['PC','SIN']
bands=['BHR_NIR','BHR_SW','BHR_SW.BHR_NIR.BHR_VIS','BHR_VIS','WNSamples']
#bands=['BHR_NIR','BHR_SW','BHR_SW.BHR_NIR.BHR_VIS','BHR_VIS','DHR_NIR','DHR_SW','DHR_VIS','RelEntropy','WNSamples']
#bands=['BHR_NIR','BHR_SW','BHR_SW.BHR_NIR.BHR_VIS','BHR_SW_CoV','BHR_VIS','DHR_NIR','DHR_SW','DHR_VIS','RelEntropy','WNSamples']
#bands=['BHR_SW','WNSamples']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-browse2mov', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-staging-browse2mov-step.sh',64)])

### matching for all years, snowModes, res, proj:
for year in years:
    for snowMode in snowModes:
        for res in resolutions:
            for proj in projections:
                for band in bands:
                    stagingMoviesInputDir = gaRootDir + '/staging/QL/albedo/' + snowMode + '/' + year + '/' + res + '/' + proj + '/' + band
                    stagingMoviesResultDir = gaRootDir + '/staging/Movies/albedo/' + snowMode + '/' + year + '/' + res + '/' + proj
                    m.execute('ga-l3-staging-browse2mov-step.sh', ['dummy'], [stagingMoviesResultDir], 
                              parameters=[year,snowMode,res,proj,band,stagingMoviesInputDir, stagingMoviesResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
