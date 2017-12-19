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

sensor = 'MVIRI'

years = ['1982','1983','1984','1985','1986','1987','1988','1989']     
#years = ['1982','1983','1984','1985']     
#years = ['1991','1992','1993','1994','1995','1996','1997','1998','1999']     
#years = ['2000','2001','2002']     
#years = ['2003','2004','2005','2006']     
#years = ['2007','2008','2009','2010']     
#years=['1990']

diskIds = ['000', '057', '063'] 
#diskIds = ['000'] 
#diskIds = ['063'] 
#diskIds = ['057'] 
hIndices = ['09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', 
            '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32']

#hIndices = ['18']

######################## BRF orbits --> tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-meteosat-mviri-bbdr-tiles',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-meteosat-bbdr-tiles-step.sh',192)])

for diskId in diskIds:

    if diskId == '000':
        diskIdString = 'MVIRI_C_BRF'
    elif diskId == '057':
        diskIdString = 'MVIRI_057_C_BRF'
    else:
        diskIdString = 'MVIRI_063_C_BRF'

    for year in years:
        bbdrTileDir = gaRootDir + '/BBDR/MVIRI/' + year 
        brfOrbitDir = gaRootDir + '/BRF_orbits/MVIRI/' + year 
        if os.path.exists(brfOrbitDir):
            brfFiles = os.listdir(brfOrbitDir)
            if len(brfFiles) > 0:
                for index in range(0, len(brfFiles)):
                    if diskIdString in brfFiles[index]:
	                brfOrbitFilePath = brfOrbitDir + '/' + brfFiles[index]
                        #print 'index, brfOrbitFilePath', index, ', ', brfOrbitFilePath
                        for hIndex in hIndices:
                            m.execute('ga-l2-meteosat-bbdr-tiles-step.sh', ['dummy'], 
                                                                           [bbdrTileDir], 
                                                                           parameters=[year,brfOrbitFilePath,brfFiles[index],bbdrTileDir,diskId,hIndex,sensor,gaRootDir,beamDir])

m.wait_for_completion()

