import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#sensors = ['MERIS','VGT']
sensors = ['PROBAV']
years = ['2015']    #test  
#years = ['2013', '2014', '2015','2016']    #test  
#years = ['2001']    #test  
#years = ['1998','1999','2000','2001','2002']  # 20160423

allMonthsMeris = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
allMonthsVGT = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']  # data at CEMS from Vito is stored like this
#allMonthsMeris = ['01']
#allMonthsVGT = ['1']

#################################################################
def getMonth(year):
    if sensor == 'MERIS':
        return getMonthMeris(year)
    if sensor == 'VGT':
        return getMonthVgt(year)
    if sensor == 'PROBAV':
        return getMonthProbav(year)
    return []

def getMonthVgt(year):
    if year == '2002':
        return ['6', '7', '8', '9', '10', '11', '12']
    if year == '2012':
        return ['1', '2', '3', '4']
    return allMonthsVGT

def getMonthProbav(year):
    if year == '2013':
        return ['10', '11', '12']
    if year == '2016':
        return ['1', '2', '3', '4', '5', '6']
    return allMonthsVGT

def getMonthMeris(year):
    if year == '2002':
        return ['04', '05', '06', '07', '08', '09', '10', '11', '12']
    if year == '2012':
        return ['01', '02', '03', '04']
    return allMonthsMeris

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

######################## SDR orbits --> tiles: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-sdr-tiles',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-sdr-tiles-step.sh',192)])

for year in years:
    for sensor in sensors:
        if sensor == 'PROBAV':
            sdrTileDir = gaRootDir + '/BBDR/' + sensor + '/' + year
        else:
            sdrTileDir = gaRootDir + '/SDR/' + sensor + '/' + year 
        for month in getMonth(year):
            if sensor == 'PROBAV':
                sdrOrbitDir = gaRootDir + '/BBDR_orbits/' + sensor + '/' + year + '/' + month
            else:
                sdrOrbitDir = gaRootDir + '/SDR_orbits/' + sensor + '/' + year + '/' + month 
            if os.path.exists(sdrOrbitDir):
                sdrFiles = os.listdir(sdrOrbitDir)
                if len(sdrFiles) > 0:
                   for index in range(0, len(sdrFiles)):
                       if sdrFiles[index].endswith(".nc") or sdrFiles[index].endswith(".nc.gz"):
		           sdrOrbitFilePath = sdrOrbitDir + '/' + sdrFiles[index]
                           m.execute('ga-l2-sdr-tiles-step.sh', ['dummy'], [sdrTileDir], parameters=[sdrOrbitFilePath,sdrFiles[index],sdrTileDir,sensor,gaRootDir,beamDir])

m.wait_for_completion()

