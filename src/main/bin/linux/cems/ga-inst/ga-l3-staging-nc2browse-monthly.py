import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

##################################################################################################
### Provides one step:
###    - staging 'nc2browse' --> png files for each band + BHR RGB from Albedo mosaic netcdf files
##################################################################################################

years=['2005']
#years=['2001','2002']

startYear=1983
endYear=2016

#years = [
#         '1982','1983','1984','1985','1986','1987','1988','1989','1990',
#         '1991','1992','1993','1994',
#         '1995','1996','1997','1998','1999','2000',
#         '2001','2002','2003','2004','2005','2006','2007','2008','2009','2010',
#         '2011','2012','2013','2014','2015','2016'
#]

#doys=['017','181']  # test!!

snowModes=['Merge']
#snowModes=['NoSnow']
#snowModes=['Snow']
#snowModes=['NoSnow','Snow']
#snowModes=['NoSnow','Snow','Merge']
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
             request='ga-l3-staging-nc2browse-monthly', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-staging-nc2browse-monthly-step.sh',64)])

### matching for all years, snowModes, res, proj:
#for year in years:
for iyear in range(startYear, endYear+1):
    year = str(iyear)
    for snowMode in snowModes:
        for res in resolutions:
            albedoMosaicDir = gaRootDir + '/Mosaic/Albedo_monthly/' + snowMode + '/' + year
            for proj in projections:
                for imonth in range(0,12):
                    month = str(imonth+1)             
                    stagingNc2browseResultDir = gaRootDir + '/staging/QL/albedo_monthly/' + snowMode + '/' + year + '/' + res + '/' + proj
                    # Qa4ecv.albedo.avh_geo.Merge.05.monthly.1982.5.PC.nc
                    stagingNc2browseFile = albedoMosaicDir + '/Qa4ecv.albedo.avh_geo.' + snowMode + '.' + res + '.monthly.' + year + '.' + month + '.' + proj + '.nc' 

                    m.execute('ga-l3-staging-nc2browse-monthly-step.sh', ['dummy'], [stagingNc2browseResultDir], 
                          parameters=[year,month,snowMode,res,proj,stagingNc2browseFile, stagingNc2browseResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
