import glob
import os
import calendar
import datetime
from pmonitor import PMonitor


################################################################################
# Reprojects Meteosat MVIRI BRF 'orbit'(disk) products onto MODIS SIN tiles
# AND converts to GA conform BBDR tile products
#
__author__ = 'olafd'
#
################################################################################

### 20160922: NOTE: bug found in SZA computation - older results need to be all reprocessed!

sensor = 'SEVIRI'

years = ['2006','2007','2008','2009']     
#years = ['2008','2009']

allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
#allMonths = ['05']
hIndices = ['09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', 
            '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32']

######################## BRF orbits --> tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-meteosat-seviri-bbdr-tiles',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-meteosat-bbdr-tiles-step.sh',192)])

diskId = '000'
diskIdString = 'HRVIS_000_C_BRF'

for year in years:
    bbdrTileDir = gaRootDir + '/BBDR/SEVIRI/' + year 
    brfOrbitDir = gaRootDir + '/BRF_orbits/SEVIRI/' + year 
    if os.path.exists(brfOrbitDir):
        brfFiles = os.listdir(brfOrbitDir)
        if len(brfFiles) > 0:
            for index in range(0, len(brfFiles)):
                if diskIdString in brfFiles[index]:
                    brfOrbitFilePath = brfOrbitDir + '/' + brfFiles[index]
                    #print 'index, brfOrbitFilePath', index, ', ', brfOrbitFilePath
                    for hIndex in hIndices:
                        m.execute('ga-l2-meteosat-bbdr-tiles-step.sh', 
                                  ['dummy'], 
                                  [bbdrTileDir], 
                                  parameters=[brfOrbitFilePath,brfFiles[index],bbdrTileDir,diskId,hIndex,sensor,gaRootDir,beamDir])

m.wait_for_completion()

