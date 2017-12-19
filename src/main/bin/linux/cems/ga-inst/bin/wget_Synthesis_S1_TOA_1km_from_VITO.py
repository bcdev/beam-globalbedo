__author__ = 'olafd'

# This is a script to download Synthesis S1_TOA 1km froducts from VITO

# parameters:
## year (e.g. 2014)
## month in [1,12]
# example call: python wget_Synthesis_S1_TOA_1km_from_VITO.py 2014 1

import os
import sys
import subprocess

#######################################################################################
def getNumMonthDays(year, month_index):
    month = int(month_index)
    if month == 2:
        if calendar.isleap(int(year)):
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

    iyear = int(year)
    imonth = int(month)
    iday = int(day)
    if iyear >= 2015:
        return '001'
    elif iyear == 2014:
        if imonth > 6:
            return '001'
        elif imonth == 6:
            if iday > 16:
                return '001'
            else:
                return '002'
        else:
            return '002'
        return 30
    else:
        if imonth > 11:
            return '002'
        elif imonth == 11:
            if iday > 27:
                return '002'
            else:
                return '003'
        else:
            return '003'

######################################################################################


if len(sys.argv) != 3:
    print 'Usage:  python wget_Synthesis_S1_TOA_1km_from_VITO.py <year> <month>'
    print 'example call:  wget_Synthesis_S1_TOA_1km_from_VITO.py 2014 1'
    sys.exit(-1)

rootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest/L1b/PROBAV'
year = sys.argv[1]
month = sys.argv[2]


print 'starting...'

monthDir = rootDir + '/' + year + '/' + month
numDays = getNumMonthDays(year, month)
for index in range(1, numDays+1):
    day = str(index)
    version = getVersion(year, month, day)
    dayDir = monthDir + "/" + day
    if not os.path.exists(dayDir):
        cmd = "mkdir -p " + dayDir
        os.popen(cmd)
        #print(cmd)
    rootUrl = "http://www.vito-eodata.be/PDF/datapool/Free_Data/PROBA-V_1km/S1_TOA_-_1_km/"
    urlSuffix = year + "/" + month + "/" + day + "/PV_S1_TOA-" + year + str(month).zfill(2) + str(day).zfill(2) + "_1KM_V" + version + "/"
    cmd = "wget -P " + dayDir + "/ -r -A \'*.HDF5\' -e robots=off -r --no-parent -nH --cut-dirs=9 --user=oda666 --password=oda666_at_bc " + rootUrl + urlSuffix
    os.popen(cmd)
    #print(cmd)

print 'done.'


