#!/opt/epd-5.1.0-rh5-x86_64/bin/python

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

def GetReflectancesForwardedModel(kernels, Prior):
    '''
    Get predicted reflectances based on model:
    refl = f0 + f1*Kvol + f2+KGeo
        Where:
            KVol = Ross-Thick (volumetric)
            KGeo = Li-Sparse (geometric)
    '''

    nWaveBands = 3
    parameters = Prior.Parameters
    parameters_SD = Prior.Parameters_SD

    # Get predicted reflectances
    VIS = parameters[0,:,:] + (parameters[1,:,:] * kernels[1,:,:] ) + (parameters[2,:,:] * kernels[2,:,:])
    NIR = parameters[3,:,:] + (parameters[4,:,:] * kernels[4,:,:] ) + (parameters[5,:,:] * kernels[5,:,:])
    SW =  parameters[6,:,:] + (parameters[7,:,:] * kernels[7,:,:] ) + (parameters[8,:,:] * kernels[8,:,:])

    # Get predicted reflectances SD
    VIS_SD = parameters_SD[0,:,:] + (parameters_SD[1,:,:] * kernels[1,:,:] ) + (parameters_SD[2,:,:] * kernels[2,:,:])
    NIR_SD = parameters_SD[3,:,:] + (parameters_SD[4,:,:] * kernels[4,:,:] ) + (parameters_SD[5,:,:] * kernels[5,:,:])
    SW_SD =  parameters_SD[6,:,:] + (parameters_SD[7,:,:] * kernels[7,:,:] ) + (parameters_SD[8,:,:] * kernels[8,:,:])

    PredictedReflectances = numpy.zeros((nWaveBands, parameters.shape[1], parameters.shape[2]), numpy.float32)
    PredictedReflectances_SD = numpy.zeros((nWaveBands, parameters.shape[1], parameters.shape[2]), numpy.float32)

    PredictedReflectances[0,:,:] = VIS
    PredictedReflectances[1,:,:] = NIR
    PredictedReflectances[2,:,:] = SW

    PredictedReflectances_SD[0,:,:] = VIS_SD
    PredictedReflectances_SD[1,:,:] = NIR_SD
    PredictedReflectances_SD[2,:,:] = SW_SD

    return ReturnReflectancesForwardedModel(PredictedReflectances, PredictedReflectances_SD)

class ReturnReflectancesForwardedModel(object):
    def __init__(self, PredictedReflectances, PredictedReflectances_SD):
        self.PredictedReflectances = PredictedReflectances
        self.PredictedReflectances_SD = PredictedReflectances_SD


def GetKernelsSum(Directory, xmin=1, ymin=1, xmax=1, ymax=1):
    '''
    Get sum of Geo/Vol Kernels including the Nsky term - 4 files
    '''

    import sys

    try:
        dataset = gdal.Open( Directory + '/Kvol_BRDF_VIS_1.img', GA_ReadOnly )
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

    # Close dataset
    dataset = None

    nWaveBands = 3
    NumberOfKernels = 3 * nWaveBands

    RossThick = ['Kvol_BRDF_VIS_','Kvol_BRDF_NIR_','Kvol_BRDF_SW_']
    LiSparse = ['Kgeo_BRDF_VIS_','Kgeo_BRDF_NIR_','Kgeo_BRDF_SW_']
    kernels = numpy.zeros((NumberOfKernels, xsize, ysize), numpy.float32)
    # Isotropic kernles = 1
    kernels[0,:,:] = kernels[3,:,:] = kernels[6,:,:] = 1.0

    for NumberOfKernelElements in range(1,5):
        dataset = gdal.Open( Directory + '/' + RossThick[0] + str(NumberOfKernelElements) + '.img', GA_ReadOnly )    # Kvol_BRDF_VIS.img
        kernels[1,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize) + kernels[1,:,:]

        dataset = gdal.Open( Directory + '/' + RossThick[1] + str(NumberOfKernelElements) + '.img', GA_ReadOnly )    # Kvol_BRDF_NIR.img
        kernels[4,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize) + kernels[4,:,:]

        dataset = gdal.Open( Directory + '/' + RossThick[2] + str(NumberOfKernelElements) + '.img', GA_ReadOnly )    # Kvol_BRDF_SW.img
        kernels[7,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize) + kernels[7,:,:]

        dataset = gdal.Open( Directory + '/' + LiSparse[0] + str(NumberOfKernelElements) + '.img', GA_ReadOnly )     # Kgeo_BRDF_VIS.img
        kernels[2,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize) + kernels[2,:,:]

        dataset = gdal.Open( Directory + '/' + LiSparse[1] + str(NumberOfKernelElements) + '.img', GA_ReadOnly )     # Kgeo_BRDF_NIR.img
        kernels[5,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize) + kernels[5,:,:]

        dataset = gdal.Open( Directory + '/' + LiSparse[2] + str(NumberOfKernelElements) + '.img', GA_ReadOnly )     # Kgeo_BRDF_SW.img
        kernels[8,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize) + kernels[8,:,:]

    return kernels


def Get_BBDR(Directory, Prior, xmin=1, ymin=1, xmax=1, ymax=1, verbose=0):
    '''
    AATSR/MERIS/VGT BBDR are in BEAM-DIMAP format with ENVI headers, which means data is in single files.
    '''
    import sys

    try:
        dataset = gdal.Open( Directory + '/BB_VIS.img', GA_ReadOnly )
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

    # Close dataset
    dataset = None

    nWaveBands = 3
    WaveBands = ['BB_VIS.img','BB_NIR.img','BB_SW.img']

    # ----------
    # Load BBDRs
    # ----------
    BBDR = numpy.zeros((nWaveBands, xsize, ysize), numpy.float32)
    Mask = numpy.ones((xsize, ysize), numpy.int16)

    print "=========================================================================================="
    print Directory , 'loading BBDR file...'
    for Broadband in range(0,nWaveBands):
        dataset = gdal.Open( Directory + '/' + WaveBands[Broadband], GA_ReadOnly )
        BBDR[Broadband,:,:] = dataset.GetRasterBand(1).ReadAsArray(Xmin, Ymin, xsize, ysize)
        # Mask BBDR data if 0 or -9999.0
        Mask[:,:] = numpy.where( (BBDR[Broadband,:,:]==0) | (BBDR[Broadband,:,:]==-9999), 0, 1)
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
    Mask = numpy.where( SnowMask==1, 0.0, Mask )

    #----------------------------------------------------------------
    # Load Kernels, Ross-Thick (volumetric) and Li-Sparse (geometric)
    #----------------------------------------------------------------
    kernels = GetKernelsSum(Directory, xmin, ymin, xmax, ymax)

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

    #--------------------------------
    # Oulier detection based on prior
    #--------------------------------
    PredictedRefl = GetReflectancesForwardedModel(kernels, Prior)
    Outliers = (BBDR - PredictedRefl.PredictedReflectances) / PredictedRefl.PredictedReflectances_SD
    OutliersMask = numpy.ones((xsize, ysize), numpy.byte)

    for Broadband in range(0,nWaveBands):
        OutlierMask = numpy.where(Outliers[Broadband,:,:] > 1.0, 0, 1) * OutliersMask

    # Update BBDR Mask
    Mask = Mask * OutlierMask

    #-------------------------------------
    # Build C -- coveriance matrix for obs
    #-------------------------------------
    # C in a symetric matrix form of nWaveBands*nWaveBands
    C = numpy.zeros((nWaveBands*nWaveBands, xsize, ysize), numpy.float32)
    thisC = numpy.zeros((nWaveBands,nWaveBands,xsize, ysize), numpy.float32)
    invC = numpy.zeros((nWaveBands,nWaveBands,xsize, ysize), numpy.float32)

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
    M = numpy.zeros((3*nWaveBands,3*nWaveBands, xsize, ysize), numpy.float32)
    V = numpy.zeros((3*nWaveBands, xsize, ysize), numpy.float32)

    for columns in range(0,xsize):
        for rows in range(0,ysize):
            if Mask[columns,rows] <> 0:
                invC = numpy.matrix(thisC[:,:,columns,rows]).I

                for j1 in range(0,nWaveBands):
                    for j2 in range(0,nWaveBands):
                        for k1 in range(0,nWaveBands):
                            for k2 in range(0,nWaveBands):
                                M[j1*3+k1,j2*3+k2,columns,rows] = kernels[j1*3+k1,columns,rows] * invC[j1,j2] * kernels[j2*3+k2,columns,rows]

                for j1 in range(0,nWaveBands):
                    for k1 in range(0,nWaveBands):
                        V[j1*3+k1,columns,rows] = kernels[j1*3+k1,columns,rows] * invC[j1,j1] * BBDR[j1,columns,rows]

    if verbose==1:
        numpy.set_printoptions(linewidth=160)

        print "BBDRs: VIS, NIR, SW", BBDR.shape
        print numpy.matrix(BBDR), "\n"

        print "BBDRs SD", SD.shape
        print numpy.matrix(SD), "\n"

        print "Kernels: iso, vol, geo", kernels.shape
        print numpy.matrix(kernels), "\n"

        print "C", thisC.shape
        print numpy.matrix(thisC), "\n"

        print "C^-1", invC.shape
        print numpy.matrix(invC), "\n"

        print "M = K^T C^-1 K", M.shape
        print numpy.matrix(M), "\n"

        print "V = K^T C^-1 BBDR", V.shape
        print numpy.matrix(V), "\n"

        print "Mask", Mask.shape
        print numpy.matrix(Mask), "\n"


    return ReturnValueGet_BBDR(BBDR, kernels, SD, correlation, thisC, invC, M, V, Mask, OutliersMask)

class ReturnValueGet_BBDR(object):
    def __init__(self, BBDR, kernels, SD, correlation, C, invC, M, V, mask, OutliersMask):
        self.BBDR = BBDR
        self.kernels = kernels
        self.SD = SD
        self.correlation = correlation
        self.C = C
        self.invC = invC
        self.M = M
        self.V = V
        self.mask = mask
        self.OutliersMask = OutliersMask


def GetPrior(File, Prior_ScaleFactor=10.0, xmin=1, ymin=1, xmax=1, ymax=1, verbose=0):
    import os
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

    print "Opening prior:", os.path.basename(filename[0]), "with scale factor:", str(Prior_ScaleFactor)

    # The number of data channels is nBands (mean) + nBands (sd) + N samples + Mask
    # which should be 20 usually, giving nBands = (nb-2)/2
    nBands = (BandCount-2)/2

    #Create M and V
    nWaveBands = 3
    C = numpy.zeros((3*nWaveBands,3*nWaveBands, xsize, ysize), numpy.float32)
    C_inv = numpy.zeros((3*nWaveBands,3*nWaveBands, xsize, ysize), numpy.float32)
    C_inv_F = numpy.zeros((3*nWaveBands, xsize, ysize), numpy.float32) # Matrix to store C^-1 * Fpr
    Mask = numpy.zeros((xsize, ysize), numpy.byte)

    prior = numpy.zeros((nBands, xsize, ysize), numpy.float32)
    priorSD = numpy.zeros((nBands, xsize, ysize), numpy.float32)
    for band in range(0,nBands):
        prior[band,:,:] = dataset.GetRasterBand(band+1).ReadAsArray(Xmin, Ymin, xsize, ysize)
        priorSD[band,:,:] = dataset.GetRasterBand(band+1+nBands).ReadAsArray(Xmin, Ymin, xsize, ysize)

        # Scale uncertaities in the prior
        priorSD[band,:,:] = priorSD[band,:,:] * Prior_ScaleFactor
        #w = numpy.where(priorSD > 1.0)
        #priorSD[w] = 1.0

        #Fill leading diagonal elements of C
        #C[band,band,:,:] = priorSD[band,:,:]
        C[band,band,:,:] = priorSD[band,:,:] * priorSD[band,:,:]

    #Open N_samples, which is band 19
    Nsamples = dataset.GetRasterBand(BandCount-1).ReadAsArray(Xmin, Ymin, xsize, ysize)
    #Open mask, which is band 20
    MaskFlag = dataset.GetRasterBand(BandCount).ReadAsArray(Xmin, Ymin, xsize, ysize)

    # Calculate C inverse
    for columns in range(0,xsize):
        for rows in range(0,ysize):
            if MaskFlag[columns,rows] >= 1 and MaskFlag[columns,rows] <= 3 and Nsamples[columns,rows] > 1.0:
                Mask[columns,rows] = 1.0
                # Calculate C inverse 
                try:
                    C_inv[:,:,columns,rows] = numpy.matrix(C[:,:,columns,rows]).I
                except numpy.linalg.LinAlgError:
                    #print columns,rows
                    #print priorSD[:,columns,rows]
                    # Uncertainty information in C generates is a singular matrix, do not process pixel
                    Mask[columns,rows] = 0.0
                    continue

                for i in range(0,nBands):
                    # Compute C^-1  * Fpr
                    C_inv_F[i,columns,rows] = C_inv[i,i,columns,rows] * prior[i,columns,rows]

    if verbose==1:
        numpy.set_printoptions(linewidth=150)

        print "Prior parameters", prior.shape
        print numpy.matrix(prior), "\n"

        print "Prior parameters SD", priorSD.shape
        print numpy.matrix(priorSD), "\n"

        print "C", C.shape
        print numpy.matrix(C), "\n"

        print "M = C^-1", C_inv.shape
        print numpy.matrix(C_inv), "\n"

        print "V = C^-1 * Prior_parameters", C_inv_F.shape
        print numpy.matrix(C_inv_F), "\n"

        print "Mask", Mask.shape
        print numpy.matrix(Mask), "\n"

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
    import numpy

    file_list_lenght = len(filelist)

    DoY = numpy.zeros(file_list_lenght, numpy.int16)
    for i in range(0, file_list_lenght):
        # Extract DoY, e.g. 294, from the string:
        # /data/raid7/2010/ucasglo/MODIS/MOD09GA/2004/h18v04/BB/MOD09GA.A2004294.h18v04.005.2008255064726.BB.bin
        #DoY[i] = filelist[i].split('/')[10].split('.')[1][5:8]
        DoY[i] = filelist[i].split('/')[9].split('_')[0][4:7]

    return DoY

def GetObsFiles(doy, year, tile, location, wings):

    import sys
    import glob

    allFiles = []
    allDoYs = []
    count = 0
    # Actually, the 'MODIS' doy is 8 days behind the period centre
    # so add 8 onto doy, wehere we understand DoY to mean MODIS form DoY
    doy += 8

    for y in [-1,0,1]:
        files_path = location + '/' + str(year) + '/' + tile + '/' + str(year) + '*'
        #Get all files in the directory
        filelist = glob.glob(files_path)
        filelist.sort()

        DoYs = GetDaysOfYear(filelist) - doy + y * 365

        #indices = numpy.where((DoYs >= -wings) & (DoYs <= wings))[0]
        indices = numpy.where((DoYs >= -wings) & (DoYs < wings))[0]

        for i in indices:
            allFiles.append(filelist[i])
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

def Inversion(M_acc, V_acc, Mask_acc, weight, Lambda, M_prior, V_prior, Mask_prior, Parameters_prior, columns, rows, verbose=0,use_prior=1,INVALID=-999):
    from  scipy.linalg import lstsq
    from  scipy.linalg import svd

    nWaveBands = 3

    parameters = numpy.zeros((3*nWaveBands, columns, rows), numpy.float32)
    det = numpy.zeros((columns, rows), numpy.float32)
    relativeentropy = numpy.zeros((columns, rows), numpy.float32)
    uncertainties = numpy.zeros((3*nWaveBands,3*nWaveBands, columns, rows), numpy.float32)

    for column in range(0,columns):
        for row in range(0,rows):

            if Mask_acc[column,row] > 0 and Mask_prior[column,row] > 0:

                M = M_acc[:,:,column,row]
                V = V_acc[:,column,row]

                M_p = M_prior[:,:,column,row]
                V_p = V_prior[:,column,row]

                if verbose==1:
                    print "========= Inversion ========="
                    print "M", M.shape
                    print numpy.matrix(M), "\n"

                    print "V", V.shape
                    print numpy.matrix(V), "\n"

                # Include prior informatio in M & V
                if use_prior == 1:
		    for i in range(0,nWaveBands*3):
                    	M[i,i] += M_p[i,i]

                    V += V_p

	        tmp = numpy.matrix(M).I
                w1 = numpy.where(numpy.isnan(tmp))[0].shape[1]
	        w2 = numpy.where(numpy.diagonal(tmp) < 0)[0].shape[0]
		if w1 > 0  or w2 > 0:
                    tmp[:,:] = INVALID
                uncertainties[:,:,column,row] = tmp

		# Compute least-squares solution to equation Ax = b
                # http://docs.scipy.org/doc/scipy/reference/generated/scipy.linalg.lstsq.html

                (P, rho_residuals, rank, svals) = lstsq( M, V, overwrite_a=True)
                parameters[:,column,row] = P

                if verbose==1:
                    print "Include prior information in M & V"
                    print "M", M.shape
                    print numpy.matrix(M), "\n"

                    print "V", V.shape
                    print numpy.matrix(V), "\n"

                    print "Uncertainties", uncertainties.shape
                    print "M^-1", numpy.matrix(uncertainties[:,:,column,row])
		    print "posterior diagonal sd Uncertainties"
		    if (uncertainties[0,0,column,row] > 0 ):
		        print numpy.sqrt(numpy.diagonal(uncertainties[:,:,column,row]))
		    else:
			print "NaN"
		    print "prior     diagonal sd Uncertainties"
		    print numpy.sqrt(numpy.diagonal(numpy.matrix(M_p).I))

                    print "Parameters", parameters.shape
                    print numpy.matrix(parameters)
                    print "Prior Parameters", parameters.shape
                    print numpy.matrix(Parameters_prior[:,column,row])

                # Compute singluar value decomposition
                # http://docs.scipy.org/doc/scipy/reference/generated/scipy.linalg.svd.html
                U, S, Vh = svd(M)
		det[column,row] = 0.5*numpy.log(numpy.product(1/S)) + S.shape[0] * numpy.sqrt(numpy.log(2*numpy.pi*numpy.e))
                # this can be calculated earlier for the prior
                U, S, Vh = svd(numpy.matrix(M_p))
		priordet = 0.5*numpy.log(numpy.product(1/S)) + S.shape[0] * numpy.sqrt(numpy.log(2*numpy.pi*numpy.e))
                if use_prior == 1:
		    relativeentropy[column,row] = priordet - det[column,row]
	        else:
		    relativeentropy[column,row] = INVALID # as this has no meaning
		if verbose==1:
		    print "entropy          : prior",priordet
		    print "entropy          : this ",det[column,row]
                    print "relative entropy :      ",relativeentropy[column,row]
		    if use_prior == 1:
			print "i.e. improvement in uncertainty  of ",numpy.exp(relativeentropy[column,row]/S.shape[0])
            else:
                # If there is not a single sample available, just use the prior parameters (f0, f1, f2) and prior uncertainties
                if Mask_prior[column,row] > 0:
                    for i in range(0,nWaveBands*3):
                        parameters[i,column,row] = Parameters_prior[i,column,row]
		    if use_prior == 1:
                        uncertainties[:,:,column,row] = numpy.matrix(M_prior[:,:,column,row]).I
		        U, S, Vh = svd(numpy.matrix(M_prior[:,:,column,row]))
		        priordet = 0.5*numpy.log(numpy.product(1/S)) + S.shape[0] * numpy.sqrt(numpy.log(2*numpy.pi*numpy.e))
	                det[column,row] = priordet
 		        relativeentropy[column,row] = 0.0
 		    else:
			uncertainties[:,:,column,row] = INVALID # as this has no meaning
			det[column,row] = INVALID # as this has no meaning
			relativeentropy[column,row] = INVALID # as this has no meaning
		        # a flag should be passed through to say that it is prior
		    if verbose==1:
                        print "entropy          : prior",priordet
                        print "entropy          : this ",priordet
                        print "relative entropy :      ",relativeentropy[column,row]

    # Make all parameters (f0, f1, f2) = 0 if any parameter is le 0 OR ge 1.0
    #indices = numpy.where((parameters <= 0.0) | (parameters >= 1.0))
    #parameters[:, indices[1], indices[2]] = 0.0
    #uncertainties[:,:, indices[1], indices[2]] = 0.0

    return ReturnValuesInversion(parameters, det, relativeentropy, uncertainties)

class ReturnValuesInversion(object):
    def __init__(self, parameters, det, relativeentropy, uncertainties):
        self.parameters = parameters
        self.entropy = det
	self.relativeentropy = relativeentropy
        self.uncertainties = uncertainties

def WriteDataset(File, Inversion, Accumulator):

    format = "ENVI"
    driver = gdal.GetDriverByName(format)

    columns = Inversion.parameters.shape[1]
    rows = Inversion.parameters.shape[2]
    number_of_parameters = Inversion.parameters.shape[0]
    number_of_uncertainties = ((number_of_parameters*number_of_parameters)-number_of_parameters)/2 + number_of_parameters
    number_of_bands = number_of_parameters + number_of_uncertainties + 1 + 2 # 2 extra: entropy, relative entropy

    # The MCD43 "emulator" output file will have only 9 bands, 3 params * 3 broadbands
    #number_of_bands = 9 + 1

    new_dataset = driver.Create( File, columns, rows, number_of_bands , GDT_Float32 )

    # Write parameters
    for parameter in range(0, number_of_parameters):
        new_dataset.GetRasterBand(parameter+1).WriteArray(Inversion.parameters[parameter,:,:])

    # Write uncertainties
    # If uncertainties are computed delete the following comments
    count = 0
    for i in range(0, number_of_parameters):
        for j in range(i, number_of_parameters):
            new_dataset.GetRasterBand(number_of_parameters+count+1).WriteArray(Inversion.uncertainties[i,j,:,:])
            count += 1

    # Number of observations
    new_dataset.GetRasterBand(number_of_bands-2).WriteArray(Accumulator.Mask[:,:])

    # entropy
    new_dataset.GetRasterBand(number_of_bands-1).WriteArray(Inversion.entropy[:,:])

    # relative entropy
    new_dataset.GetRasterBand(number_of_bands-0).WriteArray(Inversion.relativeentropy[:,:])
    
    new_dataset = None


    # Write header
    output_header = open(File.split(".bin")[0] + ".hdr", 'w')
    header_template = open("/home/ucasglo/AlbedoInversion/src/metadata/lewis_header.hdr")
    for lines in header_template:
        output_header.write(lines.replace('SAMPLES,LINES', 'samples = ' + str(columns) + '\n' + 'lines = ' + str(rows)))

    output_header.close()
    header_template.close()


def GetAccumulatorFileName(year, DoY, tile, wings):
    AccumulatorBasename = '_tp.' + tile + '.' + str(year) + '.' + str(DoY) + '.wings' + str(wings) + '.npy'
    M_accumulator_filename = 'M' + AccumulatorBasename
    V_accumulator_filename = 'V' + AccumulatorBasename
    mask_accumulator_filename = 'mask' + AccumulatorBasename

    return ReturnGetAccumulatorFileName(M_accumulator_filename, V_accumulator_filename, mask_accumulator_filename)

class ReturnGetAccumulatorFileName(object):
    def __init__(self, M_accumulator_filename, V_accumulator_filename, mask_accumulator_filename):
        self.M_accumulator_filename = M_accumulator_filename
        self.V_accumulator_filename = V_accumulator_filename
        self.mask_accumulator_filename = mask_accumulator_filename

def Accumulator(ObsFilesDoY, year, DoY, tile, wings, sensor, weight, Prior, xmin, ymin, xmax, ymax, columns, rows, verbose=0):
    import os

    Number_of_Files = len(ObsFilesDoY.allfiles)
    nWaveBands = 3

    #Acumulator matrices
    allM = numpy.zeros((3*nWaveBands,3*nWaveBands,columns, rows), numpy.float32)
    allV = numpy.zeros((3*nWaveBands,columns, rows), numpy.float32)
    allMask = numpy.zeros((columns, rows), numpy.byte)

    AccumulatorFileName = GetAccumulatorFileName(year, DoY-wings, tile, wings)
    index=0
    for BBDR in ObsFilesDoY.allfiles:

        M = numpy.zeros((3*nWaveBands,3*nWaveBands, columns, rows), numpy.float32)
        V = numpy.zeros((3*nWaveBands, columns, rows), numpy.float32)
        Mask = numpy.zeros((columns, rows), numpy.byte)

        #First verify whether or not accumulator exists for the specific BBDR
        if os.path.isfile('./M_' + os.path.basename(BBDR) + '.npy') and \
           os.path.isfile('./V_' + os.path.basename(BBDR) + '.npy') and \
           os.path.isfile('./mask_' + os.path.basename(BBDR) + '.npy'):

            #As the accumulator exists, then get the M, V and mask from the NumPy binary format
            print "Load M, V and mask for", BBDR, "data from accumulator..."

            M[:,:,:,:] = numpy.load('./M_' + os.path.basename(BBDR) + '.npy')
            V[:,:,:] = numpy.load('./V_' + os.path.basename(BBDR) + '.npy')
            Mask[:,:] = numpy.load('./mask_' + os.path.basename(BBDR) + '.npy')

        else:
            #Load data from BBDR
            bbdr = Get_BBDR(BBDR, Prior, xmin, ymin, xmax, ymax, verbose)

            M[:,:,:,:] = bbdr.M 
            V[:,:,:] = bbdr.V
            Mask[:,:] = bbdr.mask

            WriteAccumulatorFlag = 1
            if WriteAccumulatorFlag == 1:
                WriteAccumulator(os.path.basename(BBDR), M, V, Mask, bbdr.OutliersMask)

        # Fill acumulator
        print "========= Accumulator ========="
        print "Accumulator M", allM.shape
        print "Weight:", weight[index]
        print numpy.matrix(allM), "\n"

        print "Accumulator V", allV.shape
        print "Weight:", weight[index]
        print numpy.matrix(allV), "\n"

        allM += M * weight[index]
        allV += V * weight[index]
        allMask = allMask + Mask

        index += 1

    return ReturnData(allM, allV, allMask)


class ReturnData(object):
    def __init__(self, MData, VData, Mask):
        self.MData = MData
        self.VData = VData
        self.Mask = Mask


def WriteAccumulator(Accumulator_filename, M, V, Mask, OutliersMask):
    print "Saving accumulator files to NumPy binary format..."
    numpy.save('./M_' + Accumulator_filename, M)
    numpy.save('./V_' + Accumulator_filename, V)
    numpy.save('./mask_' + Accumulator_filename, Mask)
    numpy.save('./OutliersMask_' + Accumulator_filename, Mask)


#------------------------------------------------------------------------------------------#
# OD: This is obviously the 'main' routime...

import sys
import os
import time

print time.strftime("Processing starting at: %d/%m/%Y %H:%M:%S")
start = time.time()

tile = sys.argv[1]
DoY = int(sys.argv[2])
year = int(sys.argv[3])
wings = int(sys.argv[4])

#          GetObsFiles(doy, year, tile, location, wings)
if sys.argv[5] == "ALL":
    sensor = "*"
else:
    sensor = sys.argv[5]

use_prior = int(sys.argv[6])
Prior_ScaleFactor = float(sys.argv[7])

obsfiles = GetObsFiles(DoY, year, tile,'/data/geospatial_20/ucasglo/GlobAlbedo/BBDR/' + sensor, wings)
halfLife = 11.54
Lambda = 1.0 # lambda is a reserved word in Python
weight = numpy.exp(-1.0 * abs(obsfiles.alldoys)/halfLife)

#metadata = GetMetadata(obsfiles.allfiles[0])

xmin = ymin = xmax = ymax = 1
xmin = 400
xmax = 400
ymin = 400
ymax = 400

verbose = 1
INVALID = -9999

prior_file = '/data/geospatial_20/ucasglo/GlobAlbedo/Priors/' + tile + '/background/processed.p1.0.618034.p2.1.00000/Kernels.*' + str(DoY) + '*.005.' + tile + '.backGround.NoSnow'
#Prior_ScaleFactor = 30.0
prior = GetPrior(prior_file, Prior_ScaleFactor, xmin, ymin, xmax, ymax, verbose)

#GetBBDR return BBDR, kernels, SD, correlation, mask
Number_of_Files = len(obsfiles.allfiles)
columns = prior.MData.shape[2]
rows =  prior.MData.shape[3]

data = Accumulator(obsfiles, year, DoY, tile, wings, sensor, weight, prior, xmin, ymin, xmax, ymax, columns, rows, verbose)

reading_time = time.time()
print "Reading/formating BBDRs time elapsed = ", (reading_time - start)/3600.0, "hours =",  (reading_time - start)/60.0 , "minutes"

#--------------------------------------------
print "Computing GlobAlbedo inversion (temporal weighting)..."
GlobAlbedo_Inversion = Inversion(data.MData, data.VData, data.Mask, weight, Lambda, prior.MData, prior.VData, prior.Mask, prior.Parameters, columns, rows, verbose,use_prior=use_prior,INVALID=INVALID)

print "Saving results to a file..."
OUTDIR = os.getcwd()
WriteDataset(OUTDIR + '/opimage.' + str(DoY) + '.' + str(year) + '.wings' + str(wings) + '.' + str(columns) + '_' + str(rows) + '.GlobAlbedo_prior.bin', GlobAlbedo_Inversion, data)

print time.strftime("Processing finished at: %d/%m/%Y %H:%M:%S")
end = time.time()

print "Total time elapsed = ", (end - start)/3600.0, "hours =",  (end - start)/60.0 , "minutes"
