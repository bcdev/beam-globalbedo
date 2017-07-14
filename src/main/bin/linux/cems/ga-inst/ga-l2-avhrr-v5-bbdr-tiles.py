import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

from os.path import splitext

################################################################################
# Reprojects AVHRR LTDR BRF global products onto MODIS SIN tiles.
# This script is adapted for processing the new AVHRR BRF based on the new
# AVHRR LTDR v5 dataset (July 2017)
#
__author__ = 'olafd'
#
################################################################################

#years = ['1989']    #test  
#years = ['2000']    #test  
#years = ['1981','1982','1983','1984','1986','1987','1988','1989','1990','2005']  
#years = ['1999','2000','2001','2002','2003','2004','2005','2006','2007','2008']     
years = ['2001','2002','2003','2004','2005']     
#years = ['2004']     
#years = ['2002','2003','2004','2005','2006']     
#years = ['2005']     
#years = ['1993']     
#years = ['2013']     

months = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
#months = ['01']

#hStart = ['12']
#hEnd = ['14']

hStart = ['00', '03', '06', '09', '12', '15', '18', '21', '24', '27', '30', '33']
hEnd   = ['02', '05', '08', '11', '14', '17', '20', '23', '26', '29', '32', '35']

######################## BRF orbits --> tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-avhrr-v5-bbdr-tiles',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-avhrr-v5-brf-unzip-step.sh',32), ('ga-l2-avhrr-v5-bbdr-tiles-step.sh',160)])

## NOAA versions:
#...
# 1981: N07 (191 files)
# 1982: N07 (385 files, month 08 contains duplicated files of 07)
# 1983: N07 (364 files)
# 1984: N07 (357 files)
# 1985: N09 (358 files)
# 1986: N09 (394 files, month 11 contains duplicated files of 12)
# 1987: N09 (364 files)
# 1988: N09 (311 files) + N11 (54 files)
# 1989: N11 (364 files)
# 1990: N11 (364 files)
# 1991: N11 (363 files)
# 1992: N11 (366 files)
# 1993: N11 (335 files)
# 1994: N11 (365 files)
# 1995: N11 (35 files)
# 1996: N14 (365 files)
# 1997: N14 (365 files)
# 1998: N14 (365 files)
# 1999: N14 (364 files)
# 2000: N14 (364 files) + N16 (29 files)
# 2001: N16 (365 files)
# 2002: N16 (363 files)
# 2003: N16 (352 files)
# 2004: N16 (366 files)
# 2005: N16 (364 files) + N18 (176 files)
# 2006: N16 (362 files)
# 2007: N18 (365 files)
# 2008: N18 (366 files)
# 2009: N18 (492 files, 200901-03 contain duplicated files of 2007)
# 2010: N18 (24 files) + N19 (340 files)
# 2011: N18 (5 files) + N19 (360 files)
# 2012: N19 (365 files)
# 2013: N18 (69 files) + N19 (364 files)
# 2014: N19 (365 files)


for year in years:
    bbdrTileDir = gaRootDir + '/BBDR/AVHRR/' + year 
    # gaRootDir/../BRF_orbits/AVHRR/2005/AVHRR_GEOG_0.05DEG_2005_12_19_NOAA-N16_BRF.nc
    #brfOrbitDir = gaRootDir + '/BRF_orbits/AVHRR/' + year 
    # gaRootDir/../../avhrr_jrc/h05-ftp.jrc.it/fapar/BRF/1981/08/AVH_19810803_001D_900S900N1800W1800E_0005D_BRDF_N07.NC.bz2

    for month in months:
        # brfOrbitDir = gaRootDir + '/../../avhrr_jrc/h05-ftp.jrc.it/fapar/BRF/' + year + '/' + month 
	# new 201707:
        # /group_workspaces/cems2/qa4ecv/vol3/avhrr_v5_jrc/h05-ftp.jrc.it/fapar/BRF/1982/01
	brfOrbitDir = '/group_workspaces/cems2/qa4ecv/vol3/avhrr_v5_jrc/h05-ftp.jrc.it/fapar/BRF/' + year + '/' + month

        if os.path.exists(brfOrbitDir):
            brfFiles = os.listdir(brfOrbitDir)
            if len(brfFiles) > 0:
                for index in range(0, len(brfFiles)):
                #for index in range(0, 3): # test
                    brfOrbitFilePath = brfOrbitDir + '/' + brfFiles[index]

                    ## AVH_20050718_001D_900S900N1800W1800E_0005D_BRDF_N18.NC.bz2
		    # new 201707:
		    # AVHRR2_NOAA07_19820102_19820102_L1_BRF_900S900N1800W1800E_PLC_0005D_v03.zip

                    #brfUnzippedFilePath = gaRootDir + '/tmp/' + splitext(splitext(brfFiles[index])[0])[0] + '.nc'
                    #brfUnzippedFilePath = brfOrbitFilePath # files are now unzipped (by Said? 20161122)
                    # 
                    # new 201707: files are zipped again:
                    brfUnzippedFilePath = gaRootDir + '/tmp/' + splitext(splitext(brfFiles[index])[0])[0] + '.NC'

                    m.execute('ga-l2-avhrr-v5-brf-unzip-step.sh', ['dummy'],
                                                              [brfUnzippedFilePath],
                                                              parameters=[brfOrbitFilePath,gaRootDir])

                    for subtilesIndex in range(0, len(hStart)):
                        m.execute('ga-l2-avhrr-v5-bbdr-tiles-step.sh', [brfUnzippedFilePath],  # in case we had to unzip before
                        #m.execute('ga-l2-avhrr-v5-bbdr-tiles-step.sh', ['dummy'],
                                                              [bbdrTileDir],
                                                              parameters=[brfOrbitFilePath,brfFiles[index],bbdrTileDir,hStart[subtilesIndex],hEnd[subtilesIndex],gaRootDir,beamDir])

m.wait_for_completion()
