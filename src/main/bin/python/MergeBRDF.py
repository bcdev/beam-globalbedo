#!/usr/local/epd-6.3-2-rh5-x86_64/bin/python

"""
GlobAlbedo Create a BRDF merge product based on the Snow and NoSnow individual products

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


def GetInversion(File, xmin=1, ymin=1, xmax=1, ymax=1):
    """
    Get GlobAlbedo BRDF inversion data
    """
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

    print "Getting GlobAlbedo inversion...", filename[0]

    Inversion = numpy.zeros((BandCount, xsize, ysize), numpy.float32)

    for i in range(0,BandCount):
        Inversion[i,:,:] = dataset.GetRasterBand(i+1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    return Inversion


def GetMergeProductByProportion(NoSnow, Snow, PriorMask):
    """
    Merge BRDF Snow and NoSnow products using the proportion of NoSnow and Snow samples
    """

    BandCount = NoSnow.shape[0]
    cols = NoSnow.shape[1]
    rows = NoSnow.shape[2]

    # Band 55 = Entropy
    EntropyNoSnow = NoSnow[54,:,:]
    EntropySnow = Snow[54,:,:]

    # Band 57 = Weighted Number of Samples
    NSamplesNoSnow = NoSnow[56,:,:]
    NSamplesSnow = Snow[56,:,:]

    # Band_58 = Days to the closest sample
    NoSnowDaysToClosestSample = NoSnow[57,:,:]
    SnowDaysToClosestSample = Snow[57,:,:]

    # Create merge product array and, add an additional band which will be the SnowMask
    Merge = numpy.zeros((BandCount+1, cols,rows), numpy.float32)

    for column in range(0,cols):
        for row in range(0,rows):
            TotalNSamples = NSamplesNoSnow[column,row] + NSamplesSnow[column,row]
            if TotalNSamples > 0.0:
                if NSamplesNoSnow[column,row] == 0 and PriorMask[column,row] == 3.0:
                    # Inland water bodies
                    Merge[0:BandCount,column,row] = 0.0
                elif PriorMask[column,row] == 0.0 or PriorMask[column,row] == 2.0:
                    # Shoreline
                    Merge[0:BandCount,column,row] = NoSnow[:,column,row]
                else:
                    ProportionNSamplesNoSnow = NSamplesNoSnow[column,row] / TotalNSamples
                    ProportionNSamplesSnow = NSamplesSnow[column,row] / TotalNSamples

                    Merge[0:BandCount,column,row] = (NoSnow[:,column,row] * ProportionNSamplesNoSnow) + (Snow[:,column,row] * ProportionNSamplesSnow)
                    Merge[BandCount,column,row] = ProportionNSamplesSnow

            elif PriorMask[column,row] <> 0.0 and PriorMask[column,row] <> 2.0 \
                 and PriorMask[column,row] <> 3.0 and PriorMask[column,row] <> 5.0 and PriorMask[column,row] <> 15.0 \
                 and EntropySnow[column,row] <> 0.0 and  EntropyNoSnow[column,row] <> 0.0:
                if EntropySnow[column,row] <= EntropyNoSnow[column,row]:
                    Merge[0:BandCount,column,row] = Snow[:,column,row]
                    Merge[BandCount,column,row] = 1.0
                else:
                    Merge[0:BandCount,column,row] = NoSnow[:,column,row]

            elif PriorMask[column,row] <> 0.0 and PriorMask[column,row] <> 2.0 \
                 and PriorMask[column,row] <> 3.0 and PriorMask[column,row] <> 5.0 and PriorMask[column,row] <> 15.0 \
                 and EntropySnow[column,row] <> 0.0:
                    Merge[0:BandCount,column,row] = Snow[:,column,row]
                    Merge[BandCount,column,row] = 1.0

            elif PriorMask[column,row] <> 0.0 and PriorMask[column,row] <> 2.0 \
                 and PriorMask[column,row] <> 3.0 and PriorMask[column,row] <> 5.0 and PriorMask[column,row] <> 15.0 \
                  and  EntropyNoSnow[column,row] <> 0.0:
                    Merge[0:BandCount,column,row] = NoSnow[:,column,row]

            else:
                Merge[0:BandCount,column,row] = 0.0

    return Merge


def GetMergeProduct(NoSnow, Snow):
    """
    Merge BRDF Snow and NoSnow products using the number of days to the closest sample as criteria
    """

    BandCount = NoSnow.shape[0]
    cols = NoSnow.shape[1]
    rows = NoSnow.shape[2]

    # Band_58=Days to the closest sample
    NoSnowDaysToClosestSample = NoSnow[57,:,:]
    SnowDaysToClosestSample = Snow[57,:,:]

    # Create merge product array and, add an additional band which will be the SnowMask
    Merge = numpy.zeros((BandCount+1, cols,rows), numpy.float32)

    for column in range(0,cols):
        for row in range(0,rows):
            if NoSnowDaysToClosestSample[column,row] <> 0.0 and SnowDaysToClosestSample[column,row] <> 0.0:

                if NoSnowDaysToClosestSample[column,row] <= SnowDaysToClosestSample[column,row]:
                    Merge[0:BandCount,column,row] = NoSnow[:,column,row]
                elif NoSnowDaysToClosestSample[column,row] > SnowDaysToClosestSample[column,row] and Snow[0,column,row] <> 0.0:
                    Merge[0:BandCount,column,row] = Snow[:,column,row]
                    Merge[BandCount,column,row] = 1.0
                else:
                    Merge[0:BandCount,column,row] = NoSnow[:,column,row]

            elif NoSnowDaysToClosestSample[column,row] <> 0.0 and SnowDaysToClosestSample[column,row] == 0.0:
                Merge[0:BandCount,column,row] = NoSnow[:,column,row]

            elif NoSnowDaysToClosestSample[column,row] == 0.0 and SnowDaysToClosestSample[column,row] <> 0.0:
                Merge[0:BandCount,column,row] = Snow[:,column,row]
                Merge[BandCount,column,row] = 1.0

    return Merge


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


def WriteDataset(File, MergeProduct, Tile, ProductIsMerged=1):
    format = "ENVI"
    driver = gdal.GetDriverByName(format)

    BandCount = MergeProduct.shape[0]
    if ProductIsMerged == 0:
        BandCount += 1

    cols = MergeProduct.shape[1]
    rows = MergeProduct.shape[2]

    new_dataset = driver.Create( File, cols, rows, BandCount, GDT_Float32 )

    # Write data
    for band in range(0, BandCount-1):
        new_dataset.GetRasterBand(band+1).WriteArray(MergeProduct[band,:,:])

    if ProductIsMerged == 0:
        # If the product is not merged just SnowMask == 0
        new_dataset.GetRasterBand(BandCount).WriteArray(numpy.zeros((cols,cols), numpy.float32))
    else:
        new_dataset.GetRasterBand(BandCount).WriteArray(MergeProduct[BandCount-1,:,:])

    new_dataset = None

    # Write header
    ULC_coordinates = GetUpperLeftCoordinates(Tile)
    output_header = open(File.split(".bin")[0] + ".hdr", 'w')
    header_template = open("/disk/home/lsg/GlobAlbedo/src/metadata/generic_header_merge.hdr")
    for lines in header_template:
        if 'SAMPLES,LINES' in lines:
            output_header.write(lines.replace('SAMPLES,LINES', 'samples = ' + str(cols) + '\n' + 'lines = ' + str(rows)))
        elif 'X,Y' in lines:
            # These coordinates are only valid if processing the whole tile
            output_header.write(lines.replace('X,Y', ULC_coordinates))
        else:
            output_header.write(lines)

    output_header.close()
    header_template.close()

def GetPriorMask(PriorMaskFile, xmin=1, ymin=1, xmax=1, ymax=1):
    import sys
    import glob

    try:
        filename = glob.glob(PriorMaskFile)
        dataset = gdal.Open( filename[0], GA_ReadOnly )
    except:
        PriorMask = numpy.zeros((xsize, ysize), numpy.float32)
        return PriorMask


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

    print "Getting GlobAlbedo Prior Mask...", PriorMaskFile
    PriorMask = numpy.zeros((xsize, ysize), numpy.float32)
    # Mask band 20
    PriorMask[:,:] = dataset.GetRasterBand(20).ReadAsArray(Xmin, Ymin, xsize, ysize)

    return PriorMask


#=====================================================================================
from IPython.Shell import IPShellEmbed
ipshell = IPShellEmbed([''], banner = 'Dropping into IPython', exit_msg = 'Leaving Interpreter, back to program.')

import sys
import glob
import os

NoSnowFile = sys.argv[1]
SnowFile = sys.argv[2]
PriorMaskFile = sys.argv[3]
PriorMask = GetPriorMask(PriorMaskFile)

YearDoY = os.path.basename(NoSnowFile).split('.')[1]
Tile = os.path.basename(NoSnowFile).split('.')[2]

# If both files exist apply the merge algorithm
if os.path.isfile(NoSnowFile) and os.path.isfile(SnowFile):
    NoSnow = GetInversion(NoSnowFile)
    Snow = GetInversion(SnowFile)

    print "Merging products..."
    Merge = GetMergeProductByProportion(NoSnow, Snow, PriorMask)

    print "Saving merged results to ENVI file..."
    OUTDIR = os.getcwd()
    OutputFilename = OUTDIR + '/GlobAlbedo.Merge.' + YearDoY + '.' + Tile + '.bin'
    WriteDataset(OutputFilename, Merge, Tile)

elif os.path.isfile(NoSnowFile) and os.path.isfile(SnowFile) == False:
    NoSnow = GetInversion(NoSnowFile)
    print "Snow file doesn't exist, saving NoSnow as merged result to ENVI file..."
    OUTDIR = os.getcwd()
    OutputFilename = OUTDIR + '/GlobAlbedo.Merge.' + YearDoY + '.' + Tile + '.bin'
    ProductIsMerged = 0
    WriteDataset(OutputFilename, NoSnow, Tile, ProductIsMerged)

elif os.path.isfile(NoSnowFile) == False and os.path.isfile(SnowFile):
    Snow = GetInversion(SnowFile)
    print "NoSnow file doesn't exist, saving Snow as merged result to ENVI file..."
    OUTDIR = os.getcwd()
    OutputFilename = OUTDIR + '/GlobAlbedo.Merge.' + YearDoY + '.' + Tile + '.bin'
    ProductIsMerged = 0
    WriteDataset(OutputFilename, Snow, Tile, ProductIsMerged)

else:
    print "Neither Snow nor NoSnow files exist."


