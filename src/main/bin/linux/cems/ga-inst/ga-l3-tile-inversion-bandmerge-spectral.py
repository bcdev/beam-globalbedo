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

#tiles = ['h17v04']
#tiles = ['h18v04']
#tiles = ['h18v04','h19v09','h22v02','h25v06'] # the 4 GA test tiles
tiles = ['h17v02','h18v02','h19v02','h20v02',
         'h17v03','h18v03','h19v03','h20v03',
         'h17v04','h18v04','h19v04','h20v04',
         'h17v05','h18v05','h19v05','h20v05'] # Europe

startYear = 1998  # test
endYear = 2000

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
        endDoy = '365'

        postCond = 'brdf_bandmerge_spectral_' + year + '_' + tile + '_' + startDoy + '_' + endDoy
        m.execute('ga-l3-tile-inversion-bandmerge-spectral-step.sh',
                     ['brdfs'], 
		     [postCond], 
                     parameters=[tile,year,startDoy,endDoy,gaRootDir,spectralInversionRootDir,beamDir])

        ### cleanup:
        # do this manually for the moment

# wait for processing to complete
m.wait_for_completion()
