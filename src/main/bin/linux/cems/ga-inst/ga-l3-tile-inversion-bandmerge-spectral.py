import glob
import os
import os.path
import datetime
import time
import commands

from pmonitor import PMonitor

###########################################################################################
#Spectral BRDF band merge startup script
#
__author__ = 'olafd'
###########################################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

spectralInversionRootDir = gaRootDir + '/Inversion_spectral'

beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#tiles = ['h22v02']
#tiles = ['h18v04']
tiles = ['h18v04','h19v09','h22v02','h25v06'] # the 4 GA test tiles

startYear = 2007  # test
endYear = 2007

inputs = ['brdfs']

m = PMonitor(inputs,
             request='ga-l3-tile-inversion-bandmerge-spectral',
             logdir='log',
             hosts=[('localhost',128)], # let's try this number...
             types=[ ('ga-l3-tile-inversion-bandmerge-spectral-step.sh',64)] )

##### LOOP over tiles and years: ####
for tile in tiles:

    ### Spectral BRDF band merge:
    for iyear in range(startYear, endYear+1):

        year = str(iyear)
        startDoy = '001'
        endDoy = '366'

        postCond = 'brdf_bandmerge_spectral_' + year + '_' + tile + '_' + startDoy + '_' + endDoy
        m.execute('ga-l3-tile-inversion-bandmerge-spectral-step.sh',
                     ['brdfs'], 
		     [postCond], 
                     parameters=[tile,year,startDoy,endDoy,gaRootDir,spectralInversionRootDir,beamDir])

        ### cleanup:
        # do this manually for the moment

# wait for processing to complete
m.wait_for_completion()
