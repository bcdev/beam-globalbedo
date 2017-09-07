import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

########################################################################
### Provides two steps:
###    - BRDF tiles --> BRDF mosaic
###    - BRDF mosaic --> Albedo mosaic
### or in 'simple' mode, see below:
###    - Albedo tiles --> Albedo mosaic with reduced output
########################################################################

mosaicMode = 'simple'
#mosaicMode = 'default'

###########################
# set MODIS tile size
tileSize='200' # AVHRR/GEO
###########################

#######
sensorID = 'avh_geo' # must be one of: '/', 'avh', 'geo', 'avh_geo'
#sensorID = '/' # must be one of: '/', 'avh', 'geo', 'avh_geo'
#######

#years = ['1981']    #test  
years = ['2002']  
#years = ['2009','2010']    #test  
#years = ['2009']    #test  
#years = ['2008','2009','2010']  
#years = ['2004','2005','2006','2007']   
#years = ['1984','1985','1986','1987','1988']  
#years = ['1981','1982','1983','1984','1985','1986','1987','1988','1989','1990',
#         '1991','1992','1993','1994','1995','1996','1997','1998','1999','2000',
#         '2001','2002','2003','2004','2005','2006','2007','2008','2009','2010',
#         '2011','2012','2013','2014']  

snowModes = ['NoSnow']
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

# April 2017: noSnow AND snow, e.g. ../GlobAlbedoTest/Albedo/NoSnow/avh_geo/1989/h18v04
inputs = []
for year in years:
    if mosaicMode == 'simple':
        #inputs.append(gaRootDir + '/Albedo/' + year)
        #inputs.append(gaRootDir + '/Albedo/' + sensorID + '/' + year)
        for snowMode in snowModes:
            inputs.append(gaRootDir + '/Albedo/' + snowMode + '/' + sensorID + '/' + year)
    else:
        for snowMode in snowModes:
            inputs.append(gaRootDir + '/Inversion/' + snowMode + '/' + year)

# we have now the input dirs:
#gaRootDir + '/Inversion/' + year + tile + '/merge'
#gaRootDir + '/Inversion/' + year + tile + '/snow'
#gaRootDir + '/Inversion/' + year + tile + '/nosnow'
# --> we got rid of the 'Merge' folder!

#### Upscaling/Mosaicing ####

m = PMonitor(inputs, 
             request='ga-l3-mosaic-avhrrgeo', 
             logdir='log',
             hosts=[('localhost',16)], # test 20170816
	     types=[('ga-l3-brdfmosaic-avhrrgeo-step.sh', 8), ('ga-l3-albedomosaic-avhrrgeo-step.sh',8), ('ga-l3-albedomosaic-simple-avhrrgeo-step.sh',16)])
     
for year in years:
    for snowMode in snowModes:

        #for idoy in range(0,365):    
        for idoy in range(034,035):    
        #for idoy in range(180,181):    
        #for idoy in range(182,206):    
            doy = str(idoy+1).zfill(3) # daily
            #####doy = str(8*idoy+1).zfill(3)   # 8-day
            for resolution in resolutions:
                for proj in projections:

                    if mosaicMode == 'simple':
                        ### the simplified way: Albedo tiles --> Albedo mosaic
                        #albedoTileDir = gaRootDir + '/Albedo/' + year
                        # new Feb 2017: 
                        #albedoTileDir = gaRootDir + '/Albedo/' + sensorID + '/' + year
                        # April 2017: noSnow AND snow, e.g. ../GlobAlbedoTest/Albedo/NoSnow/avh_geo/1989/h18v04
                        albedoTileDir = gaRootDir + '/Albedo/' + snowMode + '/' + sensorID + '/' + year
                        #albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + '/' + year + '/' + resolution
                        albedoMosaicDir = gaRootDir + '/Mosaic/Albedo/' + sensorID + '/' + snowMode + '/' + year + '/' + resolution
                        m.execute('ga-l3-albedomosaic-simple-avhrrgeo-step.sh', [albedoTileDir], [albedoMosaicDir], parameters=[sensorID,year,doy,snowMode,resolution,proj,tileSize,gaRootDir,beamDir])
                    else:
                        ### the Alex Loew energy conservation way (as requested in GA and reported to be more precise, but slower: double number of jobs)
                        ### --> in fact this produces more or less the same as the simple mode in case of mosaicing 200x200 tiles                
                        # BRDF tiles --> BRDF mosaic:
                        brdfTileDir = gaRootDir + '/Inversion/' + snowMode + '/' + year
                        brdfMosaicDir = gaRootDir + '/Mosaic/brdf/' + snowMode + '/' + year + '/' + resolution
                        m.execute('ga-l3-brdfmosaic-avhrrgeo-step.sh', [brdfTileDir], [brdfMosaicDir], parameters=[year,doy,snowMode,resolution,proj,tileSize,gaRootDir,beamDir])

                        ## BRDF mosaic --> Albedo mosaic
                        albedoMosaicDir = gaRootDir + '/Mosaic/albedo/' + snowMode + year + '/' + '/' + resolution
                        m.execute('ga-l3-albedomosaic-avhrrgeo-step.sh', [brdfMosaicDir], [albedoMosaicDir], parameters=[year,doy,snowMode,resolution,proj,gaRootDir,beamDir])


# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#os.system('date')
