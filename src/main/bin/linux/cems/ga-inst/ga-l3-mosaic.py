import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#year = sys.argv[1]   # in [1997, 2010]
year = '2005'    #test  
#month = sys.argv[2]  # in [01,12] or ALL
month = 'ALL'

#### Upscaling/Mosaicing

## BRDF mosaicking
# TODO

## BRDF mosaic --> Albedo mosaic
# TODO

#####################################################################################################
#requestName = 'globalbedo-mosaic-'+year+'-'+month+'-request'
#requestName = 'request-mosaic'
#m = PMonitor([], request=requestName, hosts=[('localhost',1)])
#
## albedo
#m.execute('gall3-mosaic', [], [])      
#
# wait for processing to complete
#m.wait_for_completion()
#####################################################################################################
#os.system('banner Finished')
#os.system('date')
