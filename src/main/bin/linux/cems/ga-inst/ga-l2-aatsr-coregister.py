import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

year = '2005'
month = '04'

aatsrFiles = [
'ATS_TOA_1PUUPA20050407_031613_000065272036_00132_16218_9714.N1',
'ATS_TOA_1PUUPA20050408_042512_000065272036_00147_16233_9729.N1',
'ATS_TOA_1PUUPA20050411_143434_000065272036_00196_16282_9778.N1',
'ATS_TOA_1PUUPA20050411_161510_000065272036_00197_16283_9779.N1',
'ATS_TOA_1PUUPA20050412_035921_000065272036_00204_16290_9786.N1',
'ATS_TOA_1PUUPA20050412_154333_000065272036_00211_16297_9793.N1',
'ATS_TOA_1PUUPA20050413_032744_000065272036_00218_16304_9800.N1',
'ATS_TOA_1PUUPA20050413_151156_000065272036_00225_16311_9807.N1',
'ATS_TOA_1PUUPA20050414_162055_000065272036_00240_16326_9823.N1',
'ATS_TOA_1PUUPA20050415_040507_000065272036_00247_16333_9830.N1',
'ATS_TOA_1PUUPA20050415_154918_000065272036_00254_16340_9837.N1',
'ATS_TOA_1PUUPA20050416_033330_000065272036_00261_16347_9844.N1',
'ATS_TOA_1PUUPA20050416_151741_000065272036_00268_16354_9851.N1',
'ATS_TOA_1PUUPA20050417_162641_000065272036_00283_16369_9866.N1',
'ATS_TOA_1PUUPA20050418_041052_000065272036_00290_16376_9873.N1',
'ATS_TOA_1PUUPA20050418_141428_000065272036_00296_16382_9879.N1',
'ATS_TOA_1PUUPA20050418_155504_000065272036_00297_16383_9880.N1',
'ATS_TOA_1PUUPA20050419_033915_000065272036_00304_16390_9887.N1',
'ATS_TOA_1PUUPA20050419_152327_000065272036_00311_16397_9894.N1',
'ATS_TOA_1PUUPA20050420_030739_000065272036_00318_16404_0000.N1',
'ATS_TOA_1PUUPA20050421_041638_000065272036_00333_16419_0015.N1',
'ATS_TOA_1PUUPA20050421_142013_000065272036_00339_16425_0021.N1',
'ATS_TOA_1PUUPA20050421_160049_000065272036_00340_16426_0022.N1',
'ATS_TOA_1PUUPA20050422_034501_000065272036_00347_16433_0029.N1',
'ATS_TOA_1PUUPA20050422_152912_000065272036_00354_16440_0036.N1'
]

merisFiles = [
'MER_RR__1PRACR20050407_034329_000026262036_00133_16219_0000.N1.gz',
'MER_RR__1PRACR20050408_045221_000026262036_00148_16234_0000.N1.gz',
'MER_RR__1PRACR20050411_150119_000026282036_00197_16283_0000.N1.gz',
'MER_RR__1PRACR20050411_164154_000026282036_00198_16284_0000.N1.gz',
'MER_RR__1PRACR20050412_042603_000026282036_00205_16291_0000.N1.gz',
'MER_RR__1PRACR20050412_161011_000026292036_00212_16298_0000.N1.gz',
'MER_RR__1PRACR20050413_035419_000026292036_00219_16305_0000.N1.gz',
'MER_RR__1PRACR20050413_153828_000026292036_00226_16312_0000.N1.gz',
'MER_RR__1PRACR20050414_164720_000026302036_00241_16327_0000.N1.gz',
'MER_RR__1PRACR20050415_043128_000026302036_00248_16334_0000.N1.gz',
'MER_RR__1PRACR20050415_161537_000026302036_00255_16341_0000.N1.gz',
'MER_RR__1PRACR20050416_035945_000026312036_00262_16348_0000.N1.gz',
'MER_RR__1PRACR20050416_154354_000026312036_00269_16355_0000.N1.gz',
'MER_RR__1PRACR20050417_165246_000026312036_00284_16370_0000.N1.gz',
'MER_RR__1PRACR20050418_043655_000026312036_00291_16377_0000.N1.gz',
'MER_RR__1PRACR20050418_144028_000026322036_00297_16383_0000.N1.gz',
'MER_RR__1PRACR20050418_162103_000026322036_00298_16384_0000.N1.gz',
'MER_RR__1PRACR20050419_040512_000026322036_00305_16391_0000.N1.gz',
'MER_RR__1PRACR20050419_154920_000026322036_00312_16398_0000.N1.gz',
'MER_RR__1PRACR20050420_033329_000026332036_00319_16405_0000.N1.gz',
'MER_RR__1PRACR20050421_044221_000026332036_00334_16420_0000.N1.gz',
'MER_RR__1PRACR20050421_144554_000026332036_00340_16426_0000.N1.gz',
'MER_RR__1PRACR20050421_162630_000026332036_00341_16427_0000.N1.gz',
'MER_RR__1PRACR20050422_041038_000026342036_00348_16434_0000.N1.gz',
'MER_RR__1PRACR20050422_155447_000026342036_00355_16441_0000.N1.gz'
]

productDays = ['07','08', '11', '11', '12', '12', '13', '13', '14', 
'15', '15', '16', '16', '17', '18', '18', '18', '19', '19', '20', '21','21', '21', '22', '22']

#################################################################

######################## MERIS-AATSR Coreg: ###########################

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='ga-l2-aatsr-coregister',
             logdir='log', 
             hosts=[('localhost',192)],
             types=[('ga-l2-aatsr-coregister-step.sh',192)])

coregDir = gaRootDir + '/' + 'AATSR_COREG'
for productIndex in range(len(aatsrFiles)):
    aatsrProduct = gaRootDir + '/L1b/AATSR/' + year + '/' + month + '/' + productDays[productIndex] + '/' + aatsrFiles[productIndex]
    merisProduct = gaRootDir + '/L1b/MERIS/' + year + '/' + month + '/' + productDays[productIndex] + '/' + merisFiles[productIndex]
    
    print 'meris,aatsr: ', merisProduct, aatsrProduct
    m.execute('ga-l2-aatsr-coregister-step.sh', 
    ['dummy'], 
    [coregDir], 
    parameters=[aatsrProduct,aatsrFiles[productIndex],merisProduct,merisFiles[productIndex],gaRootDir])

m.wait_for_completion()
