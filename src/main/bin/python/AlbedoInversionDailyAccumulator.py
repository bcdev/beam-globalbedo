#!/usr/local/epd-6.3-2-rh5-x86_64/bin/python

"""
GlobAlbedo Broadband albedo inversion

Authors:   Gerardo Lopez-Saldana <lsg@mssl.ucl.ac.uk>
           P. Lewis <plewis@ucl.ac.uk>
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


def GetKernels(Directory, xmin=1, ymin=1, xmax=1, ymax=1):
    '''
    Get Geo/Vol Kernels
    '''

    import sys

    try:
        dataset = gdal.Open( Directory + '/Kvol_BRDF_VIS.img', GA_ReadOnly )
    except:
        print "GetKernels - Error:", sys.exc_info()[0]
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

    # Close dataset
    dataset = None

    nWaveBands = 3
    NumberOfKernels = 3 * nWaveBands

    RossThick = ['Kvol_BRDF_VIS.img','Kvol_BRDF_NIR.img','Kvol_BRDF_SW.img']
    LiSparse = ['Kgeo_BRDF_VIS.img','Kgeo_BRDF_NIR.img','Kgeo_BRDF_SW.img']
    kernels = numpy.zeros((nWaveBands, NumberOfKernels, xsize, ysize), numpy.float32)    # kernels matrix size -> rows x columns = 3 x 9
    # Isotropic kernels = 1
    kernels[0,0,:,:] = kernels[1,3,:,:] = kernels[2,6,:,:] = 1.0

    dataset = gdal.Open( Directory + '/' + RossThick[0], GA_ReadOnly )    # Kvol_BRDF_VIS.img
    kernels[0,1,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    dataset = gdal.Open( Directory + '/' + RossThick[1], GA_ReadOnly )    # Kvol_BRDF_NIR.img
    kernels[1,4,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    dataset = gdal.Open( Directory + '/' + RossThick[2], GA_ReadOnly )    # Kvol_BRDF_SW.img
    kernels[2,7,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    dataset = gdal.Open( Directory + '/' + LiSparse[0], GA_ReadOnly )     # Kgeo_BRDF_VIS.img
    kernels[0,2,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    dataset = gdal.Open( Directory + '/' + LiSparse[1], GA_ReadOnly )     # Kgeo_BRDF_NIR.img
    kernels[1,5,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    dataset = gdal.Open( Directory + '/' + LiSparse[2], GA_ReadOnly )     # Kgeo_BRDF_SW.img
    kernels[2,8,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    return kernels


def GetBBDR(Directory, Snow, Xmin, Ymin, xsize, ysize):
    '''
    AATSR/MERIS/VGT BBDR are in BEAM-DIMAP format with ENVI headers, which means data is in single files.
    '''

    nWaveBands = 3
    WaveBands = ['BB_VIS.img','BB_NIR.img','BB_SW.img']

    # ----------
    # Load BBDRs
    # ----------
    BBDR = numpy.zeros((nWaveBands, 1, xsize, ysize), numpy.float32)   # BBDR matrix size -> rows x columns = 3 x 1
    Mask = numpy.ones((xsize, ysize), numpy.int16)

    print Directory , 'loading BBDR file...'
    for Broadband in range(0,nWaveBands):
        dataset = gdal.Open( Directory + '/' + WaveBands[Broadband], GA_ReadOnly )
        BBDR[Broadband,0,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)
        # Mask BBDR data if 0 or -9999.0
        Mask[:,:] = numpy.where( (BBDR[Broadband,0,:,:]==0) | (BBDR[Broadband,0,:,:]==-9999), 0, 1)
        dataset = None

    indices = numpy.where(Mask <> 0)[0]
    nMask = len(indices)

    #--------------
    # Load SnowMask
    #--------------
    SnowMask = numpy.zeros((xsize, ysize), numpy.float32)
    SnowMaskFilename = 'snow_mask.img'
    dataset = gdal.Open( Directory + '/' + SnowMaskFilename, GA_ReadOnly )
    SnowMask[:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Update mask
    if Snow == 1:
        # Only snow samples
        Mask = numpy.where( SnowMask==1, Mask, 0.0 )
    else:
        # Snow free samples
        Mask = numpy.where( SnowMask==1, 0.0, Mask )

    #----------------------------------------------------------------
    # Load Kernels, Ross-Thick (volumetric) and Li-Sparse (geometric)
    #----------------------------------------------------------------
    kernels = GetKernels(Directory, xmin, ymin, xmax, ymax)

    #----------------------------------------------------
    # Load standard deviation and correlation information
    #----------------------------------------------------
    SD = numpy.zeros((nWaveBands, xsize, ysize), numpy.float32)
    BB_SD = ['sig_BB_VIS_VIS.img','sig_BB_NIR_NIR.img','sig_BB_SW_SW.img']

    correlation = numpy.zeros((nWaveBands, xsize, ysize), numpy.float32)
    BB_correlation = ['sig_BB_VIS_NIR.img','sig_BB_VIS_SW.img','sig_BB_NIR_SW.img']    

    for Broadband in range(0,nWaveBands): 
        dataset = gdal.Open( Directory + '/' + BB_SD[Broadband], GA_ReadOnly )
        SD[Broadband,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

        dataset = gdal.Open( Directory + '/' + BB_correlation[Broadband], GA_ReadOnly )
        correlation[Broadband,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)

        #Some datasets have BBDR information but not uncertainty, masked those pixels
        Mask = numpy.where( SD[Broadband,:,:]==0.0, 0.0, Mask )

    dataset = None

    #-------------------------------------
    # Build C -- coveriance matrix for obs
    #-------------------------------------
    # C in a symetric matrix form of nWaveBands*nWaveBands
    C = numpy.zeros((nWaveBands*nWaveBands, xsize, ysize), numpy.float32)
    Cinv = numpy.zeros((nWaveBands,nWaveBands, xsize, ysize), numpy.float32)
    thisC = numpy.zeros((nWaveBands,nWaveBands,xsize, ysize), numpy.float32)

    if nMask > 0:
        count = ccount = 0
        for j in range(0,nWaveBands):
            for k in range(j+1,nWaveBands):
                if k == j+1: ccount = ccount + 1
                C[ccount,:,:] = correlation[count,:,:] * SD[j,:,:] * SD[k,:,:]
                count = count + 1
                ccount = ccount + 1

        ccount = 0
        for j in range(0,nWaveBands):
            C[ccount,:,:] = SD[j,:,:] * SD[j,:,:]
            ccount = ccount + nWaveBands - j

        # C in a symetric matrix form of nWaveBands*nWaveBands
        count=0
        for j in range(0,nWaveBands):
            for k in range(j,nWaveBands):
                thisC[j,k,:,:] = C[count,:,:]
                thisC[k,j,:,:] = thisC[j,k,:,:]
                count = count + 1

    #Create M and V
    # M = K^T C^-1 K
    # V = K^T C^-1 BBDR
    # E = BBDR^T C^-1 BBDR
    M = numpy.zeros((3*nWaveBands,3*nWaveBands, xsize, ysize), numpy.float32)
    V = numpy.zeros((3*nWaveBands, xsize, ysize), numpy.float32)
    E = numpy.zeros((xsize, ysize), numpy.float32)

    for columns in range(0,xsize):
        for rows in range(0,ysize):
            if Mask[columns,rows] <> 0:
                Cinv[:,:,columns,rows] = numpy.matrix(thisC[:,:,columns,rows]).I

                M[:,:,columns,rows] = numpy.matrix(kernels[:,:,columns,rows]).T * numpy.matrix(Cinv[:,:,columns,rows]) * numpy.matrix(kernels[:,:,columns,rows])
                # Multiply only using lead diagonal of Cinv, additionally transpose the result to store V as a 1 x 9 vector
                V[:,columns,rows] = (numpy.matrix(kernels[:,:,columns,rows]).T * numpy.diagflat(numpy.diagonal(Cinv[:,:,columns,rows])) * BBDR[:,:,columns,rows]).T
                E[columns,rows] = numpy.matrix(BBDR[:,:,columns,rows]).T * numpy.matrix(Cinv[:,:,columns,rows]) * BBDR[:,:,columns,rows]

                #for j1 in range(0,nWaveBands):
                #    for j2 in range(0,nWaveBands):
                #        for k1 in range(0,nWaveBands):
                #            for k2 in range(0,nWaveBands):
                #                M[j1*3+k1,j2*3+k2,columns,rows] = kernels[j1*3+k1,columns,rows] * Cinv[j1,j2,columns,rows] * kernels[j2*3+k2,columns,rows]

                #for j1 in range(0,nWaveBands):
                #    for k1 in range(0,nWaveBands):
                #        V[j1*3+k1,columns,rows] = kernels[j1*3+k1,columns,rows] * Cinv[j1,j1,columns,rows] * BBDR[j1,columns,rows]

    return ReturnValueGetBBDR(BBDR, kernels, SD, correlation, thisC, Cinv, M, V, E, Mask)


class ReturnValueGetBBDR(object):
    def __init__(self, BBDR, kernels, SD, correlation, C, Cinv, M, V, E, mask):
        self.BBDR = BBDR
        self.kernels = kernels
        self.SD = SD
        self.correlation = correlation
        self.C = C
        self.Cinv = Cinv
        self.M = M
        self.V = V
        self.E = E
        self.mask = mask


def GetDaysOfYear(filelist):

    file_list_lenght = len(filelist)

    DoY = numpy.zeros(file_list_lenght, numpy.int16)
    for i in range(0, file_list_lenght):
        # Extract DoY, e.g. 356, from the directory name:
        # /data/geospatial_20/ucasglo/GlobAlbedo/BBDR/VGT/2005/h25v06/2005356_F053/
        DoY[i] = os.path.basename(os.path.dirname(filelist[i]))[4:7]

    return DoY

def GetObsFiles(year, day, tile, location):

    import sys
    import glob

    allFiles = []
    allDoYs = []
    count = 0

    files_path = location + '/' + year + '/' + tile + '/' + year + day + '*/'
    #Get all BBDR directories - the symlink to the .data directories
    filelist = glob.glob(files_path)

    #It is neccesary to sort BBDR files by YYYY/YYYYMMDD
    from operator import itemgetter, attrgetter

    TmpList = []
    for i in range(0,len(filelist)):
        TmpList.append(tuple(filelist[i].split('/'))) # create a list of tupples

    # First, sort by year, In this case year is in field 5
    # e.g. /unsafe/GlobAlbedo/BBDR/MERIS/2006/h12v04/AccumulatorFiles/2006065_142525/
    s = sorted(TmpList, key=itemgetter(5))
    # Now sort the sorted-by-year list by filename
    s = sorted(s, key=itemgetter(7))

    # Tuple to list
    filelist = []
    file = ''
    for i in range(0,len(s)):
        for j in range(1,len(s[0])):
            file = file + '/' + s[i][j]
        filelist.append(file)
        file = ''

    DoYs = GetDaysOfYear(filelist)

    allFiles = filelist
    for i in range(0, len(DoYs)):
        allDoYs.append(DoYs[i])

    if len(allFiles) == 0:
        Err = 1
    else:
        Err, count = 0, len(allFiles)

    return ReturnValueGetObsFiles(Err, count, allFiles, allDoYs)

class ReturnValueGetObsFiles(object):
    def __init__(self, Err, count, allfiles, alldoys):
        self.Err = Err
        self.count = count
        self.allfiles = allfiles
        self.alldoys = numpy.array(alldoys)


def CreateAccumulatorFiles(DataDir, OutputDir, StorageDir, ObsFilesDoY, year, tile, Snow, xmin, ymin, xmax, ymax):

    import sys
    import glob

    try:
        filename = glob.glob(ObsFilesDoY.allfiles[0] + '/BB_VIS.img')
        dataset = gdal.Open( filename[0], GA_ReadOnly )
    except:
        print "CreateAccumulatorFiles - Error:", sys.exc_info()[0]
        exit(-1)

    if xmin == ymin == xmax == ymax == 1:
        Xmin = Ymin = 0
        Xmax, Ymax, BandCount = dataset.RasterXSize, dataset.RasterYSize, dataset.RasterCount
        xsize = (Xmax-Xmin)
        ysize = (Ymax-Ymin)

    else:
        xsize = (xmax-xmin) + 1
        ysize = (ymax-ymin) + 1
        Xmin = xmin - 1
        Ymin = ymin - 1

    dataset = None

    Number_of_Files = len(ObsFilesDoY.allfiles)
    nWaveBands = 3

    NumberOfBBDRs = len(ObsFilesDoY.allfiles)
    i = 0
    while (i < NumberOfBBDRs):              # i is the day index!

        BBDR = ObsFilesDoY.allfiles[i]

        M = numpy.zeros((3*nWaveBands,3*nWaveBands, xsize, ysize), numpy.float32)
        V = numpy.zeros((3*nWaveBands, xsize, ysize), numpy.float32)
        E = numpy.zeros((xsize, ysize), numpy.float32)
        Mask = numpy.zeros((xsize, ysize), numpy.float32)

        #First, verify whether or not accumulator exists for the specific BBDR, e.g. 20051022
        BBDR_name = os.path.basename(os.path.dirname(BBDR))[0:7]
        if os.path.isfile(StorageDir + '/M_' + BBDR_name + '.npy') and \
           os.path.isfile(StorageDir + '/V_' + BBDR_name + '.npy') and \
           os.path.isfile(StorageDir + '/E_' + BBDR_name + '.npy') and \
           os.path.isfile(StorageDir + '/mask_' + BBDR_name + '.npy'):

            #Accumulator exists
            print "M, V, E and mask for", BBDR_name, "accumulator exist..."
            i = i + 1

        else:
            BBDR_DoY = ObsFilesDoY.alldoys[ObsFilesDoY.allfiles.index(BBDR)]
            #Load data from BBDR
            bbdr = GetBBDR(BBDR, Snow, Xmin, Ymin, xsize, ysize)

            M[:,:,:,:] = bbdr.M 
            V[:,:,:] = bbdr.V
            E[:,:] = bbdr.E
            Mask[:,:] = bbdr.mask

            if (i < NumberOfBBDRs - 1):
                # Create the accumulator file for ALL BBDRs from the same day
                while BBDR_DoY == ObsFilesDoY.alldoys[i+1]:
                    BBDR = ObsFilesDoY.allfiles[i+1]
                    bbdr = GetBBDR(BBDR, Snow, Xmin, Ymin, xsize, ysize)
            
                    M = M + bbdr.M
                    V = V + bbdr.V
                    E = E + bbdr.E
                    Mask = Mask + bbdr.mask

                    if (i < NumberOfBBDRs-2):
                        # Continue the loop throuh all BBDRs
                        i = i + 1
                    else:
                        # Reached the end of the list
                        break

            print "Accumulator file for", str(BBDR_DoY), "was created"

            WriteAccumulatorFlag = 1
            if WriteAccumulatorFlag == 1:
                WriteAccumulator(BBDR_name, M, V, E, Mask, OutputDir)

            i = i + 1

    return 0


def WriteAccumulator(Accumulator_filename, M, V, E, Mask, OutputDir):
    # Write accumulator to a local filesystem and then mv to another storage
    print "Saving accumulator files to NumPy binary format..."
    numpy.save(OutputDir + '/M_' + Accumulator_filename, M)
    numpy.save(OutputDir + '/V_' + Accumulator_filename, V)
    numpy.save(OutputDir + '/E_' + Accumulator_filename, E)
    numpy.save(OutputDir + '/mask_' + Accumulator_filename, Mask)

    # Move accumulator files to permanent storage
    #import os
    #command = "mv " + OutputDir + "/*" + Accumulator_filename + "*.npy " + StorageDir
    #print command
    #os.system(command)



#------------------------------------------------------------------------------------------#

from IPython.Shell import IPShellEmbed
ipshell = IPShellEmbed([''], banner = 'Dropping into IPython', exit_msg = 'Leaving Interpreter, back to program.')

import sys
import os
import time

print time.strftime("Processing starting at: %d/%m/%Y %H:%M:%S")
start = time.time()

tile = sys.argv[1]
year = sys.argv[2]
day = sys.argv[3] # Day can be the first digit of the julian day, e.g. for DoY 001, 0, for DoY 250, 2
Snow = int(sys.argv[4])

#DataDir = "/unsafe/GlobAlbedo"
# use this root path for testing on bcserver13:
DataDir = '/data/GlobAlbedo'
if Snow == 1:
    OutputDir = DataDir + "/BBDR/AccumulatorFiles/" + year + "/" + tile + "/Snow"
    StorageDir = DataDir + "/BBDR/AccumulatorFiles/" + year + "/" + tile + "/Snow"
else:
    OutputDir = DataDir + "/BBDR/AccumulatorFiles/" + year + "/" + tile + "/NoSnow"
    StorageDir = DataDir + "/BBDR/AccumulatorFiles/" + year + "/" + tile + "/NoSnow"
#StorageDir = "/disk/" + GlobAlbedoNN + "/GlobAlbedo/BBDR/AccumulatorFiles/" + year + "/" + tile

obsfiles = GetObsFiles(year, day, tile, DataDir + '/BBDR/*')
if len(obsfiles.allfiles) == 0:
    print "No files to process."
    sys.exit(0)

#print obsfiles.allfiles
#print obsfiles.alldoys

xmin = ymin = xmax = ymax = 1
#xmin = 1
#xmax = 100
#ymin = 1
#ymax = 100

Invalid = -9999

#GetBBDR return BBDR, kernels, SD, correlation, mask
Number_of_Files = len(obsfiles.allfiles)

data = CreateAccumulatorFiles(DataDir, OutputDir, StorageDir,  obsfiles, year, tile, Snow, xmin, ymin, xmax, ymax)

reading_time = time.time()
print "Reading/formating BBDRs time elapsed = ", (reading_time - start)/3600.0, "hours =",  (reading_time - start)/60.0 , "minutes"

#--------------------------------------------
print time.strftime("Processing finished at: %d/%m/%Y %H:%M:%S")
end = time.time()

"Total time elapsed = ", (end - start)/3600.0, "hours =",  (end - start)/60.0 , "minutes"
