import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#years = ['2005']    #test  
years = ['2007']    #test  
allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#################################################################

def getMonth(year):
    if year == '2002':
        return ['04', '05', '06', '07', '08', '09', '10', '11', '12']
    if year == '2012':
        return ['01', '02', '03', '04']
    #return ['05']
    return ['06']
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

######################## MERIS-AATSR Coreg: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
#gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GaTestData'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-meris-aatsr-coreg',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-meris-aatsr-coreg-step.sh',192)])

for year in years:
    for month in getMonth(year):
        for day in range(21, getNumMonthDays(year, int(month))+1):
        #for day in range(1, getNumMonthDays(year, int(month))+1):
            merisL1bDir = gaRootDir + '/L1b/MERIS_nc/' + year + '/' + month + '/' + str(day).zfill(2) + '/' 
            aatsrL1bDir = gaRootDir + '/L1b/AATSR_nc/' + year + '/' + month + '/' + str(day).zfill(2) + '/'

            if os.path.exists(merisL1bDir) and os.path.exists(aatsrL1bDir):
                merisL1bFiles = os.listdir(merisL1bDir)
                aatsrL1bFiles = os.listdir(aatsrL1bDir)
                if len(merisL1bFiles) > 0 and len(aatsrL1bFiles) > 0:
                    collocDir = gaRootDir + '/COLLOC/' + '/' + year + '/' + month + '/' + str(day).zfill(2)
                    print 'merisL1bDir: ', merisL1bDir 
                    print 'aatsrL1bDir: ', aatsrL1bDir 
                    print 'collocDir: ', collocDir 
                    for indexA in range(0, len(aatsrL1bFiles)):
                        if aatsrL1bFiles[indexA].endswith(".nc"):
                            aatsr_orbit_1 = aatsrL1bFiles[indexA][43:48] # e.g. 00479 split from 00479_16565
                            aatsr_orbit_2 = aatsrL1bFiles[indexA][49:54] # e.g. 16565 split from 00479_16565
                            meris_orbit_1 = str(int(aatsr_orbit_1)+1).zfill(5)   # the corresponding MERIS orbit is incremented by 1 (why?)
                            meris_orbit_2 = str(int(aatsr_orbit_2)+1).zfill(5)
                            meris_orbit = meris_orbit_1 + '_' + meris_orbit_2
                            print 'aatsr_orbit_1: ', aatsr_orbit_1 
                            print 'aatsr_orbit_2: ', aatsr_orbit_2 
                            print 'meris_orbit_1: ', meris_orbit_1 
                            print 'meris_orbit_2: ', meris_orbit_2 
                            print 'meris_orbit: ', meris_orbit 

                            for indexM in range(0, len(merisL1bFiles)):
                                if merisL1bFiles[indexM].endswith(".nc") and meris_orbit in merisL1bFiles[indexM]:
	                            print 'meris,aatsr: ', merisL1bFiles[indexM], aatsrL1bFiles[indexA]
                                    m.execute('ga-l2-meris-aatsr-coreg-step.sh', 
                                              ['dummy'], 
                                              [collocDir], 
                                              parameters=[aatsrL1bDir,aatsrL1bFiles[indexA],merisL1bDir,merisL1bFiles[indexM],year,month,str(day).zfill(2),gaRootDir])

m.wait_for_completion()

