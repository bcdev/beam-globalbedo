import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

# Script to process L1b --> SDR orbits for MERIS and VGT.
# Can also be used as it is for PROBA-V to process Synergy TOA L1b --> BBDR orbits .
# (todo 20160527: provide scripts also for PROBA-V L1b --> SDR)

__author__ = 'olafd'

#sensors = ['MERIS','VGT']
#sensors = ['MERIS']
sensors = ['PROBAV']
#sensors = ['VGT']
#year = sys.argv[1]   # in [1997, 2010]
#years = ['2002']    #test  
#years = ['2002','2003','2004']    #test  
#years = ['2015']    #test  
#years = ['1998','1999','2000','2001','2002']
#years = ['2002','2003','2004']    #test  
#years = ['2006','2007','2008','2009']
#years = ['2006','2007','2012']
#years = ['2008','2009','2010','2011','2012','2013','2014']  
years = ['2013','2014','2015','2016']  
#allMonths = ['01', '02', '03']
#allMonths = ['04', '05']
#allMonths = ['04', '05', '06']
#allMonths = ['10', '11', '12']
#allMonths = ['04', '05', '06', '07', '08', '09', '10', '11', '12']
#allMonths = ['07', '08', '09', '10', '11', '12']
allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
#allMonthsVGT = ['10', '11', '12']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['1', '2', '3']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['4', '5', '6']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['7', '8', '9']  # data at CEMS from Vito is stored like this
allMonthsVGT = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']  # data at CEMS from Vito is stored like this
#allMonths = ['05']
#allMonthsVGT = ['5']

#################################################################
def getMonthVGT(year):
    return allMonthsVGT

def getMonthPROBAV(year):
    if year == '2013':
        return ['10', '11', '12']
    if year == '2016':
        return ['1', '2', '3', '4', '5', '6']
    return allMonthsVGT

def getMonthMERIS(year):
    if year == '2002':
        #return ['04', '05', '06', '07', '08', '09', '10', '11', '12']
        return ['04', '05']
    if year == '2012':
        return ['01', '02', '03', '04']
    return allMonths

def getMonth(year, sensor):
    if sensor == 'MERIS':
	return getMonthMERIS(year)
    if sensor == 'PROBAV':
        return getMonthPROBAV(year)
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

######################## L1b --> BBDR: ###########################

#gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
gaSdrRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
merisL1bRootDir = gaRootDir + '/MERIS'
vgtL1bRootDir = gaRootDir + '/VGT'

#beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-sdr-orbits',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-sdr-orbits-step.sh',192)])

for year in years:
    for sensor in sensors:
        if sensor == 'MERIS':
            for month in getMonth(year, sensor):
                sdrOrbitsDir = gaSdrRootDir + '/SDR_orbits/' + sensor + '/' + year + '/' + month 
                l1bDir = gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month
                if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month):
                    for day in range(1, getNumMonthDays(year, int(month))+1):
                        if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                            l1bFiles = os.listdir(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2))
                            if len(l1bFiles) > 0:
                                for index in range(0, len(l1bFiles)):
                                    if l1bFiles[index].endswith(".N1") or l1bFiles[index].endswith(".N1.gz"):
		                        l1bPath = gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + l1bFiles[index]
                                        m.execute('ga-l2-sdr-orbits-step.sh', ['dummy'], [sdrOrbitsDir], parameters=[l1bPath,l1bFiles[index],sdrOrbitsDir,year,month,sensor,gaRootDir,beamDir])
        if sensor == 'VGT':
            for month in getMonthVGT(year):
                sdrOrbitsDir = gaSdrRootDir + '/SDR_orbits/' + sensor + '/' + year + '/' + month

                if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month):
                    for day in range(1, getNumMonthDays(year, int(month))+1):
                        if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day)):
                            l1bFiles = os.listdir(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day))
                            if len(l1bFiles) > 0:
                                # we do not want other files than *.ZIP for VGT:
                                for index in range(0,len(l1bFiles)):
                                    if l1bFiles[index].endswith(".ZIP"):
                                        l1bPath = gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day) + '/' + l1bFiles[index]
                                        #print('executing: ', l1bPath)
                                        m.execute('ga-l2-sdr-orbits-step.sh', ['dummy'], [sdrOrbitsDir], parameters=[l1bPath,l1bFiles[index],sdrOrbitsDir,year,month,sensor,gaRootDir,beamDir])

        if sensor == 'PROBAV':
            for month in getMonth(year, sensor):
                # for PROBA-V, we already write BBDR for the moment (todo: clean up this configuration!)
                sdrOrbitsDir = gaSdrRootDir + '/BBDR_orbits/' + sensor + '/' + year + '/' + month
                l1bDir = gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month
                if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month):
                    #for day in range(1, getNumMonthDays(year, int(month))+1):
                    for day in range(1, 10): # temporal, reprocess missing days, remove later!
                        if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day)):
                            l1bFiles = os.listdir(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day))
                            if len(l1bFiles) > 0:
                                for index in range(0, len(l1bFiles)):
                                    if l1bFiles[index].endswith(".HDF5"):
                                        l1bPath = gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day) + '/' + l1bFiles[index]
                                        m.execute('ga-l2-sdr-orbits-step.sh', ['dummy'], [sdrOrbitsDir], parameters=[l1bPath,l1bFiles[index],sdrOrbitsDir,year,month,sensor,gaRootDir,beamDir])


m.wait_for_completion()

