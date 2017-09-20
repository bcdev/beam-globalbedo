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

#years = ['1999','2000','2001','2002','2003','2004','2005','2006','2007','2008','2009','2010','2011']     
years = ['2012','2013','2014','2015','2016','2017']     
#years = ['2001']     

######################## BRF orbits --> tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
#msslAvhrrMaskRootDir = '/group_workspaces/cems2/qa4ecv/vol1/avhrr_mask'
#msslAvhrrMaskRootDir = '/group_workspaces/cems2/qa4ecv/vol3/avhrr_v5_mssl_mask' # new version provided by SK, 201707
msslAvhrrMaskRootDir = '/group_workspaces/cems2/qa4ecv/vol3/avhrr_v5_jrc/avhrr_corrected/avhrr_mssl_mask' # new version provided by SK, 201708
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-mssl-avhrr-mask-tiles',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-mssl-avhrr-mask-tiles-step.sh',160)])

#for year in years:
for iyear in range(1981,2017):
    year = str(iyear)
    msslAvhrrMaskTileDir = gaRootDir + '/MsslAvhrrMask/' + year 
    #msslAvhrrMaskOrbitDir = msslAvhrrMaskRootDir + '/' + year 
    msslAvhrrMaskOrbitDir = msslAvhrrMaskRootDir + '/nc/' + year # new 201707 
    if os.path.exists(msslAvhrrMaskOrbitDir):
        msslAvhrrMaskFiles = os.listdir(msslAvhrrMaskOrbitDir)
        if len(msslAvhrrMaskFiles) > 0:
            for index in range(0, len(msslAvhrrMaskFiles)):
                msslAvhrrMaskFilePath = msslAvhrrMaskOrbitDir + '/' + msslAvhrrMaskFiles[index]

                # AVH_20050718_001D_900S900N1800W1800E_0005D_BRDF_N18.NC.bz2
                m.execute('ga-l2-mssl-avhrr-mask-tiles-step.sh', 
                          ['dummy'],
                          [msslAvhrrMaskTileDir],
                          parameters=[msslAvhrrMaskFilePath,msslAvhrrMaskFiles[index],msslAvhrrMaskTileDir,gaRootDir,beamDir])

m.wait_for_completion()

