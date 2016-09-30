import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

# Script to process L1b --> BBDR orbits for MERIS and VGT.
# Step from BBDR orbits --> BBDR tiles is ferformed automatically afterwards.

__author__ = 'olafd'

#sensors = ['MERIS','VGT']
#sensors = ['MERIS']
sensors = ['AATSR']
#sensors = ['VGT']
#year = sys.argv[1]   # in [1997, 2010]
#years = ['2006']    #test  
years = ['2005']    #test  
#years = ['2004','2005']    #test  
#allMonths = ['01', '02', '03']
#allMonths = ['04', '05', '06']
#allMonths = ['10', '11', '12']
#allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
#allMonthsVGT = ['10', '11', '12']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['1', '2', '3']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['4', '5', '6']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']  # data at CEMS from Vito is stored like this
allMonths = ['04']
allMonthsVGT = ['4']

#################################################################
def getMonthVGT(year):
    return allMonthsVGT

def getMonth(year):
    if year == '2002':
        return ['04','05', '06', '07', '08', '09', '10', '11', '12']
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

######################## L1b --> BBDR: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'
l1bRootDir = gaRootDir + '/L1b/'

###
#l1bRootDir = gaRootDir + '/AATSR_COREG/'  # TEST! remove later!!
###

#beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#print 'inputs: ', inputs
inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-bbdr',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-bbdr-step.sh',192)])

for year in years:
    for sensor in sensors:
        if sensor == 'MERIS' or sensor == 'AATSR':
            for month in getMonth(year):
                bbdrL2Dir = gaRootDir + '/BBDR_orbits/' + sensor + '/' + year + '/' + month 
                bbdrTileDir = gaRootDir + '/BBDR/' + sensor + '/' + year
                if os.path.exists(l1bRootDir + sensor + '/' + year + '/' + month):
                    print 'hier 1'
                    for day in range(1, getNumMonthDays(year, int(month))+1):
                        l1bDir = l1bRootDir + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)
                        if os.path.exists(l1bDir):
                            print 'l1bDir: ', l1bDir
                            l1bFiles = os.listdir(l1bRootDir + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2))
                            if len(l1bFiles) > 0:
                                for index in range(0, len(l1bFiles)):
                                    print 'l1bFiles[index]: ', l1bFiles[index]
                                    if l1bFiles[index].endswith(".N1") or l1bFiles[index].endswith(".N1.gz") or l1bFiles[index].endswith(".nc") or l1bFiles[index].endswith(".nc.gz"):
      	                                l1bPath = l1bRootDir + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + l1bFiles[index]
                                        print 'index, l1bPath', index, ', ', l1bPath
                                        if sensor == 'MERIS':
                                            m.execute('ga-l2-bbdr-step.sh', ['dummy'], [bbdrL2Dir], parameters=[l1bPath,l1bFiles[index],bbdrL2Dir,bbdrTileDir,year,month,sensor,gaRootDir,beamDir])
                                        if sensor == 'AATSR':        
                                            m.execute('ga-l2-bbdr-step.sh', ['dummy'], [bbdrL2Dir], parameters=[l1bPath,l1bFiles[index],bbdrL2Dir,bbdrTileDir,year,month,'AATSR_NADIR',gaRootDir,beamDir])
                                            m.execute('ga-l2-bbdr-step.sh', ['dummy'], [bbdrL2Dir], parameters=[l1bPath,l1bFiles[index],bbdrL2Dir,bbdrTileDir,year,month,'AATSR_FWARD',gaRootDir,beamDir])

        if sensor == 'VGT':
            for month in getMonthVGT(year):
                bbdrL2Dir = gaRootDir + '/BBDR_orbits/' + sensor + '/' + year + '/' + month
                bbdrTileDir = gaRootDir + '/BBDR/' + sensor + '/' + year

                if os.path.exists(l1bRootDir + sensor + '/' + year + '/' + month):
                    for day in range(1, getNumMonthDays(year, int(month))+1):
                        if os.path.exists(l1bRootDir + sensor + '/' + year + '/' + month + '/' + str(day)):
                            l1bFiles = os.listdir(l1bRootDir + sensor + '/' + year + '/' + month + '/' + str(day))
                            if len(l1bFiles) > 0:
                                # we do not want other files than *.ZIP for VGT:
                                for index in range(0,len(l1bFiles)):
                                    if l1bFiles[index].endswith(".ZIP"):
                                        l1bPath = l1bRootDir + sensor + '/' + year + '/' + month + '/' + str(day) + '/' + l1bFiles[index]
                                        print 'index, l1bPath', index, ', ', l1bPath
                                        #print('executing: ', l1bPath)
                                        m.execute('ga-l2-bbdr-step.sh', ['dummy'], [bbdrL2Dir], parameters=[l1bPath,l1bFiles[index],bbdrL2Dir,bbdrTileDir,year,month,sensor,gaRootDir,beamDir])

m.wait_for_completion()

