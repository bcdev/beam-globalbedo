#!/usr/bin/env python


import glob
import os
from pmonitor11 import PMonitor

__author__ = 'marcoz'

sensor = 'VGT'
year = '2005'
month = '05'

level1Dir = '/home/globalbedo/Processing/EOData/VGT'
bbdrTileDir = '/home/globalbedo/Processing/GlobAlbedo/BBDR'

#####################################################################################################
os.system('banner Starting GlobAlbedo QR')
#####################################################################################################
inputs = glob.glob1(level1Dir + '/' + year, '*' + year + month + '*.ZIP')
#inputs = glob.glob1(level1Dir + '/' + year, '*' + year + month + '01*.ZIP')
inputs.sort()

#requestName = 'globalbedo-l2-'+year+'-'+month+'-request'
requestName = 'request-l2'
#m = PMonitor(inputs, request=requestName, hosts=[('localhost',1)])
m = PMonitor(inputs, request=requestName, hosts=[('localhost',4)])

for l1input in inputs:
    # l1 to l2 to bbdr-tiles 
    m.execute('qr_l2_bbdr VGT 2005', [l1input], [])
  
# wait for processing to complete
m.wait_for_completion()
#####################################################################################################
#requestName = 'globalbedo-l3-'+year+'-'+month+'-request'
requestName = 'request-l3'
m = PMonitor([], request=requestName, hosts=[('localhost',4)])

tiles = glob.glob1(bbdrTileDir+'/'+sensor+'/'+year, 'h??v??')
tiles.sort()

for tile in tiles:
    ## all-in-one
    m.execute('qr_l3_tile ' + tile, [], [])  


# wait for processing to complete
m.wait_for_completion()
#####################################################################################################
#requestName = 'globalbedo-mosaic-'+year+'-'+month+'-request'
requestName = 'request-mosaic'
m = PMonitor([], request=requestName, hosts=[('localhost',1)])

## albedo
m.execute('qr_l3_mosaic', [], [])      

# wait for processing to complete
m.wait_for_completion()
#####################################################################################################
os.system('banner Finished')
os.system('date')
