import glob
import os
import calendar
import datetime
from pmonitor import PMonitor


################################################################################
# Reprojects GMS BRF 'orbit'(disk) products onto MODIS SIN tiles
# AND converts to GA conform BBDR tile products
#
__author__ = 'olafd'
#
################################################################################

years = ['1997','1998','1999']     
#years = ['2000','2001','2002','2003']     
#years = ['2000']
#years = ['2001']

hIndices = ['00', '01', '02', '03', '04', '05', '06', '07', '08', '09', '10',
            '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35'] # for GMS

#hIndices = ['27']

######################## BRF orbits --> tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-gms-bbdr-tiles',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-gms-bbdr-tiles-step.sh',192)])

diskId = '140'
diskIdString = 'VIS02_140_C_BRF'

for year in years:
    bbdrTileDir = gaRootDir + '/BBDR/GMS/' + year 
    brfOrbitDir = gaRootDir + '/BRF_orbits/GMS/' + year 
    if os.path.exists(brfOrbitDir):
        brfFiles = os.listdir(brfOrbitDir)
        if len(brfFiles) > 0:
            for index in range(0, len(brfFiles)):
            #for index in range(0, 3):  # test!!
                if diskIdString in brfFiles[index]:
                    brfOrbitFilePath = brfOrbitDir + '/' + brfFiles[index]
                    #print 'index, brfOrbitFilePath', index, ', ', brfOrbitFilePath
                    for hIndex in hIndices:
                        m.execute('ga-l2-gms-bbdr-tiles-step.sh', 
                                  ['dummy'], 
                                  [bbdrTileDir], 
                                  parameters=[year,brfOrbitFilePath,brfFiles[index],bbdrTileDir,diskId,hIndex,gaRootDir,beamDir])

m.wait_for_completion()

