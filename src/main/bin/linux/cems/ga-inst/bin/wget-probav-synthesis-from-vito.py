__author__ = 'olafd'

# This is a script to download Synthesis S1_TOA 1km froducts from VITO

import os
import sys
import calendar

from pmonitor import PMonitor

#######################################################################################
def getNumMonthDays(year, month):
    if month == 2:
        if calendar.isleap(year):
            return 29
        else:
            return 28
    elif month == 4 or month == 6 or month == 9 or month == 11:
        return 30
    else:
        #return 1  # test!!
        return 31
######################################################################################
def getVersion(year, month, day):

    # see Product_User_Manual.pdf, p.32:
    # 20131016-20131127: V003
    # 20131128-20140616: V002
    # 20141017-        : V001

    if year >= 2015:
        return '001'
    elif year == 2014:
        if month > 6:
            return '001'
        elif month == 6:
            if day > 16:
                return '001'
            else:
                return '002'
        else:
            return '002'
        return 30
    else:
        if month > 11:
            return '002'
        elif month == 11:
            if day > 27:
                return '002'
            else:
                return '003'
        else:
            return '003'

######################################################################################


rootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest/L1b/PROBAV'
years = ['2013','2016']

inputs = ['dummy']
m = PMonitor(inputs,
             request='wget-probav-synthesis-from-vito',
             logdir='log',
             hosts=[('localhost',16)],
             types=[('wget-probav-synthesis-from-vito-step.sh',16)])

print 'starting...'

for year in years:
    iyear = int(year)
    for imonth in range(1, 13):
    #for imonth in range(1, 4):
        month = str(imonth)
        month02 = str(imonth).zfill(2)
        monthDir = rootDir + '/' + year + '/' + month
        numDays = getNumMonthDays(iyear, imonth)
        for iday in range(1, numDays+1):
        #for iday in range(1, 4):
            day = str(iday)
            day02 = str(iday).zfill(2)
            version = getVersion(iyear, imonth, iday)
            dayDir = monthDir + "/" + day
            m.execute('wget-probav-synthesis-from-vito-step.sh', ['dummy'], [dayDir], parameters=[dayDir,year,month,month02,day,day02,version])

m.wait_for_completion()

