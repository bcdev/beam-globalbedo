import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

##################################################################################################
### Provides one step:
###    - staging 'nc2browse' --> png files for each band + BHR RGB from Albedo mosaic netcdf files
##################################################################################################

years=['2002']
#years=['1989']

#years=['2001','2002']
#years=['1982','1983','2004','2013']
#years=['2001','2002','2003','2004','2005','2006']
#years=['2011','2012','2013','2014','2015','2016']

#years = [
#'1982','1983','1984','1985','1986','1987','1988','1989','1990',
#         '1991','1992','1993','1994',
#         '1995','1996','1997','1998','1999',
#         '2001','2002','2003','2004','2005','2006','2007','2008','2009','2010',
#         '2011','2012','2013','2014','2015','2016'
#]

#doys=['017','181']  # test!!

#snowModes=['Merge']
#snowModes=['NoSnow']
#snowModes=['Snow']
#snowModes=['NoSnow','Snow']
snowModes=['NoSnow','Snow','Merge']
#resolutions=['005','05']
#resolutions=['005']
resolutions=['05']
#projections=['PC','SIN']
#projections=['SIN']
projections=['PC']

#######
sensorID = 'avh_geo' # must be one of: '/', 'avh', 'geo', 'avh_geo'
#sensorID = '/' # must be one of: '/', 'avh', 'geo', 'avh_geo'
#######

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
stagingListsRootDir = gaRootDir + '/staging/lists'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='ga-l3-staging-nc2browse', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-staging-nc2browse-step.sh',64)])

### matching for all years, snowModes, res, proj:
for year in years:
    for snowMode in snowModes:
        for res in resolutions:
            #albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + year + '/' + res
            #albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + year + '_avhrr/' + res
            #albedoMosaicDir = gaRootDir + '/Mosaic/Albedo/' + snowMode + '/' + year + '_avhrr/' + res
            #albedoMosaicDir = gaRootDir + '/Mosaic/Albedo/' + snowMode + '/' + year + '/' + res
            #albedoMosaicDir = gaRootDir + '/Mosaic/Albedo/' + sensorID + '/' + snowMode + '/' + year + '/' + res
            albedoMosaicDir = gaRootDir + '/Mosaic/Albedo/' + snowMode + '/' + sensorID + '/' + year + '/' + res
            for proj in projections:
                #for idoy in range(180,365):
                #for idoy in range(0,365):
                #for idoy in range(0,366):
                #for idoy in range(365,366):
                #for doy in doys:
                #for idoy in range(120,121):
                #for idoy in range(215,216):
                #for idoy in range(364,365):
                for idoy in range(0,365):
                    doy = str(idoy+1).zfill(3)             
                    stagingNc2browseResultDir = gaRootDir + '/staging/QL/albedo/' + snowMode + '/' + year + '/' + res + '/' + proj
                    #stagingNc2browseFile = albedoMosaicDir + '/GlobAlbedo.albedo.' + snowMode + '.' + res + '.' + year + doy + '.' + proj + '.nc' 
                    
                    # this is what is should be now, 20170216:
                    # stagingNc2browseFile = albedoMosaicDir + '/Qa4ecv.albedo.' + sensorID + '.' + snowMode + '.' + res + '.' + year + doy + '.' + proj + '.nc' 
                    
                    # test:
                    # Qa4ecv.albedo.avhrrgeo.NoSnow.005.2001345.PC.nc
                    stagingNc2browseFile = albedoMosaicDir + '/Qa4ecv.albedo.avh_geo.' + snowMode + '.' + res + '.' + year + doy + '.' + proj + '.nc' 

                    m.execute('ga-l3-staging-nc2browse-step.sh', ['dummy'], [stagingNc2browseResultDir], 
                          parameters=[year,doy,snowMode,res,proj,stagingNc2browseFile, stagingNc2browseResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
