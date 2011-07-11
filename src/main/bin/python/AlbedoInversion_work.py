#!/usr/local/epd-7.0-2-rh5-x86_64/bin/python

"""
GlobAlbedo Broadband albedo inversion

Authors:   Gerardo Lopez-Saldana <lsg@mssl.ucl.ac.uk>
           P. Lewis <plewis@ucl.ac.uk>
"""
import os
import time

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


def GetPrior(File, Prior_ScaleFactor=10.0, xmin=1, ymin=1, xmax=1, ymax=1):
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

    print "Opening prior", filename[0], "with scale factor:", str(Prior_ScaleFactor)

    # The number of data channels is nBands (mean) + nBands (sd) + N samples + Mask
    # which should be 20 usually, giving nBands = (nb-2)/2
    nBands = (BandCount-2)/2

    #Create M and V
    nWaveBands = 3
    C = numpy.zeros((3*nWaveBands,3*nWaveBands, xsize, ysize), numpy.float32)
    C_inv = numpy.zeros((3*nWaveBands,3*nWaveBands, xsize, ysize), numpy.float32)
    C_inv_F = numpy.zeros((3*nWaveBands, xsize, ysize), numpy.float32) # Matrix to store C^-1 * Fpr
    Mask = numpy.zeros((xsize, ysize), numpy.float32)

    prior = numpy.zeros((nBands, xsize, ysize), numpy.float32)
    priorSD = numpy.zeros((nBands, xsize, ysize), numpy.float32)

    #Open N_samples, which is band 19
    Nsamples = dataset.GetRasterBand(BandCount-1).ReadAsArray(Xmin, Ymin, xsize, ysize)
    #Open mask, which is band 20
    MaskFlag = dataset.GetRasterBand(BandCount).ReadAsArray(Xmin, Ymin, xsize, ysize)

    for band in range(0,nBands):
        prior[band,:,:] = dataset.GetRasterBand(band+1).ReadAsArray(Xmin, Ymin, xsize, ysize)
        priorSD[band,:,:] = dataset.GetRasterBand(band+1+nBands).ReadAsArray(Xmin, Ymin, xsize, ysize)

#	if band == 0:
#	    print "Prior mean (1) [0,570,288]: ",  prior[0,288,570]
#    	    print "Prior SD (1) [0,570,288]  : ",  priorSD[0,288,570]
#	    print "mask (1) [570,288]        : ",  Mask[288,570]

        #---------------------------------------------------------------------------------------------------------------------------------
        # In some cases the Snow prior has information in parameters, no uncert, set Mask=1.0, uncert to 1.0 and NSamples = 1.0e-20
        Mask[:,:] = numpy.where((prior[band]>0.0) & (priorSD[band]==0.0), 1.0, Mask)
        Nsamples[:,:] = numpy.where((prior[band]>0.0) & (priorSD[band]==0.0), 1.0e-20, Nsamples)
        priorSD[band,:,:] = numpy.where((prior[band]>0.0) & (priorSD[band]==0.0), 1.0, priorSD[band])

#	if band == 0:
#            print "Prior mean (2) [0,570,288]: ",  prior[0,288,570]
#            print "Prior SD (2) [0,570,288]  : ",  priorSD[0,288,570]
#	    print "mask (2) [570,288]        : ",  Mask[288,570]

        # In some other cases the Snow prior has information in parameters, has uncert, mask = 0, set uncert to 1.0 and NSamples = 1.0e-20
        Nsamples[:,:] = numpy.where((prior[band]>0.0) & (priorSD[band]>0.0) & (Mask>0), 1.0e-20, Nsamples)
        priorSD[band,:,:] = numpy.where((prior[band]>0.0) & (priorSD[band]>0.0) & (Mask>0), 1.0, priorSD[band])
        Mask[:,:] = numpy.where((prior[band]>0.0) & (priorSD[band]>0.0) & (Mask>0), 1.0, Mask)
        #---------------------------------------------------------------------------------------------------------------------------------

#	if band == 0:
#            print "Prior mean (3) [0,570,288]: ",  prior[0,288,570]
#            print "Prior SD (3) [0,570,288]  : ",  priorSD[0,288,570]
#	    print "mask (3) [342,570,288]        : ",  Mask[288,570]

        # Scale uncertaities in the prior
        priorSD[band,:,:] = priorSD[band,:,:] * Prior_ScaleFactor

#	if band == 0:
#            print "Prior scale factor        : ",  Prior_ScaleFactor
#            print "Prior mean (4) [0,570,288]: ",  prior[0,288,570]
#            print "Prior SD (4) [0,570,288]  : ",  priorSD[0,288,570]
#	    print "mask (4) [570,288]        : ",  Mask[288,570]

        # Cap the prior uncert to 1.0
        priorSD[band,:,:] = numpy.where(priorSD[band,:,:] > 1.0, 1.0, priorSD[band,:,:])

        #Fill leading diagonal elements of C
        C[band,band,:,:] = priorSD[band,:,:] * priorSD[band,:,:]

#    print "Prior mean [0,342,200]: ",  prior[0,200,342]
#    print "Prior SD [0,342,200]  : ",  priorSD[0,200,342]
#    print "NSamples [342,200]  : ",  Nsamples[200,342]
#    print "mask [342,200]      : ",  Mask[200,342]
   
#    print "Prior mean [0,427,383]: ",  prior[0,383,427]
#    print "Prior SD [0,427,383]  : ",  priorSD[0,383,427]
#    print "NSamples [427,383]  : ",  Nsamples[383,427]
#    print "mask [427,383]      : ",  Mask[383,427]

#    print "Prior mean [0,570,288]: ",  prior[0,288,570]
#    print "Prior SD [0,570,288]  : ",  priorSD[0,288,570]
#    print "NSamples [570,288]  : ",  Nsamples[288,570]
#    print "mask [570,288]]     : ",  Mask[288,570]

#    print "Prior mean [0,727,291]: ",  prior[0,291,727]
#    print "Prior SD [0,727,291]  : ",  priorSD[0,291,727]
#    print "NSamples [727,291]  : ",  Nsamples[291,727]
#    print "mask [727,291]      : ",  Mask[291,727]

#    print "Prior mean [0,714,541]: ",  prior[0,541,714]
#    print "Prior SD [0,714,541]  : ",  priorSD[0,541,714]
#    print "NSamples [714,541]  : ",  Nsamples[541,714]
#    print "mask [714,541]      : ",  Mask[541,714]

    # Calculate C inverse
    for columns in range(0,xsize):
        for rows in range(0,ysize):
            if MaskFlag[columns,rows] >= 1 and MaskFlag[columns,rows] <= 3 and Nsamples[columns,rows] > 0.0 \
               or (MaskFlag[columns,rows] == 15 and Nsamples[columns,rows] > 0.0) \
               or (Nsamples[columns,rows] > 0.0):
                Mask[columns,rows] = 1.0
                # Calculate C inverse 
                try:
                    C_inv[:,:,columns,rows] = numpy.matrix(C[:,:,columns,rows]).I
#					if columns == 541 and  rows == 714:
#					    print "C_inv OK"
                except numpy.linalg.LinAlgError:
                    # For some cases the prior has information in the parameters and SD but not uncert, therefore just assign the max uncert = 1.0
#					if columns == 541 and  rows == 714:
#					    print "C_inv EXCEPTION!"
#						print "prior[:,714,541]  : ", prior[:,541,714]
#						print "priorSD[:,714,541]: ", priorSD[:,541,714]
#						print "numpy.where( (prior[:,714,541]>0.0)): ", numpy.where( (prior[:,columns,rows]>0.0)
                    if numpy.where( (prior[:,columns,rows]>0.0) & (prior[:,columns,rows]<=1.0) )[0].shape[0] == 9 and \
                       numpy.where( (priorSD[:,columns,rows]>=0.0) & (priorSD[:,columns,rows]<=1.0) )[0].shape[0] == 9:

                        C[:,:,columns,rows] =  numpy.identity(nWaveBands*nWaveBands)
                        C_inv[:,:,columns,rows] = numpy.matrix(C[:,:,columns,rows]).I

                        for i in range(0,nBands):
                            # Compute C^-1  * Fpr
                            C_inv_F[i,columns,rows] = C_inv[i,i,columns,rows] * prior[i,columns,rows]

                        continue

                    else:
                        #print columns,rows
                        #print priorSD[:,columns,rows]
                        # Uncertainty information in C generates is a singular matrix, do not process pixel
                        Mask[columns,rows] = 0.0
                        continue

                for i in range(0,nBands):
                    # Compute C^-1  * Fpr
                    C_inv_F[i,columns,rows] = C_inv[i,i,columns,rows] * prior[i,columns,rows]

    return ReturnPriorData(C_inv, C_inv_F, Mask, prior, priorSD)


class ReturnPriorData(object):
    def __init__(self, MData, VData, Mask, Parameters, Parameters_SD):
        self.MData = MData
        self.VData = VData
        self.Mask = Mask
        self.Parameters = Parameters
        self.Parameters_SD = Parameters_SD


def GetMetadata(File):
    dataset = gdal.Open( File, GA_ReadOnly )
    xmax, ymax, BandCount = dataset.RasterXSize, dataset.RasterYSize, dataset.RasterCount
    BandNames = dataset.GetMetadata_List()
    dataset = None

    return ReturnValueGetMetadata(xmax, ymax, BandCount, BandNames)

class ReturnValueGetMetadata(object):
    def __init__(self, xmax, ymax, BandCount, BandNames):
        self.xmax = xmax
        self.ymax = ymax
        self.BandCount = BandCount
        self.BandNames = BandNames


def GetDaysOfYear(filelist):

    file_list_lenght = len(filelist)

    DoY = numpy.zeros(file_list_lenght, numpy.int16)
    Year = numpy.zeros(file_list_lenght, numpy.int16)

    for i in range(0, file_list_lenght):
        # Extract DoY, e.g. 356, from the directory name:
        # /unsafe/GlobAlbedo/BBDR/MERIS/2006/h11v04/2006001_162315/M_2006001_162315.npy
        DoY[i] = os.path.basename(filelist[i])[6:9]
        Year[i] = os.path.basename(filelist[i])[2:6]

    return DoY, Year

def GetObsFiles(doy, year, tile, location, wings, Snow):

    import sys
    import glob

    allFiles = []
    allDoYs = []
    count = 0
    # Actually, the 'MODIS' doy is 8 days behind the period centre
    # so add 8 onto doy, wehere we understand DoY to mean MODIS form DoY
    doy += 8

    if Snow == 1:
        files_path = location + '/200?/' + tile + '/Snow/python/M_200?*.npy'
    else:
        files_path = location + '/200?/' + tile + '/NoSnow/python/M_200?*.npy'

    #Get all BBDR directories - the symlink to the .data directories
    filelist = glob.glob(files_path)

    #It is neccesary to sort BBDR files by YYYY/YYYYMMDD
    from operator import itemgetter, attrgetter

    TmpList = []
    for i in range(0,len(filelist)):
        TmpList.append(tuple(filelist[i].split('/'))) # create a list of tupples

    # First, sort by year, In this case year is in field 5
    # e.g. /unsafe/GlobAlbedo/BBDR/AccumulatorFiles/2005/h18v04/NoSnow/M_2005363.npy
    s = sorted(TmpList, key=itemgetter(5))
    # Now sort the sorted-by-year list by filename
    s = sorted(s, key=itemgetter(8))

    # Tuple to list
    filelist = []
    file = ''
    for i in range(0,len(s)):
        for j in range(1,len(s[0])):
            file = file + '/' + s[i][j]
        filelist.append(file)
        file = ''

    DoYs, Year = GetDaysOfYear(filelist)

    # Left wing
    print "doy, wings: ", doy, ", ", wings
    if ( 365+(doy-wings)<=366 ):
        DoY_index = numpy.where((DoYs>=366+(doy-wings)) & (Year<year))[0]
#	print "doy_index: ", DoY_index
        for i in DoY_index:
            allFiles.append(filelist[i])
#	    print "filellist_i: ", DoYs[i], ", ", filelist[i]
            allDoYs.append((DoYs[i]-doy-366))

    # Center
#    sys.exit()
    DoY_index = numpy.where((DoYs<doy+wings) & (DoYs>=doy-wings) & (Year==year))[0]
    for i in DoY_index:
        allFiles.append(filelist[i])
        allDoYs.append(DoYs[i]-doy)

    # Right wing
    if ( (doy+wings)-365>0 ):
        DoY_index = numpy.where((DoYs<=(doy+wings)-365) & (Year>year))[0]
        for i in DoY_index:
            allFiles.append(filelist[i])
            allDoYs.append(DoYs[i]-doy+365)

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

def Inversion(M_acc, V_acc, Mask_acc, Lambda, M_prior, V_prior, Mask_prior, Parameters_prior, columns, rows, UsePrior, Invalid=-9999):
    from  numpy.linalg import lstsq
    from  numpy.linalg import svd

    Invalid = -9999
    nWaveBands = 3

    parameters = numpy.zeros((3*nWaveBands, columns, rows), numpy.float32)
    parameters_no_prior = numpy.zeros((3*nWaveBands, columns, rows), numpy.float32)

    det = numpy.zeros((columns, rows), numpy.float32)
    uncertainties = numpy.zeros((3*nWaveBands,3*nWaveBands, columns, rows), numpy.float32)
    uncertainties_no_prior = numpy.zeros((3*nWaveBands,3*nWaveBands, columns, rows), numpy.float32)

    RelativeEntropy = numpy.zeros((columns, rows), numpy.float32)

#    for i in range(0,9):
#        for j in range(0,9):
#            print "Inversion: M_prior[",i,",",j,",714,541]: ", M_prior[j,i,541,714]

#    for i in range(0,9):
#        print "Inversion: V_prior[",i,",714,541]: ", V_prior[i,541,714]
#        print "Inversion: Parameters_prior[",i,",714,541]: ", Parameters_prior[i,541,714]

#    print "Inversion: Mask_prior[714,541]: ", Mask_prior[541,714]

    for column in range(0,columns):
        for row in range(0,rows):

            # If boths masks, in BBDR data and prior are 1 and there are no zeros in the main diagonal of BBDR uncertainties
            #if Mask_acc[column,row] > 0 and Mask_prior[column,row] > 0 and numpy.where(numpy.diagonal(M_acc[:,:,column,row]) <= 0)[0].shape[0] == 0:
            if Mask_acc[column,row] > 0 and Mask_prior[column,row] > 0:

                M = M_acc[:,:,column,row]
                V = V_acc[:,column,row]

                M_p = M_prior[:,:,column,row]
                V_p = V_prior[:,column,row]

                # Include prior informatio in M & V to constraint the inversion
                if UsePrior == 1:
                    for i in range(0,nWaveBands*3):
                        M[i,i] += M_p[i,i]

                    V += V_p

                #===========================
                # Uncertainties in inversion
                #===========================
                try:
                    tmp_uncert = numpy.matrix(M).I

                    # Test for Nan in uncertainties
                    NaN_indices = numpy.where(numpy.isnan(tmp_uncert))[0].shape[1]
                    # Test for zeros in main diagonal
                    Zeros_indices = numpy.where(numpy.diagonal(tmp_uncert) < 0)[0].shape[0]

                    if NaN_indices > 0 or Zeros_indices > 0:
                        tmp_uncert = Invalid

                    uncertainties[:,:,column,row] = tmp_uncert

                except numpy.linalg.LinAlgError:
                    #print columns,rows
                    #print priorSD[:,columns,rows]
                    # Uncertainty information in C generates is a singular matrix, do not process pixel
                    parameters[:,column,row] = Invalid
                    uncertainties[:,:,column,row] = Invalid
                    Mask_acc[column,row] = 0.0

                # If Mask_acc was modified during uncertainties calculation avoid parameters estimation
                if Mask_acc[column,row] == 0.0:
                    continue

		# Compute least-squares solution to equation Ax = b
                # http://docs.scipy.org/doc/scipy/reference/generated/scipy.linalg.lstsq.html

                (P, rho_residuals, rank, svals) = lstsq(M, V)
                parameters[:,column,row] = P

                # Compute singluar value decomposition
                # http://docs.scipy.org/doc/scipy/reference/generated/scipy.linalg.svd.html
                U, S, Vh = svd(M)
                det[column,row] = 0.5*numpy.log(numpy.product(1/S)) + S.shape[0] * numpy.sqrt(numpy.log(2*numpy.pi*numpy.e))

                # This can be calculated earlier for the prior
                U, S, Vh = svd(numpy.matrix(M_p))
                PriorDet = 0.5*numpy.log(numpy.product(1/S)) + S.shape[0] * numpy.sqrt(numpy.log(2*numpy.pi*numpy.e))
                if UsePrior == 1:
                    RelativeEntropy[column,row] = PriorDet - det[column,row]
                else:
                    RelativeEntropy[column,row] = Invalid # As this has no meaning

            else:
                # If there is not a single sample available, just use the prior parameters (f0, f1, f2) and prior uncertainties
                if Mask_prior[column,row] > 0:
                    for i in range(0,nWaveBands*3):
                        parameters[i,column,row] = Parameters_prior[i,column,row]

                    if UsePrior == 1:
                        uncertainties[:,:,column,row] = numpy.matrix(M_prior[:,:,column,row]).I
                        U, S, Vh = svd(numpy.matrix(M_prior[:,:,column,row]))
                        PriorDet = 0.5*numpy.log(numpy.product(1/S)) + S.shape[0] * numpy.sqrt(numpy.log(2*numpy.pi*numpy.e))
                        det[column,row] = PriorDet
                        RelativeEntropy[column,row] = 0.0
                    else:
                        uncertainties[:,:,column,row] = Invalid # As this has no meaning
                        det[column,row] = Invalid # as this has no meaning
                        RelativeEntropy[column,row] = Invalid # as this has no meaning
                        # a flag should be passed through to say that it is prior


    # Make all parameters (f0, f1, f2) = 0 if any parameter is le 0 OR ge 1.0
    #indices = numpy.where((parameters <= 0.0) | (parameters >= 1.0))
    #parameters[:, indices[1], indices[2]] = 0.0
    #uncertainties[:,:, indices[1], indices[2]] = 0.0
    #uncertainties = numpy.where(uncertainties>=1.0, 1.0, uncertainties)

    return ReturnValuesInversion(parameters, det, RelativeEntropy, uncertainties, Mask_acc)

class ReturnValuesInversion(object):
    def __init__(self, parameters, Entropy, RelativeEntropy, uncertainties, Mask):
        self.parameters = parameters
        self.Entropy = Entropy
        self.RelativeEntropy = RelativeEntropy
        self.uncertainties = uncertainties
        self.Mask = Mask


def WriteDataset(File, Inversion, DoYClosestSample, GoodnessOfFit, Tile):

    format = "ENVI"
    driver = gdal.GetDriverByName(format)

    columns = Inversion.parameters.shape[1]
    rows = Inversion.parameters.shape[2]
    number_of_parameters = Inversion.parameters.shape[0]
    number_of_uncertainties = ((number_of_parameters*number_of_parameters)-number_of_parameters)/2 + number_of_parameters
    # 1 extra: NSamples, 2 extra: Entropy, RelativeEntropy, 1 extra: DoYClosestSample, 1 extra: GoodnessOfFit
    number_of_bands = number_of_parameters + number_of_uncertainties + 1 + 2 + 1 + 1

    new_dataset = driver.Create( File, columns, rows, number_of_bands , GDT_Float32 )

    print "num params: ", number_of_parameters
    # Write parameters
    for parameter in range(0, number_of_parameters):
        new_dataset.GetRasterBand(parameter+1).WriteArray(Inversion.parameters[parameter,:,:])

    # Write uncertainties
    count = 0
    for i in range(0, number_of_parameters):
        for j in range(i, number_of_parameters):
            new_dataset.GetRasterBand(number_of_parameters+count+1).WriteArray(Inversion.uncertainties[i,j,:,:])
            count += 1

    # Entropy
    new_dataset.GetRasterBand(number_of_bands-4).WriteArray(Inversion.Entropy[:,:])

    # Relative Entropy
    new_dataset.GetRasterBand(number_of_bands-3).WriteArray(Inversion.RelativeEntropy[:,:])

    # Weighted number of samples
    new_dataset.GetRasterBand(number_of_bands-2).WriteArray(Inversion.Mask[:,:])

    # Distance in days to the closest sample use to perform the inversion
    new_dataset.GetRasterBand(number_of_bands-1).WriteArray(DoYClosestSample[:,:])

    # Goodness of fit
    new_dataset.GetRasterBand(number_of_bands).WriteArray(GoodnessOfFit[:,:])

    new_dataset = None

    # Write header
    ULC_coordinates = GetUpperLeftCoordinates(Tile)
    output_header = open(File.split(".bin")[0] + ".hdr", 'w')
    header_template = open("/data/GlobAlbedo/src/metadata/generic_header.hdr")
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
	
def WriteDatasetSingleMatrixElement(File, M, V, E, Tile, OutputDir):

    format = "ENVI"
    driver = gdal.GetDriverByName(format)

    columns = 1200
    rows = 1200
    number_of_bands = 12

    OutputFile = OutputDir + '/matrices_' + File
    print "TEST - Writing single matrix elements to ENVI file: ", OutputFile
    new_dataset = driver.Create(OutputFile, columns, rows, number_of_bands , GDT_Float32 )

    # Write matrix elements
    new_dataset.GetRasterBand(1).SetDescription('M_00')
    new_dataset.GetRasterBand(1).WriteArray(M[0,0,:,:])
    new_dataset.GetRasterBand(2).SetDescription('M_03')
    new_dataset.GetRasterBand(2).WriteArray(M[0,3,:,:])
    new_dataset.GetRasterBand(3).SetDescription('M_26')
    new_dataset.GetRasterBand(3).WriteArray(M[2,6,:,:])
    new_dataset.GetRasterBand(4).SetDescription('M_33')
    new_dataset.GetRasterBand(4).WriteArray(M[3,3,:,:])
    new_dataset.GetRasterBand(5).SetDescription('M_44')
    new_dataset.GetRasterBand(5).WriteArray(M[4,4,:,:])
    new_dataset.GetRasterBand(6).SetDescription('M_67')
    new_dataset.GetRasterBand(6).WriteArray(M[6,7,:,:])
    new_dataset.GetRasterBand(7).SetDescription('M_88')
    new_dataset.GetRasterBand(7).WriteArray(M[8,8,:,:])
    new_dataset.GetRasterBand(8).SetDescription('V_0')
    new_dataset.GetRasterBand(8).WriteArray(V[0,:,:])
    new_dataset.GetRasterBand(9).SetDescription('V_3')
    new_dataset.GetRasterBand(9).WriteArray(V[3,:,:])
    new_dataset.GetRasterBand(10).SetDescription('V_5')
    new_dataset.GetRasterBand(10).WriteArray(V[5,:,:])
    new_dataset.GetRasterBand(11).SetDescription('V_8')
    new_dataset.GetRasterBand(11).WriteArray(V[8,:,:])
    new_dataset.GetRasterBand(12).SetDescription('E')
    new_dataset.GetRasterBand(12).WriteArray(E[:,:])

    # Write header
    ULC_coordinates = GetUpperLeftCoordinates(Tile)
    output_header = open(File.split(".bin")[0] + ".hdr", 'w')
    header_template = open("/data/GlobAlbedo/src/metadata/generic_header.hdr")
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
    ULC_coords_file = '/data/GlobAlbedo/src/metadata/Tiles_UpperLeftCorner_Coordinates.txt'
    ULC_coordinates = open(ULC_coords_file, 'r')

    coordinates = ''
    for line in ULC_coordinates:
        if Tile in line:
            coordinates = line.split(',')[1].strip() + ',' + line.split(',')[2].strip()

    return coordinates


def Accumulator(AllFiles, AllDoY, year, tile, wings, sensor, AllWeights, xmin, ymin, xmax, ymax, columns, rows):

    NumberOfAccumulators = AllWeights.shape[0]
    nWaveBands = 3
    print "Number Accumulators: ", NumberOfAccumulators

    #Acumulator matrices
#    allMTest = numpy.zeros((45, 3*nWaveBands,3*nWaveBands,columns, rows), numpy.float32)
    testsize = 4*45*9*9*1200*1200/1000000
    print "Test array in MB: ", testsize
    allM = numpy.zeros((NumberOfAccumulators, 3*nWaveBands,3*nWaveBands,columns, rows), numpy.float32)
    allV = numpy.zeros((NumberOfAccumulators, 3*nWaveBands,columns, rows), numpy.float32)
    allE = numpy.zeros((NumberOfAccumulators, columns, rows), numpy.float32)
    allMask = numpy.zeros((NumberOfAccumulators, columns, rows), numpy.float32)

    DoYClosestSample = numpy.zeros((NumberOfAccumulators, columns, rows), numpy.float32)

    index=0
    for BBDR in AllFiles:

        M = numpy.zeros((3*nWaveBands,3*nWaveBands, columns, rows), numpy.float32)
        V = numpy.zeros((3*nWaveBands, columns, rows), numpy.float32)
        E = numpy.zeros((columns, rows), numpy.float32)
        Mask = numpy.zeros((columns, rows), numpy.float32)

        #First verify whether or not accumulator exists for the specific BBDR
        BBDR_dirname = os.path.dirname(BBDR)
        BBDR_name = os.path.basename(os.path.splitext(BBDR)[0])[2:]

        if os.path.isfile(BBDR_dirname + '/M_' + BBDR_name + '.npy') and \
           os.path.isfile(BBDR_dirname + '/V_' + BBDR_name + '.npy') and \
           os.path.isfile(BBDR_dirname + '/E_' + BBDR_name + '.npy') and \
           os.path.isfile(BBDR_dirname + '/mask_' + BBDR_name + '.npy'):

            #As the accumulator exists, then get the M, V and mask from the NumPy binary format
            print "Load M, V and mask for", BBDR_name, "data from accumulator..."

            try:
                M[:,:,:,:] = numpy.load(BBDR_dirname + '/M_' + BBDR_name + '.npy')
                V[:,:,:] = numpy.load(BBDR_dirname + '/V_' + BBDR_name + '.npy')
                E[:,:] = numpy.load(BBDR_dirname + '/E_' + BBDR_name + '.npy')
                Mask[:,:] = numpy.load(BBDR_dirname + '/mask_' + BBDR_name + '.npy')
            except:
                print BBDR, "accumulator file error."
                continue 

        else:
            #Load data from BBDR
            print BBDR, "accumulator file must be created."
            index += 1
            continue

	print "BBDR: ", BBDR
        for i in range(0,NumberOfAccumulators):
            BBDR_DaysToDoY = numpy.abs(AllDoY[i,AllFiles.index(BBDR)]) + 1 # Plus one to avoid 0s
	    print "BBDR_DaysToDoY: ", BBDR_DaysToDoY
            #First file
            if AllFiles.index(BBDR) == 0:
                DoYClosestSample[i] = numpy.where(Mask > 0, BBDR_DaysToDoY, DoYClosestSample[i])
            else:
                # Where there are no observations asign the DoYClosestSample
                DoYClosestSample[i] = numpy.where((Mask>0) & (DoYClosestSample[i]==0), BBDR_DaysToDoY, DoYClosestSample[i])
                # Find the closest DoY to BBDR_DaysToDoY
                DoYClosestSample[i] = numpy.where((Mask>0) & (DoYClosestSample[i]>0), numpy.minimum(BBDR_DaysToDoY, DoYClosestSample[i]), DoYClosestSample[i])
	    
	    print "DoYClosestSample: ", DoYClosestSample[i,540,480]	    

        for i in range(0,NumberOfAccumulators):
            # Fill acumulators
            allM[i] += M * AllWeights[i,index]
            allV[i] += V * AllWeights[i,index]
            allE[i] += E * AllWeights[i,index]
            allMask[i] += Mask * AllWeights[i,index]

        index += 1

    return ReturnData(allM, allV, allE, allMask, DoYClosestSample)


class ReturnData(object):
    def __init__(self, MData, VData, EData, Mask, DoYClosestSample):
        self.MData = MData
        self.VData = VData
        self.EData = EData
        self.Mask = Mask
        self.DoYClosestSample = DoYClosestSample


def GetGoodnessOfFit(M, V, E, F, Mask, columns, rows):
    '''
    Compute goodnes of fit of the model to the observation set x^2

        x^2 = F^T M F + F^T V - 2E

            F and V are stored as a 1X9 vector (transpose), so, actually F^T and V^T are the regular 9X1 vectors

        see Eq. 19b in GlobAlbedo -  Albedo ATBD

    '''

    GoodnessOfFit = numpy.zeros((columns, rows), numpy.float32)

    for column in range(0,columns):
        for row in range(0,rows):
            if Mask[column,row] > 0:
                GoodnessOfFit[column,row] = ( numpy.matrix(F[:,column,row]) * numpy.matrix(M[:,:,column,row]) * numpy.matrix(F[:,column,row]).T ) + \
                                            ( numpy.matrix(F[:,column,row]) * numpy.matrix(V[:,column,row]).T ) - \
                                            ( 2 * numpy.matrix(E[column,row]) )

    return GoodnessOfFit

def WriteAccumulator(Accumulator_filename, M, V, E, Mask, OutputDir):
    # Write accumulator to a local filesystem
    print "Saving accumulator files to NumPy binary format..."
    print "accumulator path: ", OutputDir + '/M_' + Accumulator_filename
    numpy.save(OutputDir + '/M_' + Accumulator_filename, M)
    numpy.save(OutputDir + '/V_' + Accumulator_filename, V)
    numpy.save(OutputDir + '/E_' + Accumulator_filename, E)
    numpy.save(OutputDir + '/mask_' + Accumulator_filename, Mask)


def ProcessInversion(M_acc, V_acc, E_acc, Mask_acc, Lambda, PriorFile, columns, rows, UsePrior, DoYClosestSample, tile, year, Snow, OUTDIR, Invalid=-9999):

    prior = GetPrior(PriorFile, Prior_ScaleFactor, xmin, ymin, xmax, ymax)
    columns = prior.MData.shape[2]
    rows =  prior.MData.shape[3]
    DoY = int(os.path.basename(PriorFile).split('.')[1])

    if len(str(DoY)) == 1:
        strDoY = '00' + str(DoY)
    elif len(str(DoY)) == 2:
        strDoY = '0' + str(DoY)
    else:
        strDoY = str(DoY)

    GlobAlbedoInversion = Inversion(M_acc, V_acc, Mask_acc, Lambda, prior.MData, prior.VData, prior.Mask, prior.Parameters, columns, rows, UsePrior)
    GoodnessOfFit = GetGoodnessOfFit(M_acc, V_acc, E_acc, GlobAlbedoInversion.parameters, GlobAlbedoInversion.Mask, columns, rows)    

    print "Saving results to ENVI file..."
#    OUTDIR = os.getcwd()
#    OUTDIR = '/data/GlobAlbedo/inversion_py'
    if Snow == 1:
        OutputFilename = OUTDIR + '/GlobAlbedo.' + str(year) + strDoY + '.' + tile + '.Snow'
        AccFilename = 'GlobAlbedo.' + str(year) + strDoY + '.' + tile + '.Snow'
    else:
        OutputFilename = OUTDIR + '/GlobAlbedo.' + str(year) + strDoY + '.' + tile + '.NoSnow'
        AccFilename = 'GlobAlbedo.' + str(year) + strDoY + '.' + tile + '.NoSnow'

    WriteDataset(OutputFilename + '.bin', GlobAlbedoInversion, DoYClosestSample, GoodnessOfFit, tile)

#    WriteAccumulator(AccFilename, M_acc, V_acc, E_acc, GlobAlbedoInversion.Mask, OUTDIR + '/AccFiles')
#    WriteAccumulator(AccFilename, M_acc, V_acc, E_acc, GlobAlbedoInversion.Mask, OUTDIR)
#    WriteAccumulator(AccFilename, M_acc, V_acc, E_acc, Mask_acc, OUTDIR)
#    WriteDatasetSingleMatrixElement(AccFilename + '.bin', M_acc, V_acc, E_acc, tile, OUTDIR)


#------------------------------------------------------------------------------------------#
# First import the embeddable shell class
from IPython.Shell import IPShellEmbed

# Now create an instance of the embeddable shell. The first argument is a
# string with options exactly as you would type them if you were starting
# IPython at the system command line. Any parameters you want to define for
# configuration can thus be specified here.
ipshell = IPShellEmbed([''],
                       banner = 'Dropping into IPython',
                       exit_msg = 'Leaving Interpreter, back to program.')


import sys
import os
import time
import glob

print time.strftime("Processing starting at: %d/%m/%Y %H:%M:%S")
start = time.time()

print "argv: ", sys.argv

tile = sys.argv[1]
year = int(sys.argv[2])
wings = int(sys.argv[3])

print "tile: ", tile
print "year: ", year
print "wings: ", wings
#sys.exit();

if sys.argv[4] == "ALL":
    sensor = "*"
else:
    sensor = sys.argv[4]

Prior_ScaleFactor = float(sys.argv[5])
Snow = int(sys.argv[6])

PriorDoY = sys.argv[7]
#PriorDoY = "*"
OutDir = sys.argv[8]
print "outdir: ", OutDir
DataDir = '/data/GlobAlbedo'

#Get ALL DoY to process from prior
if Snow == 1:
    prior_files_path = DataDir + '/Priors/' + tile + '/background/processed.p1.0.618034.p2.1.00000/Kernels.' + PriorDoY + '*.005.' + tile + '.backGround.Snow.bin'
else:
    prior_files_path = DataDir + '/Priors/' + tile + '/background/processed.p1.0.618034.p2.1.00000/Kernels.' + PriorDoY + '*.005.' + tile + '.backGround.NoSnow.bin'

PriorFiles = glob.glob(prior_files_path)
PriorFiles.sort()

halfLife = 11.54
Lambda = 1.0 # lambda is a reserved word in Python
i = 0

print "PriorFiles: ", PriorFiles

for PriorFile in PriorFiles:
    DoY = int(os.path.basename(PriorFile).split('.')[1])
    obsfiles = GetObsFiles(DoY, year, tile, DataDir + '/BBDR/AccumulatorFiles', wings, Snow)
    print "PRIOR: ",  DoY, ", ", PriorFile
#    for i in range(0,len(obsfiles.allfiles)):
#	 print "       obsfiles: ",  obsfiles.alldoys[i], ", ", obsfiles.allfiles[i]
#    print "       obsfiles: ", obsfiles.allfiles
#    print "       obsdoys : ", obsfiles.alldoys
#    print obsfiles.allfiles
#    print obsfiles.alldoys

    weight = numpy.exp(-1.0 * abs(obsfiles.alldoys)/halfLife)
    print "weight: ", weight

#    NumberOfBBDRs = len(obsfiles.allfiles)
#    AllWeights = numpy.zeros((len(PriorFiles), NumberOfBBDRs), numpy.float32)
#    AllDoY = numpy.zeros((len(PriorFiles), NumberOfBBDRs), numpy.float32)

    if PriorFile == PriorFiles[0]:
        NumberOfBBDRs = len(obsfiles.allfiles)
        AllWeights = numpy.zeros((len(PriorFiles), NumberOfBBDRs), numpy.float32)
        AllDoY = numpy.zeros((len(PriorFiles), NumberOfBBDRs), numpy.float32)

        AllWeights[0,:] = weight
        AllDoY[0,:] = obsfiles.alldoys

    else:
        AllWeights[i,:] = weight
        AllDoY[i,:] = obsfiles.alldoys

    i += 1
    print ""

#sys.exit()
xmin = ymin = xmax = ymax = 1
#xmin = 1
#xmax = 100
#ymin = 1
#ymax = 100

columns = rows = 1200

# Create accumulator
data = Accumulator(obsfiles.allfiles, AllDoY, year, tile, wings, sensor, AllWeights, xmin, ymin, xmax, ymax, columns, rows)

reading_time = time.time()
print "Reading/formating BBDRs time elapsed = ", (reading_time - start)/3600.0, "hours =",  (reading_time - start)/60.0 , "minutes"

#---------------------
# Inversion WITH Prior
#---------------------
NumProcesses = 12
PriorFileToProcess = []

for PriorFile in PriorFiles:
    PriorFileToProcess.append(PriorFile)

import multiprocessing
Processes = []

#sys.exit()
print "Computing GlobAlbedo inversion WITH Prior information..."
UsePrior = 1

i = 0
# Run until all the threads are done, and there is no BRDF model inversion to process...
while Processes or PriorFileToProcess:

    # if we aren't using all the processors AND there is still inversions left to
    # compute, then spawn another thread
    if (len(Processes) < NumProcesses) and PriorFileToProcess:
        p = multiprocessing.Process(target=ProcessInversion, args=[ data.MData[i], data.VData[i], data.EData[i], data.Mask[i], Lambda, PriorFileToProcess[0], columns, rows, UsePrior, data.DoYClosestSample[i], tile, year, Snow, OutDir])

        p.daemon = True
        p.name = PriorFileToProcess[0]
        p.start()
        Processes.append(p)

        PriorFileToProcess.pop(0) # Delete from list
        i += 1

    # in the case that we have the maximum number of threads check if any of them
    # are done. (also do this when we run out of Priors, until all the threads are done)
    else:
        for process in Processes:
            if not process.is_alive():
                Processes.remove(process)


print time.strftime("Processing finished at: %d/%m/%Y %H:%M:%S")
end = time.time()

print "Total time elapsed = ", (end - start)/3600.0, "hours =",  (end - start)/60.0 , "minutes"
