#!/usr/bin/env python


import glob
import os
from pmonitor11 import PMonitor

__author__ = 'marcoz'

sensor = 'MERIS'
year = '2005'
month = '05'

level1Dir = '/home/globalbedo/Processing/EOData/MERIS'
bbdrTileDir = '/home/globalbedo/Processing/GlobAlbedo/BBDR'

#####################################################################################################
os.system('banner Resuming GlobAlbedo QR')
#####################################################################################################
requestName = 'request-mosaic'
m = PMonitor([], request=requestName, hosts=[('localhost',1)])

## albedo
m.execute('qr_l3_mosaic', [], [])      

# wait for processing to complete
m.wait_for_completion()
#####################################################################################################
os.system('banner Finished')
os.system('date')
