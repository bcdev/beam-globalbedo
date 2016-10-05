import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

##################################################################################################
### Provides one step:
###    - staging 'nc2browse' --> png files for each band BB_VIS, BB_NIR, BB_SW from BBDR tile netcdf files
##################################################################################################


##################################################################################
def getDateFromDoy(year, doy):
    return datetime.date(year, 1, 1) + datetime.timedelta(doy - 1)
##################################################################################

year = '2005'
tile = 'h17v08'
sensor = 'MVIRI'
#sensor = 'AVHRR'
lonGeo = '000'
#lonGeo = '063'

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-nc2browse-bbdr-tile', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-staging-nc2browse-bbdr-tile-step.sh',64)])

bbdrTileDir = gaRootDir + '/BBDR/' + sensor + '/' + year + '/' + tile 
for idoy in range(0,365):
#for idoy in range(0,31):
   doy = str(idoy+1).zfill(3)             
   thisdate = getDateFromDoy(int(year), idoy+1) 
   datestring = str(thisdate)
   stagingNc2browseResultDir = gaRootDir + '/staging/QL/bbdr/' + sensor + '/' + year + '/' + tile
   if sensor == 'MVIRI':
       if lonGeo == '000': 
           # W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_C_BBDR_EUMP_20050114000000_h17v08.nc
           stagingNc2browseFile = bbdrTileDir + '/W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_C_BBDR_EUMP_' + year + datestring[5:7] + datestring[8:10] + '000000_' + tile + '.nc' 
       else:
           # W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET5+MVIRI_063_C_BRF_EUMP_20050116000000_h17v08
           stagingNc2browseFile = bbdrTileDir + '/W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET5+MVIRI_063_C_BRF_EUMP_' + year + datestring[5:7] + datestring[8:10] + '000000_' + tile + '.nc' 

       # todo: AVHRR

       m.execute('ga-l3-staging-nc2browse-bbdr-tile-step.sh', ['dummy'], [stagingNc2browseResultDir], 
       parameters=[year,doy,tile,sensor,stagingNc2browseFile, stagingNc2browseResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
