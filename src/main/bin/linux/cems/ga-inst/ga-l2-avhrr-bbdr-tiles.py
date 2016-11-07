import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

from os.path import splitext

################################################################################
# Reprojects AVHRR LTDR BRF global products onto MODIS SIN tiles
#
__author__ = 'olafd'
#
################################################################################

#years = ['1985']    #test  
#years = ['2006']    #test  
#years = ['1989','1990','1991','1992','1993','1994','1995','1996','1997']  
#years = ['1999','2000','2001','2002','2003','2004','2005','2006','2007','2008']     
years = ['2009','2010','2011','2012','2013','2014']     
#years = ['1998']     
#years = ['2002','2003','2004','2005']     
#years = ['2005']     

months = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
#months = ['01']

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
             types=[('ga-l2-avhrr-brf-unzip-step.sh',32), ('ga-l2-avhrr-bbdr-tiles-step.sh',160)])

for year in years:
    bbdrTileDir = gaRootDir + '/BBDR/AVHRR/' + year 
    # gaRootDir/../BRF_orbits/AVHRR/2005/AVHRR_GEOG_0.05DEG_2005_12_19_NOAA-N16_BRF.nc
    #brfOrbitDir = gaRootDir + '/BRF_orbits/AVHRR/' + year 
    # gaRootDir/../../avhrr_jrc/h05-ftp.jrc.it/fapar/BRF/1981/08/AVH_19810803_001D_900S900N1800W1800E_0005D_BRDF_N07.NC.bz2
    for month in months:
        brfOrbitDir = gaRootDir + '/../../avhrr_jrc/h05-ftp.jrc.it/fapar/BRF/' + year + '/' + month 
        if os.path.exists(brfOrbitDir):
            brfFiles = os.listdir(brfOrbitDir)
            if len(brfFiles) > 0:
                for index in range(0, len(brfFiles)):
                #for index in range(0, 3): # test
                    brfOrbitFilePath = brfOrbitDir + '/' + brfFiles[index]
                    brfUnzippedFilePath = gaRootDir + '/tmp/' + splitext(splitext(brfFiles[index])[0])[0] + '.nc'

                    m.execute('ga-l2-avhrr-brf-unzip-step.sh', ['dummy'],
                                                              [brfUnzippedFilePath],
                                                              parameters=[brfOrbitFilePath,gaRootDir])

                    # TODO: in case of multiple NOAA versions per day, take highest version only
                    for subtilesIndex in range(0, len(hStart)):
                        m.execute('ga-l2-avhrr-bbdr-tiles-step.sh', [brfUnzippedFilePath], 
                                                              [bbdrTileDir], 
                                                              parameters=[brfOrbitFilePath,brfFiles[index],bbdrTileDir,hStart[subtilesIndex],hEnd[subtilesIndex],gaRootDir,beamDir])

m.wait_for_completion()

