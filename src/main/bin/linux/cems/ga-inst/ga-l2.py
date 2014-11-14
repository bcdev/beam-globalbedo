import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#year = sys.argv[1]   # in [1997, 2010]
year = '2005'    #test  
#month = sys.argv[2]  # in [01,12] or ALL
month = 'ALL'

########################### L1B --> BBDR #################################
### TODO ###

#level1RootDir = sys.argv[3] # e.g. /home/globalbedo/Processing/EOData
# L2 for MERIS:
#level1Dir = level1RootDir + '/' + 'MERIS'
#
#if month == "ALL":
#    inputs = glob.glob1(level1Dir + '/' + year, '*' + year + '*')
#else:
#    inputs = glob.glob1(level1Dir + '/' + year, '*' + year + month + '*')
#
#inputs.sort()
#
#requestName = 'globalbedo-l2-'+year+'-'+month+'-request'
#requestName = 'request-l2'
#m = PMonitor(inputs, request=requestName, hosts=[('localhost',1)])
#
#for l1input in inputs:
#    # l1 to l2 to bbdr-tiles 
#    m.execute('qr_l2_bbdr MERIS 2005', [l1input], [])
#  
# wait for processing to complete
#m.wait_for_completion()
#
# L2 for VGT:
#level1Dir = level1RootDir + '/' + 'VGT'
#
#if month == "ALL":
#    inputs = glob.glob1(level1Dir + '/' + year, '*' + year + '*')
#else:
#    inputs = glob.glob1(level1Dir + '/' + year, '*' + year + month + '*')
#
#inputs.sort()
#
#requestName = 'globalbedo-l2-'+year+'-'+month+'-request'
#requestName = 'request-l2'
#m = PMonitor(inputs, request=requestName, hosts=[('localhost',1)])
#
#for l1input in inputs:
#    # l1 to l2 to bbdr-tiles
#    m.execute('qr_l2_bbdr VGT 2005', [l1input], [])
#
# wait for processing to complete
#m.wait_for_completion()

#####################################################################################################
