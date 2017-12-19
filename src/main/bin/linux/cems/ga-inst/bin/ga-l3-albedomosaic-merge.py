import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides merge of AVHRR/GEO albedo snow and nosnow mosaics
########################################################################

#######
sensorID = 'avh_geo'
#######

#years = ['1982','1983','1984','1985','1986','1987','1988','1989','1990']  
#years = ['2001','2002','2003','2004','2005','2006','2007','2008','2009','2010']  
#years = ['2011','2012','2013','2014','2015','2016']  
#years = ['1995','1996','1997','1998','1999']  
#years = ['1991','1992','1993','1994','1995']  
#years = ['1987','1986','1986','1985','1984','1983','1982']  
years = ['2002']  

snowModes = ['Merge']
#snowModes = ['Snow']
#snowModes = ['NoSnow','Snow']

#resolutions = ['05', '005']
#resolutions = ['005']
resolutions = ['05']

#projections = ['SIN', 'PC']
#projections = ['SIN']
projections = ['PC']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#### Upscaling/Mosaicing ####

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l3-albedomosaic-merge', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('ga-l3-albedomosaic-merge-step.sh',64)])
     
for year in years:
#for iyear in range(1981,2015):    
    #year = str(iyear+1)
    for idoy in range(0,365):    
    #for idoy in range(0,31):    
    #for idoy in range(159,160):    
    #for idoy in range(324,325):    
        doy = str(idoy+1).zfill(3) # daily
        for resolution in resolutions:
            for proj in projections:
                albedoMosaicDir = gaRootDir + '/Mosaic/Albedo/Merge/' + sensorID + '/' + year + '/' + resolution
                m.execute('ga-l3-albedomosaic-merge-step.sh', 
                          ['dummy'], [albedoMosaicDir], 
                          parameters=[year,doy,resolution,proj,gaRootDir,beamDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
