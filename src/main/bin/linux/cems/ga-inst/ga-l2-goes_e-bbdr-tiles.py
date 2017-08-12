import glob
import os
import calendar
import datetime
from pmonitor import PMonitor


################################################################################
# Reprojects GOES_E BRF 'orbit'(disk) products onto MODIS SIN tiles
# AND converts to GA conform BBDR tile products
#
__author__ = 'olafd'
#
################################################################################

sensor = 'GOES_E'

#years = ['2000','2001','2002','2003']     
#years = ['2005','2006','2007']     
years = ['2008','2009','2010','2011']     
#years = ['2004']

hIndices = ['02', '03', '04', '05', '06', '07', '08', '09', '10',
            '11', '12', '13', '14', '15', '16', '17']
#hIndices = ['10']


######################## BRF orbits --> tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-goes_e-bbdr-tiles',
             logdir='log', 
             hosts=[('localhost',128)],
             types=[('ga-l2-goes-bbdr-tiles-step.sh',128)])

diskId = '075'
diskIdString = 'VIS02_-75_C_BRF'

for year in years:
    bbdrTileDir = gaRootDir + '/BBDR/GOES_E/' + year 
    brfOrbitDir = gaRootDir + '/BRF_orbits/GOES_E/' + year 
    if os.path.exists(brfOrbitDir):
        brfFiles = os.listdir(brfOrbitDir)
        if len(brfFiles) > 0:
            for index in range(0, len(brfFiles)):
            #for index in range(0, 2):  # test
                if diskIdString in brfFiles[index]:
                    brfOrbitFilePath = brfOrbitDir + '/' + brfFiles[index]
                    #print 'index, brfOrbitFilePath', index, ', ', brfOrbitFilePath
                    for hIndex in hIndices:
                        m.execute('ga-l2-goes-bbdr-tiles-step.sh', 
                                  ['dummy'], 
                                  [bbdrTileDir], 
                                  parameters=[brfOrbitFilePath,brfFiles[index],bbdrTileDir,diskId,hIndex,sensor,gaRootDir,beamDir])

m.wait_for_completion()

