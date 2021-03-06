#!/usr/bin/env python

import sys
import glob
import os
from pmonitor11 import PMonitor

__author__ = 'olafd'

year = sys.argv[1]   # in [1997, 2010]
month = sys.argv[2]  # in [01,12] or ALL
level1RootDir = sys.argv[3] # e.g. /home/globalbedo/Processing/EOData

#####################################################################################################
os.system('banner Starting GlobAlbedo QR')
#####################################################################################################
# L2 for MERIS:
level1Dir = level1RootDir + '/' + 'MERIS'

if month == "ALL":
    inputs = glob.glob1(level1Dir + '/' + year, '*' + year + '*')
else:
    inputs = glob.glob1(level1Dir + '/' + year, '*' + year + month + '*')

inputs.sort()

#requestName = 'globalbedo-l2-'+year+'-'+month+'-request'
requestName = 'request-l2'
m = PMonitor(inputs, request=requestName, hosts=[('localhost',1)])

for l1input in inputs:
    # l1 to l2 to bbdr-tiles 
    m.execute('qr_l2_bbdr MERIS 2005', [l1input], [])
  
# wait for processing to complete
m.wait_for_completion()

# L2 for VGT:
level1Dir = level1RootDir + '/' + 'VGT'

if month == "ALL":
    inputs = glob.glob1(level1Dir + '/' + year, '*' + year + '*')
else:
    inputs = glob.glob1(level1Dir + '/' + year, '*' + year + month + '*')

inputs.sort()

#requestName = 'globalbedo-l2-'+year+'-'+month+'-request'
requestName = 'request-l2'
m = PMonitor(inputs, request=requestName, hosts=[('localhost',1)])

for l1input in inputs:
    # l1 to l2 to bbdr-tiles
    m.execute('qr_l2_bbdr VGT 2005', [l1input], [])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
#requestName = 'globalbedo-l3-'+year+'-'+month+'-request'
bbdrTileDir = '/home/globalbedo/Processing/GlobAlbedo/BBDR'

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
