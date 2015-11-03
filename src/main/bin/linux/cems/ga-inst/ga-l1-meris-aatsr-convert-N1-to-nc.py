import glob
import os
import os
import ntpath

import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

years = ['2005']    #test  
allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#################################################################

def get_file_basename(filepath):
    return os.path.splitext(ntpath.basename(filepath))[0]

def getMonth(year):
    if year == '2002':
        return ['06', '07', '08', '09', '10', '11', '12']
    if year == '2012':
        return ['01', '02', '03', '04']
    return ['05']
    #return allMonths

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
        return 1  # test!!
        #return 31

######################## MERIS-AATSR N1 --> nc : ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
#gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GaTestData'

#beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

upperLat = '85.0'
lowerLat = '75.0'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l1-meris-aatsr-convert-N1-to-nc',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l1-meris-aatsr-convert-N1-to-nc.sh',192)])

for year in years:
    for month in getMonth(year):
        for day in range(1, getNumMonthDays(year, int(month))+1):
            merisL1bDir = gaRootDir + '/L1b/MERIS/' + year + '/' + month + '/' + str(day).zfill(2) + '/' 

            if os.path.exists(merisL1bDir):
                merisL1bFiles = os.listdir(merisL1bDir)
                if len(merisL1bFiles) > 0:
                    merisNcDir = gaRootDir + '/L1b/MERIS_nc' + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/'
                    print 'merisL1bDir: ', merisL1bDir 
                    print 'merisNcDir: ', merisNcDir 
                    for index in range(0, len(merisL1bFiles)):
                        if merisL1bFiles[index].endswith(".N1") or merisL1bFiles[index].endswith(".N1.gz"):
                            merisL1bProductPath = merisL1bDir + merisL1bFiles[index]
		 	    merisL1bProductBase = get_file_basename(get_file_basename(merisL1bFiles[index])) # call twice as we have usually filebase.N1.gz
                            merisNcProductPath = merisNcDir + merisL1bProductBase + '.nc'
			    m.execute('ga-l1-meris-aatsr-convert-N1-to-nc-step.sh', 
                                      ['dummy'], [merisNcDir], 
                                      parameters=[merisL1bProductPath,merisL1bFiles[index],merisNcProductPath,year,month,str(day).zfill(2),gaRootDir,beamDir,merisNcDir,upperLat,lowerLat])                            

for year in years:
    for month in getMonth(year):
        for day in range(1, getNumMonthDays(year, int(month))+1):
            aatsrL1bDir = gaRootDir + '/L1b/AATSR/' + year + '/' + month + '/' + str(day).zfill(2) + '/ats_toa_1p/'

            if os.path.exists(aatsrL1bDir):
                aatsrL1bFiles = os.listdir(aatsrL1bDir)
                if len(aatsrL1bFiles) > 0:
                    aatsrNcDir = gaRootDir + '/L1b/AATSR_nc' + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/'
                    print 'aatsrL1bDir: ', aatsrL1bDir
                    print 'aatsrNcDir: ', aatsrNcDir
                    for index in range(0, len(aatsrL1bFiles)):
                        if aatsrL1bFiles[index].endswith(".N1") or aatsrL1bFiles[index].endswith(".N1.gz"):
                            aatsrL1bProductPath = aatsrL1bDir + aatsrL1bFiles[index]
                            aatsrL1bProductBase = get_file_basename(get_file_basename(aatsrL1bFiles[index]))
                            aatsrNcProductPath = aatsrNcDir + aatsrL1bProductBase + '.nc'
                            m.execute('ga-l1-meris-aatsr-convert-N1-to-nc-step.sh', 
                                      ['dummy'], [aatsrNcDir], 
                                      parameters=[aatsrL1bProductPath,aatsrL1bFiles[index],aatsrNcProductPath,year,month,str(day).zfill(2),gaRootDir,beamDir,aatsrNcDir,upperLat,lowerLat])

m.wait_for_completion()

