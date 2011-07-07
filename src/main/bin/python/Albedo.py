#!/usr/local/epd-6.3-2-rh5-x86_64/bin/python

"""
GlobAlbedo Albedo products based on GlobAlbedo BRDF model parameters

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

def GetAlbedo(F, C, Mask, SZA, columns, rows):
    '''
    Get Albedo based on model:
        F = BRDF model parameters (1x9 vector)
        C = Variance/Covariance matrix (9x9 matrix)
        U = Polynomial coefficients

        Black-sky Albedo = f0 + f1 * (-0.007574 + (-0.070887 * SZA^2) + (0.307588 * SZA^3)) + f2 * (-1.284909 + (-0.166314 * SZA^2) + (0.041840 * SZA^3)) 

        White-sky Albedo = f0 + f1 * (0.189184) + f2 * (-1.377622) 

        Uncertainties = U^T C^-1 U , U is stored as a 1X9 vector (transpose), so, actually U^T is the regulat 9x1 vector
    '''

    from numpy import deg2rad
    SZA = deg2rad(SZA)

    Invalid = -9999
    nWaveBands = 3

    BSA_sigma = numpy.zeros((nWaveBands, columns, rows), numpy.float32)
    WSA_sigma = numpy.zeros((nWaveBands, columns, rows), numpy.float32)

    U_WSA_VIS = numpy.array([1.0, 0.189184, -1.377622, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0])
    U_WSA_NIR = numpy.array([0.0, 0.0, 0.0, 1.0, 0.189184, -1.377622, 0.0, 0.0, 0.0,])
    U_WSA_SW =  numpy.array([0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.189184, -1.377622])

    C_inv = numpy.zeros((nWaveBands*nWaveBands,nWaveBands*nWaveBands, columns, rows), numpy.float32)

    # Calculate uncertainties
    for column in range(0,columns):
        for row in range(0,rows):
            if Mask[column,row] > 0.0:
                try:
                    #C_inv[:,:,column,row] = numpy.matrix(C[:,:,column,row]).I
                    # Calculate White-Sky sigma
                    WSA_sigma[0, column,row] = numpy.matrix(U_WSA_VIS) * numpy.matrix(C[:,:,column,row]) * numpy.matrix(U_WSA_VIS).T
                    WSA_sigma[1, column,row] = numpy.matrix(U_WSA_NIR) * numpy.matrix(C[:,:,column,row]) * numpy.matrix(U_WSA_NIR).T
                    WSA_sigma[2, column,row] = numpy.matrix(U_WSA_SW)  * numpy.matrix(C[:,:,column,row]) * numpy.matrix(U_WSA_SW).T

                    # Calculate Black-Sky sigma
                    U_BSA = numpy.array([ (1.0 + 0.0 + 0.0),
                                           (-0.007574 + (-0.070887 * numpy.power(SZA[column,row],2)) + (0.307588 * numpy.power(SZA[column,row],3))),
                                           (-1.284909 + (-0.166314 * numpy.power(SZA[column,row],2)) + (0.041840 * numpy.power(SZA[column,row],3))) ] )

                    U_BSA_VIS = numpy.concatenate((U_BSA, [0.0,0.0,0.0,0.0,0.0,0.0]))
                    U_BSA_NIR = numpy.concatenate(([0.0,0.0,0.0], U_BSA, [0.0,0.0,0.0]))
                    U_BSA_SW = numpy.concatenate(([0.0,0.0,0.0,0.0,0.0,0.0], U_BSA))

                    BSA_sigma[0, column,row] = numpy.matrix(U_BSA_VIS) * numpy.matrix(C[:,:,column,row]) * numpy.matrix(U_BSA_VIS).T
                    BSA_sigma[1, column,row] = numpy.matrix(U_BSA_NIR) * numpy.matrix(C[:,:,column,row]) * numpy.matrix(U_BSA_NIR).T
                    BSA_sigma[2, column,row] = numpy.matrix(U_BSA_SW)  * numpy.matrix(C[:,:,column,row]) * numpy.matrix(U_BSA_SW).T

                except numpy.linalg.LinAlgError:
                    continue

    # Calculate Black-Sky Albedo
    BSA_VIS = F[0,:,:] + \
              F[1,:,:] * (-0.007574 + (-0.070887 * numpy.power(SZA,2)) + (0.307588 * numpy.power(SZA,3))) + \
              F[2,:,:] * (-1.284909 + (-0.166314 * numpy.power(SZA,2)) + (0.041840 * numpy.power(SZA,3)))

    BSA_NIR = F[3,:,:] + \
              F[4,:,:] * (-0.007574 + (-0.070887 * numpy.power(SZA,2)) + (0.307588 * numpy.power(SZA,3))) + \
              F[5,:,:] * (-1.284909 + (-0.166314 * numpy.power(SZA,2)) + (0.041840 * numpy.power(SZA,3)))

    BSA_SW  = F[6,:,:] + \
              F[7,:,:] * (-0.007574 + (-0.070887 * numpy.power(SZA,2)) + (0.307588 * numpy.power(SZA,3))) + \
              F[8,:,:] * (-1.284909 + (-0.166314 * numpy.power(SZA,2)) + (0.041840 * numpy.power(SZA,3)))

    # Calculate White-Sky Albedo
    WSA_VIS = F[0,:,:] + (F[1,:,:] * U_WSA_VIS[1]) + (F[2,:,:] * U_WSA_VIS[2])
    WSA_NIR = F[3,:,:] + (F[4,:,:] * U_WSA_NIR[4]) + (F[5,:,:] * U_WSA_NIR[5])
    WSA_SW =  F[6,:,:] + (F[7,:,:] * U_WSA_SW[7]) + (F[8,:,:] * U_WSA_SW[8])

    BlackSkyAlbedo = numpy.zeros((nWaveBands, columns, rows), numpy.float32)
    BlackSkyAlbedo[0,:,:] = BSA_VIS
    BlackSkyAlbedo[1,:,:] = BSA_NIR
    BlackSkyAlbedo[2,:,:] = BSA_SW

    WhiteSkyAlbedo = numpy.zeros((nWaveBands, columns, rows), numpy.float32)
    WhiteSkyAlbedo[0,:,:] = WSA_VIS
    WhiteSkyAlbedo[1,:,:] = WSA_NIR
    WhiteSkyAlbedo[2,:,:] = WSA_SW

    # Cap uncertainties and calculate sqrt
    BSA_sigma = numpy.where(BSA_sigma >= 1.0, 1.0, BSA_sigma)
    BSA_sigma = numpy.sqrt(numpy.abs(BSA_sigma))
    WSA_sigma = numpy.where(WSA_sigma >= 1.0, 1.0, WSA_sigma)
    WSA_sigma = numpy.sqrt(numpy.abs(WSA_sigma))

    return ReturnGetAlbedo(BlackSkyAlbedo, WhiteSkyAlbedo, BSA_sigma, WSA_sigma)


class ReturnGetAlbedo(object):
    def __init__(self, BlackSkyAlbedo, WhiteSkyAlbedo, BSA_sigma, WSA_sigma):
        self.BlackSkyAlbedo = BlackSkyAlbedo
        self.WhiteSkyAlbedo = WhiteSkyAlbedo
        self.BSA_sigma = BSA_sigma
        self.WSA_sigma = WSA_sigma

def GetSZA(File, xmin=1, ymin=1, xmax=1, ymax=1):
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

    print "Opening SZA", filename[0]

    SZA = numpy.zeros((xsize, ysize), numpy.float32)

    SZA[:,:] = dataset.GetRasterBand(3).ReadAsArray(Xmin, Ymin, xsize, ysize)

    return SZA


def GetInversion(File, xmin=1, ymin=1, xmax=1, ymax=1):
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

    print "Opening GlobAlbedo parameters", filename[0]

    # Load parameters
    nWaveBands = 3
    parameters = numpy.zeros((nWaveBands*nWaveBands, xsize, ysize), numpy.float32)

    for i in range(0,nWaveBands*3):
        parameters[i,:,:] = dataset.GetRasterBand(i+1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Load covariance matrix C, 9x9
    # First 9 bands are BRDF parameters
    NumberOfCovarianceBands = 45
    n = nWaveBands*nWaveBands
    covariance = numpy.zeros((NumberOfCovarianceBands, xsize, ysize), numpy.float32)
    for i in range(n-1,NumberOfCovarianceBands+n):
        covariance[i-n-1,:,:] = dataset.GetRasterBand(i).ReadAsArray(Xmin, Ymin, xsize, ysize)

    C = numpy.zeros((n, n, xsize, ysize), numpy.float32)
    for k in range(n,0,-1):
        if k == n:
            index1 = n
            index2 = (index1+index1) - 1
        else:
            index1 = index2 + 1
            index2 = index2 + k

        # Populate C
        C[n-k,n-k:n,:,:] = covariance[index1-n:index2-n+1,:,:]
        C[n-k:n,n-k,:,:] = covariance[index1-n:index2-n+1,:,:]

    # Use the Entropy as the data mask - band 55
    Entropy = numpy.zeros((xsize, ysize), numpy.float32)
    Entropy[:,:] = dataset.GetRasterBand(55).ReadAsArray(Xmin, Ymin, xsize, ysize)
    Mask = numpy.where(Entropy <> 0.0 , 1.0 , 0.0)

    # Relative entropy - band 56
    RelativeEntropy = numpy.zeros((xsize, ysize), numpy.float32)
    RelativeEntropy[:,:] = dataset.GetRasterBand(56).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # The simplified version of the Relative Entropy = H
    N = 9.0
    H = numpy.exp( (RelativeEntropy / N) )

    # Mask based on NSamples - band 57
    NSamples = numpy.zeros((xsize, ysize), numpy.float32)
    NSamples[:,:] = dataset.GetRasterBand(57).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Goodness of Fit - band 59
    GoodnessOfFit = numpy.zeros((xsize, ysize), numpy.float32)
    GoodnessOfFit[:,:] = dataset.GetRasterBand(59).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Snow Mask - band 60
    SnowMask = numpy.zeros((xsize, ysize), numpy.float32)
    SnowMask[:,:] = dataset.GetRasterBand(60).ReadAsArray(Xmin, Ymin, xsize, ysize)

    return parameters, C, Mask, H, GoodnessOfFit, SnowMask, NSamples


def WriteDataset(File, Albedo, SZA, Mask, H, GoodnessOfFit, SnowMask, NSamples, Tile):
    format = "ENVI"
    driver = gdal.GetDriverByName(format)

    columns = Albedo.BlackSkyAlbedo.shape[1]
    rows = Albedo.BlackSkyAlbedo.shape[2]

    new_dataset = driver.Create( File, columns, rows, 18, GDT_Float32 )

    # Write parameters
    for band in range(0, 3):
        new_dataset.GetRasterBand(band+1).WriteArray(Albedo.BlackSkyAlbedo[band,:,:])
        new_dataset.GetRasterBand(band+4).WriteArray(Albedo.WhiteSkyAlbedo[band,:,:])
        new_dataset.GetRasterBand(band+7).WriteArray(Albedo.BSA_sigma[band,:,:])
        new_dataset.GetRasterBand(band+10).WriteArray(Albedo.WSA_sigma[band,:,:])

    new_dataset.GetRasterBand(13).WriteArray(NSamples[:,:])
    new_dataset.GetRasterBand(14).WriteArray(H[:,:])
    new_dataset.GetRasterBand(15).WriteArray(GoodnessOfFit[:,:])
    new_dataset.GetRasterBand(16).WriteArray(SnowMask[:,:])
    new_dataset.GetRasterBand(17).WriteArray(Mask[:,:])
    new_dataset.GetRasterBand(18).WriteArray(SZA[:,:])

    new_dataset = None

    # Write header
    ULC_coordinates = GetUpperLeftCoordinates(Tile)
    output_header = open(File.split(".bin")[0] + ".hdr", 'w')
    header_template = open("/disk/home/lsg/GlobAlbedo/src/metadata/generic_header_Albedo.hdr")
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

#===================================================================================

from IPython.Shell import IPShellEmbed
ipshell = IPShellEmbed([''], banner = 'Dropping into IPython', exit_msg = 'Leaving Interpreter, back to program.')

import sys

Tile = sys.argv[1]
ParametersFile = sys.argv[2]
#ParametersFile = "/home/lsg/GlobAlbedo/Inversion/h12v04/GlobAlbedo.2005001.h12v04.prior_1.bin"
SZA_file = sys.argv[3]
#SZA_file = "/home/lsg/GlobAlbedo/Inversion/h12v04/SZA/h12v04.001.SZA_LocalNoon.bin"

# Get Solar Zenith Angle at Solar Noon Local Time
SZA = GetSZA(SZA_file)
Parameters, C, Mask, H, GoodnessOfFit, SnowMask, NSamples = GetInversion(ParametersFile)

rows = SZA.shape[0]
cols = SZA.shape[1]

#Use RelativeEntropy H as a mask
Albedo = GetAlbedo(Parameters, C, H, SZA, cols, rows)

OUTDIR = os.getcwd()

YearDoY = os.path.basename(ParametersFile).split('.')[2]
OutputFilename = 'GlobAlbedo.' + YearDoY + '.' + Tile + '.bin'
WriteDataset(OUTDIR + "/" + OutputFilename, Albedo, SZA,  Mask, H, GoodnessOfFit, SnowMask, NSamples, Tile)

