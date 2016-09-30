import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#sensors = ['MERIS','VGT']
sensors = ['MERIS']
#sensors = ['VGT']
#year = sys.argv[1]   # in [1997, 2010]
#years = ['2006']    #test  
years = ['2005']    #test  
#years = ['2004','2005']    #test  
#allMonths = ['01', '02', '03']
#allMonths = ['04', '05', '06']
#allMonths = ['04', '05', '06']
#allMonths = ['10', '11', '12']
#allMonths = ['04', '05', '06', '07', '08', '09', '10', '11', '12']
#allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
#allMonthsVGT = ['10', '11', '12']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['1', '2', '3']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['4', '5', '6']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['7', '8', '9']  # data at CEMS from Vito is stored like this
#allMonthsVGT = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']  # data at CEMS from Vito is stored like this
allMonths = ['04']
allMonthsVGT = ['4']

#################################################################
def getMonthVGT(year):
    return allMonthsVGT

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

######################## L1b --> BBDR: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest'
merisL1bRootDir = gaRootDir + '/MERIS'
vgtL1bRootDir = gaRootDir + '/VGT'

#beamDir = '/group_workspaces/cems/globalalbedo/soft/beam-5.0.1'
beamDir = '/group_workspaces/cems2/qa4ecv/vol4/software/beam-5.0.1'

#inputs = []
#for year in years:
#    for sensor in sensors:
#        if sensor == 'MERIS':
#            # MERIS L1b files are stored per month...
#            for month in getMonth(year):
#                if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month):
#                    for day in range(1, getNumMonthDays(year, int(month))+1):
#                        if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)):
#                            l1bFiles = os.listdir(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2))
#                            if len(l1bFiles) > 0:
#                                # we do not want other files than *.N1 for MERIS:
#                                for index in range(0, len(l1bFiles)):
#                                    if l1bFiles[index].endswith(".N1") or l1bFiles[index].endswith(".N1.gz"):
#                                        inputs.append(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + l1bFiles[index])
#
#for year in years:
#    for sensor in sensors:
#        if sensor == 'VGT':
#            # VGT L1b files are stored per day...
#            for month in getMonthVGT(year):
#                if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month):
#                    for day in range(1, getNumMonthDays(year, int(month))+1):
#                        if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day)):
#                            l1bFiles = os.listdir(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day))
#                            if len(l1bFiles) > 0:
#                                # we do not want other files than *.ZIP for VGT:
#                                for index in range(0,len(l1bFiles)):
#                                    if l1bFiles[index].endswith(".ZIP"):
#                                        inputs.append(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day) + '/' + l1bFiles[index])

#print 'inputs: ', inputs

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-sdr',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-sdr-step.sh',192)])

for year in years:
    for sensor in sensors:
        if sensor == 'MERIS':
            for month in getMonth(year):
                sdrL2Dir = gaRootDir + '/SDR_L2/' + sensor + '/' + year + '/' + month 
                sdrTileDir = gaRootDir + '/SDR/' + sensor + '/' + year 

                if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month):
                    for day in range(1, getNumMonthDays(year, int(month))+1):
                        #l1bFiles = os.listdir(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month)
                        if os.path.exists(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                            l1bFiles = os.listdir(gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2))
                            if len(l1bFiles) > 0:
                                for index in range(0, len(l1bFiles)):
                                    if l1bFiles[index].endswith(".N1") or l1bFiles[index].endswith(".N1.gz"):
                                        #l1bPath = gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + l1bFiles[index] 
		                        l1bPath = gaRootDir + '/L1b/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + l1bFiles[index]
                                        #print 'index, l1bPath', index, ', ', l1bPath
                                        #m.execute('ga-l2-sdr-step.sh', [l1bPath], [sdrL2Dir], parameters=[l1bPath,l1bFiles[index],sdrL2Dir,sdrTileDir,year,month,sensor,gaRootDir,beamDir])
                                        m.execute('ga-l2-sdr-step.sh', ['dummy'], [sdrL2Dir], parameters=[l1bPath,l1bFiles[index],sdrL2Dir,sdrTileDir,year,month,sensor,gaRootDir,beamDir])
        if sensor == 'VGT':
            for month in getMonthVGT(year):
                sdrL2Dir = gaRootDir + '/SDR_L2/' + sensor + '/' + year + '/' + month
                sdrTileDir = gaRootDir + '/SDR/' + sensor + '/' + year

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
                                        #m.execute('ga-l2-sdr-step.sh', [l1bPath], [sdrL2Dir], parameters=[l1bPath,l1bFiles[index],sdrL2Dir,sdrTileDir,year,month,sensor,gaRootDir,beamDir])
                                        m.execute('ga-l2-sdr-step.sh', ['dummy'], [sdrL2Dir], parameters=[l1bPath,l1bFiles[index],sdrL2Dir,sdrTileDir,year,month,sensor,gaRootDir,beamDir])

m.wait_for_completion()

