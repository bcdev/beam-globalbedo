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

#year = '2005'
year = '2011'
tile = 'h17v17'
#sensor = 'MVIRI'
sensor = 'AVHRR'
noaa_version = 'N19' # needed for AVHRR only, make sure we have the right one for given year
#sensor = 'SEVIRI'
lonGeo = '000' # needed for MVIRI only
#lonGeo = '063'
#bands = ['BB_SW', 'sig_BB_SW_SW', 'Kvol_BRDF_SW', 'Kgeo_BRDF_SW']
bands = ['BB_SW']

#plot_min = {
#'BB_SW' : '0.0',
#'sig_BB_SW_SW' : '0.0',
#'Kvol_BRDF_SW' : '0.0',
#'Kgeo_BRDF_SW' : '-1.0'
#}
#plot_max = {
#'BB_SW' : '0.5',
#'sig_BB_SW_SW' : '0.5',
#'Kvol_BRDF_SW' : '0.1',
#'Kgeo_BRDF_SW' : '0.0'
#}

plot_min = {
'BB_SW' : '0.0'
}
plot_max = {
'BB_SW' : '0.5'
}


gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-nc2browse-bbdr-tile', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-staging-nc2browse-bbdr-tile-step.sh',64)])

bbdrTileDir = gaRootDir + '/BBDR/' + sensor + '/' + year + '/' + tile 
#for idoy in range(0,10):
for idoy in range(0,365):
    doy = str(idoy+1).zfill(3)             
    thisdate = getDateFromDoy(int(year), idoy+1) # has format yyyyMMdd
    datestring = str(thisdate)
    stagingNc2browseResultDir = gaRootDir + '/staging/QL/bbdr/' + sensor + '/' + year + '/' + tile
    if sensor == 'MVIRI':
        if lonGeo == '000': 
            # W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_C_BBDR_EUMP_20050114000000_h17v08.nc
            stagingNc2browseFile = bbdrTileDir + '/W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_C_BBDR_EUMP_' + year + datestring[5:7] + datestring[8:10] + '000000_' + tile + '.nc' 
        else:
            # W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET5+MVIRI_063_C_BRF_EUMP_20050116000000_h17v08
            stagingNc2browseFile = bbdrTileDir + '/W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET5+MVIRI_' + lonGeo + '_C_BBDR_EUMP_' + year + datestring[5:7] + datestring[8:10] + '000000_' + tile + '.nc'
    elif sensor == 'SEVIRI':
        # W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET8+SEVIRI_HRVIS_000_C_BBDR_EUMP_20060101000000_h17v08.nc
        lonGeo = '000' 
        stagingNc2browseFile = bbdrTileDir + '/W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET8+SEVIRI_HRVIS_000_C_BBDR_EUMP_' + year + datestring[5:7] + datestring[8:10] + '000000_' + tile + '.nc'
    else:
       # AVHRR_GEOG_0.05DEG_2004_11_27_NOAA-N16_BRF_h17v08.nc  TODO: change BRF to BBDR in processing!
        lonGeo = 'global' 
       #  stagingNc2browseFile = bbdrTileDir + '/AVHRR_GEOG_0.05DEG_' + year + '_' + datestring[5:7] + '_' + datestring[8:10] + '_NOAA-N16_BRF_' + tile + '.nc' 
       # AVH_20110101_001D_900S900N1800W1800E_0005D_BRDF_N19_h17v17.nc
        stagingNc2browseFile = bbdrTileDir + '/AVH_' + year + datestring[5:7] + datestring[8:10] + '_001D_900S900N1800W1800E_0005D_BRDF_' + noaa_version + '_' + tile + '.nc' 

    
    for band in bands:
        parms = [datestring,tile,sensor,lonGeo,band,plot_min[band],plot_max[band],stagingNc2browseFile, stagingNc2browseResultDir, gaRootDir]
        m.execute('ga-l3-staging-nc2browse-bbdr-tile-step.sh', ['dummy'], [stagingNc2browseResultDir], parameters=parms)

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
