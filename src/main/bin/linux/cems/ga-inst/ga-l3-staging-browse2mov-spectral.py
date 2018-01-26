import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#############################################################################################################################
### Provides one step:
###    - staging 'browse2mov spectral' --> 1-year movies from png browse files from spectral Albedo mosaic netcdf files
#############################################################################################################################

years=['2005']

snowModes=['Merge']
resolutions=['005']
projections=['PC']
#bands=['BHR_b4','WNSamples']
bands=['BHR_b1','BHR_b2','BHR_b4','WNSamples']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-browse2mov-spectral', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-staging-browse2mov-spectral-step.sh',64)])

### matching for all years, snowModes, res, proj:
for year in years:
    for snowMode in snowModes:
        for res in resolutions:
            for proj in projections:
                for band in bands:
                    stagingMoviesInputDir = gaRootDir + '/staging/QL/albedo_spectral/' + snowMode + '/' + year + '/' + res + '/' + proj + '/' + band
                    stagingMoviesResultDir = gaRootDir + '/staging/Movies/albedo_spectral/' + snowMode + '/' + year + '/' + res + '/' + proj
                    m.execute('ga-l3-staging-browse2mov-spectral-step.sh', ['dummy'], [stagingMoviesResultDir], 
                              parameters=[year,band,stagingMoviesInputDir, stagingMoviesResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
