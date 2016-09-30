import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

################################################################################
# Reprojects AVHRR LTDR BRF global products onto MODIS SIN tiles
#
__author__ = 'olafd'
#
################################################################################


#years = ['1985']    #test  
#years = ['2006']    #test  
#years = ['1989','1990','1991','1992','1993','1994','1995','1996','1997']  
#years = ['1998','1999','2000','2001']     
#years = ['2002','2003','2004','2005']     
years = ['2005']     
#allMonths = ['10', '11', '12']
allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
#allMonths = ['01', '02', '03', '04', '05']
#allMonths = ['06']

#hStart = '18'
#hEnd = '20'

hStart = ['00', '03', '06', '09', '12', '15', '18', '21', '24', '27', '30', '33']
hEnd   = ['02', '05', '08', '11', '14', '17', '20', '23', '26', '29', '32', '35']

######################## BRF orbits --> tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-avhrr-bbdr-tiles',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-avhrr-brf-tiles-step.sh',192)])

for year in years:
    bbdrTileDir = gaRootDir + '/BBDR/AVHRR/' + year 
    brfOrbitDir = gaRootDir + '/BRF_orbits/AVHRR/' + year 
    if os.path.exists(brfOrbitDir):
        brfFiles = os.listdir(brfOrbitDir)
        if len(brfFiles) > 0:
            for index in range(0, len(brfFiles)):
                brfOrbitFilePath = brfOrbitDir + '/' + brfFiles[index]
                for subtilesIndex in range(0, len(hStart)):
                    m.execute('ga-l2-avhrr-bbdr-tiles-step.sh', ['dummy'], 
                                                              [bbdrTileDir], 
                                                              parameters=[brfOrbitFilePath,brfFiles[index],bbdrTileDir,hStart[subtilesIndex],hEnd[subtilesIndex],gaRootDir,beamDir])

m.wait_for_completion()

