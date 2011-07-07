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

    for band in range(0,nBands):
        prior[band,:,:] = dataset.GetRasterBand(band+1).ReadAsArray(Xmin, Ymin, xsize, ysize)
        priorSD[band,:,:] = dataset.GetRasterBand(band+1+nBands).ReadAsArray(Xmin, Ymin, xsize, ysize)

        # Scale uncertaities in the prior
        priorSD[band,:,:] = priorSD[band,:,:] * Prior_ScaleFactor

        # Cap the prior uncert to 1.0
        priorSD[band,:,:] = numpy.where(priorSD[band,:,:] > 1.0, 1.0, priorSD[band,:,:])

        #Fill leading diagonal elements of C
        C[band,band,:,:] = priorSD[band,:,:] * priorSD[band,:,:]

    #Open N_samples, which is band 19
    Nsamples = dataset.GetRasterBand(BandCount-1).ReadAsArray(Xmin, Ymin, xsize, ysize)
    #Open mask, which is band 20
    MaskFlag = dataset.GetRasterBand(BandCount).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Calculate C inverse
    for columns in range(0,xsize):
        for rows in range(0,ysize):
            if MaskFlag[columns,rows] >= 1 and MaskFlag[columns,rows] <= 3 and Nsamples[columns,rows] > 0.0 \
               or (MaskFlag[columns,rows] == 15 and Nsamples[columns,rows] > 0.0): # Some pixel may have Mask eq 15 but still valid params
                Mask[columns,rows] = 1.0
                # Calculate C inverse 
                try:
                    C_inv[:,:,columns,rows] = numpy.matrix(C[:,:,columns,rows]).I
                except numpy.linalg.LinAlgError:
                    # For some cases the prior has information in the parameters and SD but not uncert, therefore just assign the max uncert = 1.0
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
        files_path = location + '/200?/' + tile + '/Snow/M_200?*.npy'
    else:
        files_path = location + '/200?/' + tile + '/NoSnow/M_200?*.npy'

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
    if ( 365+(doy-wings)<=366 ):
        DoY_index = numpy.where((DoYs>=366+(doy-wings)) & (Year<year))[0]
        for i in DoY_index:
            allFiles.append(filelist[i])
            allDoYs.append((DoYs[i]-doy-366))

    # Center
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

def Inversion(M_acc, V_acc, Mask_acc, weight, Lambda, M_prior, V_prior, Mask_prior, Parameters_prior, columns, rows, UsePrior, Invalid=-9999):
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


def WriteDataset(File, Inversion, Accumulator, GoodnessOfFit, Tile):

    format = "ENVI"
    driver = gdal.GetDriverByName(format)

    columns = Inversion.parameters.shape[1]
    rows = Inversion.parameters.shape[2]
    number_of_parameters = Inversion.parameters.shape[0]
    number_of_uncertainties = ((number_of_parameters*number_of_parameters)-number_of_parameters)/2 + number_of_parameters
    # 1 extra: NSamples, 2 extra: Entropy, RelativeEntropy, 1 extra: DoYClosestSample, 1 extra: GoodnessOfFit
    number_of_bands = number_of_parameters + number_of_uncertainties + 1 + 2 + 1 + 1

    print "File: ", file
    print "columns: ", columns
    print "rows: ", rows
    print "number_of_bands: ", number_of_bands
    print "number_of_parameters: ", number_of_parameters
    new_dataset = driver.Create( File, columns, rows, number_of_bands , GDT_Float32 )
    print "new dataset: ", new_dataset
    for parameter in range(0, number_of_parameters):
        print "Parameters: ", Inversion.parameters[parameter,:,:]

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
    new_dataset.GetRasterBand(number_of_bands-1).WriteArray(Accumulator.DoYClosestSample[:,:])

    # Goodness of fit
    new_dataset.GetRasterBand(number_of_bands).WriteArray(GoodnessOfFit[:,:])

    new_dataset = None

    # Write header
    ULC_coordinates = GetUpperLeftCoordinates(Tile)
    output_header = open(File.split(".bin")[0] + ".hdr", 'w')
    header_template = open("/home/lsg/GlobAlbedo/src/metadata/generic_header.hdr")
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
    ULC_coords_file = '/home/lsg/GlobAlbedo/src/metadata/Tiles_UpperLeftCorner_Coordinates.txt'
    ULC_coordinates = open(ULC_coords_file, 'r')

    coordinates = ''
    for line in ULC_coordinates:
        if Tile in line:
            coordinates = line.split(',')[1].strip() + ',' + line.split(',')[2].strip()

    return coordinates


def Accumulator(ObsFilesDoY, year, DoY, tile, wings, sensor, weight, Prior, xmin, ymin, xmax, ymax, columns, rows):

    Number_of_Files = len(ObsFilesDoY.allfiles)
    nWaveBands = 3

    #Acumulator matrices
    allM = numpy.zeros((3*nWaveBands,3*nWaveBands,columns, rows), numpy.float32)
    allV = numpy.zeros((3*nWaveBands,columns, rows), numpy.float32)
    allE = numpy.zeros((columns, rows), numpy.float32)
    allMask = numpy.zeros((columns, rows), numpy.float32)

    DoYClosestSample = numpy.zeros((columns, rows), numpy.float32)

    index=0
    for BBDR in ObsFilesDoY.allfiles:

        BBDR_DaysToDoY = ObsFilesDoY.alldoys[ObsFilesDoY.allfiles.index(BBDR)]

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
            continue

        # Some BBDRs have valid reflectances but uncertainties generate a singular matrix, mask those pixels
        for column in range(0,columns):
            for row in range(0,rows):
                if Mask[column,row] > 0:
                    try:
                        tmp_uncert = numpy.matrix(M[:,:,column,row]).I
                        # Store the closest DoY with valid data
                        if ObsFilesDoY.allfiles.index(BBDR) == 0.0 or DoYClosestSample[column,row] == 0.0:
                            DoYClosestSample[column,row] = BBDR_DaysToDoY
                        elif BBDR_DaysToDoY < 0 and DoYClosestSample[column,row] <= 0:
                            # If both days are negative store the greatest
                            DoYClosestSample[column,row] = numpy.max([BBDR_DaysToDoY, DoYClosestSample[column,row]])
                        elif DoYClosestSample[column,row] < 0 and BBDR_DaysToDoY > 0:
                            # If the stored DoYClosest sample os negative and the BBDR_DaysToDoY is positive stored the closest to zero
                            if (DoYClosestSample[column,row] + BBDR_DaysToDoY) <= 0:
                                DoYClosestSample[column,row] = BBDR_DaysToDoY # introduce random to avoid bias
                        elif DoYClosestSample[column,row] > 0 and BBDR_DaysToDoY > 0:
                            # If both days are positive store the smallest
                            DoYClosestSample[column,row] = numpy.min([BBDR_DaysToDoY, DoYClosestSample[column,row]])

                    except numpy.linalg.LinAlgError:   # LinAlgError: Singular matrix
                        M[:,:,column,row] = 0.0
                        V[:,column,row] = 0.0
                        E[column,row] = 0.0
                        Mask[column,row] = 0.0
                        DoYClosestSample[column,row] = 0.0
                        print "Uncertainty information in BBDR is not valid", str(column), str(row)

        # Fill acumulator
        allM += M * weight[index]
        allV += V * weight[index]
        allE += E * weight[index]
        allMask += Mask * weight[index]

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

print time.strftime("Processing starting at: %d/%m/%Y %H:%M:%S")
start = time.time()

tile = sys.argv[1]
DoY = int(sys.argv[2])
year = int(sys.argv[3])
wings = int(sys.argv[4])

if sys.argv[5] == "ALL":
    sensor = "*"
else:
    sensor = sys.argv[5]

Prior_ScaleFactor = float(sys.argv[6])
Snow = int(sys.argv[7])

#DataDir = '/unsafe/GlobAlbedo'
# use this root path for testing on bcserver13:
DataDir = '/data/GlobAlbedo'

print "entering GetObsFiles..."
obsfiles = GetObsFiles(DoY, year, tile, DataDir + '/BBDR/AccumulatorFiles', wings, Snow)
print "back from GetObsFiles."
print "obsfiles: ", obsfiles.allfiles
print "obsdays: ", obsfiles.alldoys

halfLife = 11.54
Lambda = 1.0 # lambda is a reserved word in Python
weight = numpy.exp(-1.0 * abs(obsfiles.alldoys)/halfLife)

xmin = ymin = xmax = ymax = 1
#xmin = 1
#xmax = 100
#ymin = 1
#ymax = 100

Invalid = -9999

#OUTDIR = os.getcwd()
#OUTDIR = '/data/GlobAlbedo/Inversion/$tile'
OUTDIR = '/data/GlobAlbedo/tmp'
if Snow == 1:
    prior_file = DataDir + '/Priors/' + tile + '/background/processed.p1.0.618034.p2.1.00000/Kernels.*' + sys.argv[2] + '*.005.' + tile + '.backGround.Snow'
    OutputFilename = OUTDIR + '/GlobAlbedo.' + str(year) + sys.argv[2] + '.' + tile + '.Snow'
else:
    prior_file = DataDir + '/Priors/' + tile + '/background/processed.p1.0.618034.p2.1.00000/Kernels.*' + sys.argv[2] + '*.005.' + tile + '.backGround.NoSnow'
    OutputFilename = OUTDIR + '/GlobAlbedo.' + str(year) + sys.argv[2] + '.' + tile + '.NoSnow'

#Prior_ScaleFactor = 30.0
print "entering GetPrior..."
prior = GetPrior(prior_file, Prior_ScaleFactor, xmin, ymin, xmax, ymax)
print "back from GetPrior."

Number_of_Files = len(obsfiles.allfiles)
columns = prior.MData.shape[2]
rows =  prior.MData.shape[3]

data = Accumulator(obsfiles, year, DoY, tile, wings, sensor, weight, prior, xmin, ymin, xmax, ymax, columns, rows)

reading_time = time.time()
print "Reading/formating BBDRs time elapsed = ", (reading_time - start)/3600.0, "hours =",  (reading_time - start)/60.0 , "minutes"

#---------------------
# Inversion WITH Prior
#---------------------
print "Computing GlobAlbedo inversion WITH Prior information..."
UsePrior = 1
GlobAlbedoInversion = Inversion(data.MData, data.VData, data.Mask, weight, Lambda, prior.MData, prior.VData, prior.Mask, prior.Parameters, columns, rows, UsePrior)
GoodnessOfFit = GetGoodnessOfFit(data.MData, data.VData, data.EData, GlobAlbedoInversion.parameters, GlobAlbedoInversion.Mask, columns, rows)

#------------------------
# Inversion WITHOUT Prior
#------------------------
print "Computing GlobAlbedo inversion WITHOUT Prior information..."
UsePrior = 0
GlobAlbedoInversionNoPrior = Inversion(data.MData, data.VData, data.Mask, weight, Lambda, prior.MData, prior.VData, prior.Mask, prior.Parameters, columns, rows, UsePrior)
GoodnessOfFitNoPrior = GetGoodnessOfFit(data.MData, data.VData, data.EData, GlobAlbedoInversionNoPrior.parameters, GlobAlbedoInversionNoPrior.Mask, columns, rows)


print "Saving results to ENVI file..."
#OUTDIR = os.getcwd()
WriteDataset(OutputFilename + '.bin', GlobAlbedoInversion, data, GoodnessOfFit, tile)
WriteDataset(OutputFilename + '.NoPrior.bin', GlobAlbedoInversionNoPrior, data, GoodnessOfFitNoPrior, tile)

print time.strftime("Processing finished at: %d/%m/%Y %H:%M:%S")
end = time.time()

print "Total time elapsed = ", (end - start)/3600.0, "hours =",  (end - start)/60.0 , "minutes"
