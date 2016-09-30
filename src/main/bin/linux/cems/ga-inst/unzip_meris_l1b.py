import glob
import os
import sys
import subprocess
import calendar
import datetime
from os.path import basename

__author__ = 'olafd'

years = ['2005']    #test  
allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#################################################################

def getMonth(year):
    if year == '2002':
        return ['04', '05', '06', '07', '08', '09', '10', '11', '12']
    if year == '2012':
        return ['01', '02', '03', '04']
    return allMonths

def getNumMonthDays(year, month_index):
    if month_index == 2:
        if calendar.isleap(int(year)):      
            return 29 
        else:
            return 28
    elif month_index == 4 or month_index == 6 or month_index == 9 or month_index == 11:
        return 30
    else:
        #return 3  # test!!
        return 31

#################################################################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
merisL1bGzippedRootDir = gaRootDir + '/MERIS'
merisL1bUnzippedRootDir = '/group_workspaces/cems2/qa4ecv/vol2/olafd/GlobAlbedoTest'

for year in years:
    # MERIS L1b files are stored per month...
    for month in getMonth(year):
        if os.path.exists(gaRootDir + '/L1b/MERIS/' + year + '/' + month):
            for day in range(1, getNumMonthDays(year, int(month))+1):
                merisL1bUnzippedTargetDir =  merisL1bUnzippedRootDir + '/L1b/MERIS/' + year + '/' + month + '/' + str(day).zfill(2)
                if not os.path.exists(merisL1bUnzippedTargetDir):
                    mkdircommand = "mkdir -p " + merisL1bUnzippedTargetDir
                    print mkdircommand
                    os.popen(mkdircommand)
                l1bFiles = os.listdir(gaRootDir + '/L1b/MERIS/' + year + '/' + month + '/' + str(day).zfill(2))
                if len(l1bFiles) > 0:
                    # we do not want other files than *.N1 for MERIS:
                    for index in range(0, len(l1bFiles)):
                        if l1bFiles[index].endswith(".N1.gz"):
                            l1bDir = gaRootDir + '/L1b/MERIS/' + year + '/' + month + '/' + str(day).zfill(2)
                            gzippedFilepath = l1bDir + '/' + l1bFiles[index]
                            unzippedFilepath = merisL1bUnzippedTargetDir + '/' + l1bFiles[index].rsplit('.', 1)[0]
                            #print 'basename: ', l1bFiles[index], ', ', l1bFiles[index].rsplit('.', 1)[0]
                            gunzipcommand = "gunzip -c " + gzippedFilepath + ' > ' + unzippedFilepath
                            print gunzipcommand
                            os.popen(gunzipcommand)

