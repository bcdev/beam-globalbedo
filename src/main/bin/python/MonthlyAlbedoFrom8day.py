#!/usr/local/epd-6.3-2-rh5-x86_64/bin/python

"""
GlobAlbedo Create monthly weighted BRDF parameters from 8-day products

Authors:   Gerardo Lopez-Saldana <lsg@mssl.ucl.ac.uk>
"""
import os

try:
  import numpy
except ImportError:
  print 'Numpy is not installed.'
  exit(-1)

try:
  import osgeo.gdal as gdal
  from osgeo.gdalconst import *
except ImportError:
  print 'GDAL is not installed.'
  exit(-1)

def GetAlbedo(File, xmin=1, ymin=1, xmax=1, ymax=1):
    import sys
    import glob

    try:
        filename = glob.glob(File)
        dataset = gdal.Open( filename[0], GA_ReadOnly )
    except:
        print "Error:", sys.exc_info()[0]
        exit(-1)

    if xmin == ymin == xmax == ymax == 1:
        Xmin = Ymin = 0
        Xmax, Ymax, BandCount = dataset.RasterXSize, dataset.RasterYSize, dataset.RasterCount
        xsize = (Xmax-Xmin)
        ysize = (Ymax-Ymin)

    else:
        BandCount = dataset.RasterCount
        xsize = (xmax-xmin) + 1
        ysize = (ymax-ymin) + 1
        Xmin = xmin - 1
        Ymin = ymin - 1

    print "Opening GlobAlbedo DHR/BHR", filename[0]

    nWaveBands = 3

    DHR = numpy.zeros((nWaveBands, xsize, ysize), numpy.float32)
    for i in range(0,nWaveBands):
        DHR[i,:,:] = dataset.GetRasterBand(i+1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    BHR = numpy.zeros((nWaveBands, xsize, ysize), numpy.float32)
    for i in range(0,nWaveBands):
        BHR[i,:,:] = dataset.GetRasterBand(i+4).ReadAsArray(Xmin, Ymin, xsize, ysize)

    DHR_SD = numpy.zeros((nWaveBands, xsize, ysize), numpy.float32)
    for i in range(0,nWaveBands):
        DHR_SD[i,:,:] = dataset.GetRasterBand(i+7).ReadAsArray(Xmin, Ymin, xsize, ysize)

    BHR_SD = numpy.zeros((nWaveBands, xsize, ysize), numpy.float32)
    for i in range(0,nWaveBands):
        BHR_SD[i,:,:] = dataset.GetRasterBand(i+10).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Weighted Number of Samples, band 13
    NSamples = numpy.zeros((xsize, ysize), numpy.float32)
    NSamples[:,:] = dataset.GetRasterBand(13).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Relative Entropy, band 14
    RelativeEntropy = numpy.zeros((xsize, ysize), numpy.float32)
    RelativeEntropy[:,:] = dataset.GetRasterBand(14).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Goodness Of Fit, band 15
    GoodnessOfFit = numpy.zeros((xsize, ysize), numpy.float32)
    GoodnessOfFit[:,:] = dataset.GetRasterBand(15).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # SnowFraction, band 16
    SnowFraction = numpy.zeros((xsize, ysize), numpy.float32)
    SnowFraction[:,:] = dataset.GetRasterBand(16).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Data Mask, band 17
    DataMask = numpy.zeros((xsize, ysize), numpy.float32)
    DataMask[:,:] = dataset.GetRasterBand(17).ReadAsArray(Xmin, Ymin, xsize, ysize)

    return DHR, BHR, DHR_SD, BHR_SD, NSamples, RelativeEntropy, GoodnessOfFit, SnowFraction, DataMask

def WriteDataset(File, Monthly_DHR, Monthly_BHR, Monthly_DHR_SD, Monthly_BHR_SD, Monthly_NSamples, Monthly_RelativeEntropy, Monthly_GoodnessOfFit, Monthly_SnowFraction, Monthly_DataMask, Tile):
    format = "ENVI"
    driver = gdal.GetDriverByName(format)

    columns = Monthly_DHR.shape[1]
    rows = Monthly_DHR.shape[2]

    new_dataset = driver.Create( File, columns, rows, 17, GDT_Float32 )

    nWaveBands = 3

    # Write data
    for i in range(0, nWaveBands):
        new_dataset.GetRasterBand(i+1).WriteArray(Monthly_DHR[i,:,:])
        new_dataset.GetRasterBand(i+4).WriteArray(Monthly_BHR[i,:,:])
        new_dataset.GetRasterBand(i+7).WriteArray(Monthly_DHR_SD[i,:,:])
        new_dataset.GetRasterBand(i+10).WriteArray(Monthly_BHR_SD[i,:,:])

    new_dataset.GetRasterBand(13).WriteArray(Monthly_NSamples[:,:])
    new_dataset.GetRasterBand(14).WriteArray(Monthly_RelativeEntropy[:,:])
    new_dataset.GetRasterBand(15).WriteArray(Monthly_GoodnessOfFit[:,:])
    new_dataset.GetRasterBand(16).WriteArray(Monthly_SnowFraction[:,:])
    new_dataset.GetRasterBand(17).WriteArray(Monthly_DataMask[:,:])

    new_dataset = None

    # Write header
    ULC_coordinates = GetUpperLeftCoordinates(Tile)
    output_header = open(File.split(".bin")[0] + ".hdr", 'w')
    header_template = open("/disk/home/lsg/GlobAlbedo/src/metadata/generic_header_MonthlyAlbedo.hdr")
    for lines in header_template:
        if 'SAMPLES,LINES' in lines:
            output_header.write(lines.replace('SAMPLES,LINES', 'samples = ' + str(columns) + '\n' + 'lines = ' + str(rows)))
        elif 'X,Y' in lines:
            # These coordinates are only valid if processing the whole tile
            output_header.write(lines.replace('X,Y', ULC_coordinates))
        else:
            output_header.write(lines)

    output_header.close()
    header_template.close()


def GetUpperLeftCoordinates(Tile):
    '''
    Get the Upper Left Corner coordinates in meters for a given MODIS tile
    Coordinates are stored in an ASCII file
    '''
    ULC_coords_file = '/disk/home/lsg/GlobAlbedo/src/metadata/Tiles_UpperLeftCorner_Coordinates.txt'
    ULC_coordinates = open(ULC_coords_file, 'r')

    coordinates = ''
    for line in ULC_coordinates:
        if Tile in line:
            coordinates = line.split(',')[1].strip() + ',' + line.split(',')[2].strip()

    return coordinates


def GetMonthlyWeighting():

    #46 8-day time periods in a year, as first one starts on Jan 9th
    EightDayTimePeriods = numpy.array(range(1,365,8))
    DaysInYear = numpy.array(range(1,365+1))

    EightDayTimePeriodsExtended = numpy.array(range(1,365+8,8)) # Necessary as the last 8-day period, DoY=361 extends to the following year
    weight = numpy.zeros((len(EightDayTimePeriodsExtended), len(DaysInYear)), numpy.float32)
    halfLife = 11.54
    i = 0
    for DoY in EightDayTimePeriodsExtended:
        DeltaTime = DaysInYear - DoY
        weight[i,:] = numpy.exp(-1.0 * abs(DeltaTime)/halfLife)
        i += 1

    StartingDoY = [1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335]
    NDays = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]

    MonthlyWeighting = numpy.zeros((12, len(DaysInYear)), numpy.float32)

    j = 0
    for StartingDayInMonth in StartingDoY:
        NumberOfDaysInMonth = NDays[j]
        #print StartingDayInMonth, NumberOfDaysInMonth
        for Days in DaysInYear:
            ND = Sum = i = 0
            for DoY in EightDayTimePeriods:
                if DoY >= StartingDayInMonth - 8 and DoY <= StartingDayInMonth + NumberOfDaysInMonth + 8:
                    MonthlyWeight = 1.0

                    if DoY >= StartingDayInMonth + NumberOfDaysInMonth - 8:
                        Distance = (StartingDayInMonth + NumberOfDaysInMonth - DoY) / 8.
                        MonthlyWeight = Distance * 0.5 + 0.5

                    if DoY <= StartingDayInMonth + 8:
                        Distance = (StartingDayInMonth + 8 - DoY) / 8.
                        MonthlyWeight = Distance * 0.5 + 0.5

                    ND += MonthlyWeight
                    # Do not take into account the first weight
                    Sum += weight[(DoY + 8 -1)/8,Days-1] * MonthlyWeight

                i += 1
            MonthlyWeighting[j, Days-1] = Sum/ND
        j += 1


    #import pylab
    #for k in range(0, MonthlyWeighting.shape[0]):
    #    pylab.plot(MonthlyWeighting[k])

    #pylab.show()


    return MonthlyWeighting

#===================================================================================
from IPython.Shell import IPShellEmbed
ipshell = IPShellEmbed([''], banner = 'Dropping into IPython', exit_msg = 'Leaving Interpreter, back to program.')

import sys
import glob
import os

Tile = sys.argv[1]
Month = sys.argv[2]
MonthIndex = int(sys.argv[2]) - 1

AlbedoFiles = glob.glob('/disk/Globalbedo6?/GlobAlbedo/Albedo/' + Tile + '/*.bin')
AlbedoFiles.sort()

nWaveBands = 3
n = nWaveBands*nWaveBands

# Open first file and get dimensions
DoY = int(os.path.basename(AlbedoFiles[0]).split('.')[1][4:7])
DHR, BHR, DHR_SD, BHR_SD, NSamples, RelativeEntropy, GoodnessOfFit, SnowFraction, DataMask = GetAlbedo(AlbedoFiles[0])
cols = DHR.shape[1]
rows = DHR.shape[2]

# Create output arrays
Monthly_DHR = numpy.zeros((nWaveBands, cols, rows), numpy.float32)
Monthly_BHR = numpy.zeros((nWaveBands, cols, rows), numpy.float32)
Monthly_DHR_SD = numpy.zeros((nWaveBands, cols, rows), numpy.float32)
Monthly_BHR_SD = numpy.zeros((nWaveBands, cols, rows), numpy.float32)
Monthly_NSamples = numpy.zeros((cols, rows), numpy.float32)
Monthly_RelativeEntropy = numpy.zeros((cols, rows), numpy.float32)
Monthly_GoodnessOfFit = numpy.zeros((cols, rows), numpy.float32)
Monthly_SnowFraction = numpy.zeros((cols, rows), numpy.float32)
Monthly_DataMask = numpy.zeros((cols, rows), numpy.float32)

MonthlyWeighting = GetMonthlyWeighting()
SumWeights = 0.0

for file in AlbedoFiles:

    if file <> AlbedoFiles[0]:
        # GlobAlbedo.2005001.h12v04.prior_1.bin
        DoY = int(os.path.basename(file).split('.')[1][4:7])
        print file, DoY, MonthlyWeighting[MonthIndex,DoY-1]
        DHR, BHR, DHR_SD, BHR_SD, NSamples, RelativeEntropy, GoodnessOfFit, SnowFraction, DataMask = GetAlbedo(file)

    Monthly_DHR = Monthly_DHR + DHR * MonthlyWeighting[MonthIndex,DoY-1]
    Monthly_BHR = Monthly_BHR + BHR * MonthlyWeighting[MonthIndex,DoY-1]
    Monthly_DHR_SD = Monthly_DHR_SD + DHR_SD * MonthlyWeighting[MonthIndex,DoY-1]
    Monthly_BHR_SD = Monthly_BHR_SD + BHR_SD * MonthlyWeighting[MonthIndex,DoY-1]
    Monthly_NSamples = Monthly_NSamples + NSamples * MonthlyWeighting[MonthIndex,DoY-1]
    Monthly_RelativeEntropy = Monthly_RelativeEntropy + RelativeEntropy * MonthlyWeighting[MonthIndex,DoY-1]
    Monthly_GoodnessOfFit = Monthly_GoodnessOfFit + GoodnessOfFit * MonthlyWeighting[MonthIndex,DoY-1]
    Monthly_SnowFraction = Monthly_SnowFraction + SnowFraction * MonthlyWeighting[MonthIndex,DoY-1]
    Monthly_DataMask = numpy.where(Monthly_DataMask + DataMask > 0.0, 1.0, 0.0)

    SumWeights = SumWeights + MonthlyWeighting[MonthIndex,DoY-1]

Monthly_DHR = Monthly_DHR / SumWeights
Monthly_BHR = Monthly_BHR / SumWeights
Monthly_DHR_SD = Monthly_DHR_SD / SumWeights
Monthly_BHR_SD = Monthly_BHR_SD / SumWeights
Monthly_NSamples = Monthly_NSamples / SumWeights
Monthly_RelativeEntropy = Monthly_RelativeEntropy / SumWeights
Monthly_GoodnessOfFit = Monthly_GoodnessOfFit / SumWeights
Monthly_SnowFraction = Monthly_SnowFraction / SumWeights


OUTDIR = os.getcwd()
OutputFilename = 'GlobAlbedo.2005' + Month + '.' + Tile + '.bin'
WriteDataset(OUTDIR + "/" + OutputFilename, Monthly_DHR, Monthly_BHR, Monthly_DHR_SD, Monthly_BHR_SD, Monthly_NSamples, Monthly_RelativeEntropy, Monthly_GoodnessOfFit, Monthly_SnowFraction, Monthly_DataMask, Tile)

