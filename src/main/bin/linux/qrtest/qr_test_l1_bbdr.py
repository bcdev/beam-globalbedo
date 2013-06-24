#!/usr/bin/env python


import glob
import os
from pmonitor11 import PMonitor

__author__ = 'marcoz'

year = '2005'
month = '05'

level1Dir = '/home/globalbedo/Processing/EOData/VGT'

#####################################################################################################
#os.system('banner Starting GlobAlbedo QR')
os.system('echo "Starting GlobAlbedo QR: VGT L1 --> BBDR"')
#####################################################################################################
inputs = glob.glob1(level1Dir + '/' + year, 'V2KRNP____20050530F248.ZIP')
inputs.sort()

#requestName = 'globalbedo-l2-'+year+'-'+month+'-request'
requestName = 'request-l2'
m = PMonitor(inputs, request=requestName, hosts=[('localhost',1)])

for l1input in inputs:
    # l1 to l2 to bbdr-tiles 
    m.execute('qr_l2_bbdr VGT 2005', [l1input], [])
    # m.execute('qr_l2_bbdr_vgt', [l1input], [])
  
# wait for processing to complete
m.wait_for_completion()
#####################################################################################################
#os.system('banner Finished')
os.system('echo "Finished"')
os.system('date')
