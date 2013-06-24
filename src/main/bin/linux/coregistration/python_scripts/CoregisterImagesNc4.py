'''
Created on Feb 20, 2013

@author: danielfisher

'''
#import matplotlib.pyplot as plt
import numpy as np
import os
from PIL import Image
import scipy.linalg as linalg
#import scipy.interpolate as interp
import sys
import subprocess

from netCDF4 import Dataset
#from Scientific.IO.NetCDF import NetCDFFile

def convert_to_grayscale(image):
    if np.min(image == 0) and np.max(image == 255):
        print 'image already grayscale'
        return image
    else:
        minPixelValue = np.min(image) * 1.0
        maxPixelValue = np.max(image) * 1.0
        imageGrayScale = ((image + minPixelValue)/maxPixelValue)*255
        return imageGrayScale

def process_image(image, resultname):
    """ Process an image and save the results in a file. """
    
    #covert image to 8bit and save in .pgm format 
    image = Image.fromarray(np.uint8(image))
    image.save('tmp.pgm')
    imagename = 'tmp.pgm'

    #call sift
#    p = subprocess.call(['/Users/danielfisher/usr/src/vlfeat-0.9.16/bin/maci64/sift ' + imagename + ' --output='+resultname + ' ' + params], shell=True)

    p = subprocess.call(['/home/globalbedo/od/globalbedo/seaice/coregistered/vlfeat-0.9.16/bin/glnxa64/sift ' + \
                         imagename + ' --output='+resultname + ' --edge-thresh 5 --peak-thresh 2 --octaves 5'], shell=True)

#    siftCommand = ["C:/Users/olafd/globalbedo_CCN/vlfeat-0.9.16/bin/win64/sift.exe", imagename, \
#                   "--output", resultname, \
#                   "--edge-thresh", "5", \
#                   "--peak-thresh", "2", \
#                   "--octaves", "5"]
#    p = subprocess.call(siftCommand)

    return p

def read_features_from_file(filename):
    """ Read feature properties and return in matrix form. """
    f = np.loadtxt(filename)
    return f[:,:4],f[:,4:] # feature locations, descriptors
    
def write_features_to_file(filename,locs,desc):
    """ Save feature location and descriptor to file. """
    np.savetxt(filename,np.hstack((locs,desc)))
    
def ice_mask(l1, d1, aatsrDS):
    '''Snow/Ice mask as defined in Istomina 2011
    '''     
    #read in aatsr bands and set to float
    n12 = aatsrDS.variables['btemp_nadir_1200'][:,:].astype('float')
    n11 = aatsrDS.variables['btemp_nadir_1100'][:,:].astype('float')
    n37 = aatsrDS.variables['btemp_nadir_0370'][:,:].astype('float')
    n16 = aatsrDS.variables['reflec_nadir_1600'][:,:].astype('float')
    n87 = aatsrDS.variables['reflec_nadir_0870'][:,:].astype('float')
    n67 = aatsrDS.variables['reflec_nadir_0670'][:,:].astype('float')
    n55 = aatsrDS.variables['reflec_nadir_0550'][:,:].astype('float')
    
    #get the zeros and null values in the divisors
    n37ZeroInd = n37 < 0
    n87ZeroInd = n87 < 0
    n67ZeroInd = n67 < 0
        
    #set this indexes to 1 to prevent divide by zero
    n37[n37ZeroInd] = 1
    n87[n87ZeroInd] = 1
    n67[n67ZeroInd] = 1
        
    #less than 0.03 is cloud free
    mask1 = (np.abs((n37-n11)/n37)) < 0.03
    mask2 = (np.abs((n37-n12)/n37)) < 0.03
                   
    #greater than 0.8 is cloud free
    mask3 = ((n87 - n16)/ n87) > 0.8
                
    #less than 0.1 is cloud free
    mask4 = ((n87 - n67)/ n87) < 0.1
                
    #less than 0.4 cloud free
    mask5 = np.abs((n67-n55)/n67) < 0.4
        
    #set the zeros in the divisors to 0 indicating no cloud
    mask1[n37ZeroInd] = 0
    mask2[n37ZeroInd] = 0
    mask3[n87ZeroInd] = 0
    mask4[n87ZeroInd] = 0
    mask5[n67ZeroInd] = 0
        
    mask = mask1 & mask2 & mask3 & mask4 & mask5
    
    #apply mask
    if l1 != None:
        index = mask[l1[:,1].astype('int'), l1[:,0].astype('int')]
        goodIndex = index==1
        l1 = l1[goodIndex,:]
        d1 = d1[goodIndex,:]
        return l1, d1, mask
    else:
        return None, None, mask
              
def match(desc1,desc2):
    """ For each descriptor in the first image,
    select its match in the second image.
    input: desc1 (descriptors for the first image),
    desc2 (same for second image). """
    desc1 = np.array([d/linalg.norm(d) for d in desc1])
    desc2 = np.array([d/linalg.norm(d) for d in desc2])
    
    dist_ratio = 0.6
    desc1_size = desc1.shape
    
    matchscores = np.zeros((desc1_size[0],1),'int')
    desc2t = desc2.T # precompute matrix transpose
    for i in range(desc1_size[0]):
        dotprods = np.dot(desc1[i,:],desc2t) # vector of dot products
        dotprods = 0.9999*dotprods
        # inverse cosine and sort, return index for features in second image
        indx = np.argsort(np.arccos(dotprods))
        
        # check if nearest neighbor has angle less than dist_ratio times 2nd
        if np.arccos(dotprods)[indx[0]] < dist_ratio * np.arccos(dotprods)[indx[1]]:
            matchscores[i] = int(indx[0])
    return matchscores

def match_twosided(desc1,desc2):
    """ Two-sided symmetric version of match(). """
    
    matches_12 = match(desc1,desc2)
    matches_21 = match(desc2,desc1)
    
    ndx_12 = matches_12.nonzero()[0]
    
    # remove matches that are not symmetric
    for n in ndx_12:
        if matches_21[int(matches_12[n])] != n:
            matches_12[n] = 0

    return matches_12

def get_warp(locs1, locs2):
    
    #init warpRMSE
    bestWarpRMSE = 99999
    
    #intialise bucketsize at 0
    bucketCount = 4
    
    #find suitable bucket size
    nTrials = 100
    while ((nTrials >= 100) | (nTrials < 0)): 
        
        bucketSize = 2 ** bucketCount
        bucketCount+=1
    
        if bucketSize == 512:
            return -1
        
        #divide the image up into buckets to aid tie point selection
        nBucketsY, nBucketsX, sampledYBuckets, sampledXBuckets, sampledBuckets = buckets(locs1, bucketSize)
    
        #determine the number of trials to find optimum based on sampling distribution
        nTrials = number_of_trials(sampledBuckets, nBucketsY*nBucketsX)
           
    #iterate
    for trial in xrange(nTrials.astype('int')):
        
        #select tiepoints from AATSR points (AATSR is the 'image')
        tiepointIndexes, checkpointIndexes = select_tiepoints(locs1, sampledBuckets, nBucketsY, nBucketsX, sampledYBuckets, sampledXBuckets, bucketSize)
        
        #perform two runs to remove outliers
        for run in ['1st_run','2nd_run']:
            
            #derive image transformation by mapping the 'image' (AATSR), onto the 'map' (MERIS)
            transform = lsfit(locs1, locs2, tiepointIndexes)
            
            #check transformation quality
            tiepointIndexes, warpRMSE = assess_warp(run, locs1, locs2, tiepointIndexes, checkpointIndexes, transform)
            
        if warpRMSE < bestWarpRMSE:
            optimalTransform = transform
            bestWarpRMSE = warpRMSE
                
    return optimalTransform
            
def warp(optimalTransform, image1, image2Shape, look):
    
    #use the transform to warp the image
    return warp_image(optimalTransform, image1, image2Shape, look)

def buckets(locations, bucketSize):
    
    #tie points are x,y rather than usual numpy y,x convention
    xMaxTiepoint = np.round(np.max(locations[:,0])) 
    xMinTiepoint = np.round(np.min(locations[:,0]))
    yMaxTiepoint = np.round(np.max(locations[:,1]))
    yMinTiepoint = np.round(np.min(locations[:,1]))
        
    #find the (whole) number of buckets 
    differenceX = xMaxTiepoint - xMinTiepoint
    differenceY = yMaxTiepoint - yMinTiepoint
    remainderX = differenceX % bucketSize
    remainderY = differenceY % bucketSize
    nBucketsX = (differenceX - remainderX)/bucketSize + 1 #add one more bucket as we have subtracted the remainder 
    nBucketsY = (differenceY - remainderY)/bucketSize + 1
                        
    #get the number of sampled buckets by rounding x and y bucket to it nearest bucket
    sampledXBuckets = round_to_base(np.round(locations[:,0]) - xMinTiepoint, bucketSize)
    sampledYBuckets = round_to_base(np.round(locations[:,1]) - yMinTiepoint, bucketSize)
    sampledBuckets = np.unique(sampledXBuckets + sampledYBuckets*10).shape[0]  #10 is an arbitrary scaling value
    
    return nBucketsY, nBucketsX, sampledYBuckets, sampledXBuckets, sampledBuckets

def number_of_trials(sampledBuckets, totalBuckets):
    #determine number of trials to get to .99% confidence that optimum sample located
    trials = np.round(np.log(0.01) / np.log(1 - (sampledBuckets/totalBuckets)**(totalBuckets/sampledBuckets)))
    if np.isnan(trials) == True:
        return -1
    if np.isinf(trials) == True:
        return -1
    return trials

def select_tiepoints(locations, sampledBuckets, nBucketsY, nBucketsX, sampledYBuckets, sampledXBuckets, bucketSize):
    
    #find the bucket to which each tiepoint belongs
    #and define the weight (sample ratio) for the bucket
    bucketIndex = np.zeros(locations.shape[0])
    bucketWeights = np.zeros(sampledBuckets)
    bucketID = 0
    for i in np.arange(0, (nBucketsY + 1)*bucketSize, bucketSize):
        for j in np.arange(0, (nBucketsX + 1)*bucketSize, bucketSize):
            whichBuckets = (sampledYBuckets == i) & (sampledXBuckets == j)
            if np.max(whichBuckets) != 0:
                bucketIndex[whichBuckets] = bucketID
                bucketWeights[bucketID] = np.sum(whichBuckets)
                bucketID += 1
    
    #scale the bucket weights from 0,1 and cumulatively sum
    bucketWeights = np.cumsum(bucketWeights * (1.0/np.sum(bucketWeights)))
    
    #randomly select a bucket
    tiepointIndexes = None
    checkpointIndexes = None
    while np.min(bucketWeights) < 99999:
        randomNumber = np.random.random()
        chosenBucket = np.argmin(np.abs(bucketWeights - randomNumber))
            
        #randomly select a tiepoint
        potentialTiepoints = np.where(bucketIndex == chosenBucket)[0]
        if potentialTiepoints.size > 1:
            chooser = np.zeros(potentialTiepoints.size, dtype='bool') 
            chosenTiepoint = np.argmin(np.random.random(potentialTiepoints.size))
            chooser[chosenTiepoint] = 1  
        
        #extract tiepoint index from bin
        if tiepointIndexes == None:
            if potentialTiepoints.size == 1:
                tiepointIndexes = potentialTiepoints
            else:
                tiepointIndexes = potentialTiepoints[chooser]
        else:
            if potentialTiepoints.size == 1:
                tiepointIndexes = np.concatenate([tiepointIndexes, potentialTiepoints], axis=0)
            else:
                tiepointIndexes = np.concatenate([tiepointIndexes, potentialTiepoints[chooser]], axis=0)
            
        #set any remaining tie points in the bin to check points
        if potentialTiepoints.size > 1:
            if checkpointIndexes == None:
                checkpointIndexes = potentialTiepoints[~chooser]
            else:
                checkpointIndexes = np.concatenate([checkpointIndexes, potentialTiepoints[~chooser]], axis=0)
            
        #remove the chosen bucket from further consideration
        bucketWeights[chosenBucket] = 99999
          
    return tiepointIndexes, checkpointIndexes
       
def lsfit(locs1, locs2, index):
    '''
    This deermines the transformation to warp the image 
    onto the map
    '''
    #regressand -  the 'map' coordinates, which are dependent upon the 'image' 
    R = np.zeros([index.size*2]) 
    R[0:index.shape[0]] =  locs2[index,1] #map indexes (y)
    R[index.shape[0]:] = locs2[index,0] #map indexes (x)
        
    #design - the 'image' coordinates
    inputD = np.zeros([index.size, 3])
    inputD[:,0] = 1
    inputD[:,1] = locs1[index,1] #image indexes (r)
    inputD[:,2] = locs1[index,0] #image indexes (c)
        
    D = np.zeros([index.size*2, 6])
    D[0:index.size, 0:3] = inputD
    D[index.size:,3:] = inputD 
                    
    #derive the function to transform the image coordinates (X) onto the map (Y), so that Y=f(X)
    DT = D.T 
    DTD = np.dot(DT,D)
    DTR = np.dot(DT, R)
    L,lower = linalg.cho_factor(DTD, lower=True)
    return linalg.cho_solve((L,lower),DTR)  
        
def assess_warp(run, locs1, locs2, index, checkIndex, transform):
    
    #apply warp to the 'image'
    warpedY, warpedX = warp_points(locs1, index, transform)
             
    #compute distances of the warped 'image' coordinates from the 'map' coordinates 
    diffX = warpedX - locs2[index,0]
    diffY = warpedY - locs2[index,1]
    
    #furst run to remove out-lying tie-points
    if run == '1st_run':
        meanDiffY = np.mean(diffY) 
        meanDiffX = np.mean(diffX)
        stdDiffY = np.std(diffY)
        stdDiffX = np.std(diffX)
        inliers = (diffY > (meanDiffY - stdDiffY)) & (diffY < (meanDiffY + stdDiffY)) & (diffX > (meanDiffX - stdDiffX)) & (diffX < (meanDiffX + stdDiffX))
        index = index[inliers]
        
        return index, None
    
    else:
        #evaluate tiepoints
        residuals = np.sqrt(diffY**2 + diffX**2)
        sumSquareResiduals  = np.sum(residuals**2)
        tiepointRMSE = np.sqrt(sumSquareResiduals/diffY.shape[0]) 
        
        #warp checkpoints
        warpedY, warpedX = warp_points(locs1, checkIndex, transform)
        
        #evaluate checkpoints
        diffX = warpedX - locs2[checkIndex,0]
        diffY = warpedY - locs2[checkIndex,1]
        residuals = np.sqrt(diffY**2 + diffX**2)
        sumSquareResiduals  = np.sum(residuals**2)
        checkpointRMSE = np.sqrt(sumSquareResiduals/diffY.shape[0]) 
            
        #compute RMSE
        RMSE = tiepointRMSE + checkpointRMSE
        
        return None, RMSE
                     
def warp_points(data, index, transform):
    '''
    Apply the warp to the 'image', to transform in into the 'map'
    '''
    warpedY = transform[0] + data[index,1]*transform[1] + data[index,0]*transform[2] 
    warpedX = transform[3] + data[index,1]*transform[4] + data[index,0]*transform[5]
    
    return warpedY, warpedX
        
def warp_image(transform, image, mapShape, look):
    
    #make the image coordinate grid
    gridX, gridY = np.meshgrid(np.arange(image.shape[1]), np.arange(image.shape[0]))
    
    #create the output image
    resampledImage = np.zeros(mapShape) + (-99999)
    
    #get new image coordinates
    if look=='nadir':
        warpedGridY = transform[0] + gridY*transform[1] + gridX*transform[2] 
        warpedGridX = transform[3] + gridY*transform[4] + gridX*transform[5]
    if look=='fward':
        #adjust the forward by approximate translation (0th affine coefficient) from Fisher and Muller (2013), which is approximately 1.5 in both axes for 2007  
        warpedGridY = (transform[0] - 1.5) + gridY*transform[1] + gridX*transform[2]
        warpedGridX = (transform[3] + 1.5) + gridY*transform[4] + gridX*transform[5]      

    #round NN coordintes
    warpedGridY = np.round(warpedGridY).astype('int')
    warpedGridX = np.round(warpedGridX).astype('int')
    
    #find the inbound map points
    index = (warpedGridY >= 0) & (warpedGridY < resampledImage.shape[0]) & (warpedGridX >= 0) & (warpedGridX < resampledImage.shape[1])
    
    #obtain the inbound map points
    y = np.squeeze(warpedGridY[index])
    x = np.squeeze(warpedGridX[index])
    
    #obtain the inbound image point
    r = np.squeeze(gridY[index])
    c = np.squeeze(gridX[index])
    
    #place data into resampled image
    resampledImage[y,x] = image[r,c]

    return resampledImage

def warp_and_write(outDir, aatsrDS, merisDS, warpParams):
        
    #Generate the output nc dataset
    rootgrp = Dataset(outDir, 'w', format='NETCDF4')
    y = merisDS.variables['latitude'][:, :].shape[0]
    x = merisDS.variables['latitude'][:, :].shape[1]
    rootgrp.createDimension('y', y)
    rootgrp.createDimension('x', x)
    latitudes = rootgrp.createVariable('latitude_S','f4',('y','x'), zlib=True, complevel=1)
    latitudes[:,:] = merisDS.variables['latitude'][:,:]
    longitudes = rootgrp.createVariable('longitude_S','f4',('y','x'), zlib=True, complevel=1)
    longitudes[:,:] = merisDS.variables['longitude'][:,:]
    
    #place the meris data into the file
    for variable in merisDS.variables:
        if variable != "longitude" and variable != "latitude":
            temp = rootgrp.createVariable(variable + '_M','f4',('y','x'), zlib=True, complevel=1)
            temp[:,:] = merisDS.variables[variable][:,:]
            if hasattr(merisDS.variables[variable], 'scale_factor'):
                scaleFactor = merisDS.variables[variable].getncattr('scale_factor')
                temp[:,:] /= scaleFactor
                unscaledArray = np.zeros(shape=(y,x))
                unscaledArray[:,:] = temp[:,:]
                unscaledArray[unscaledArray<=0.0] += 65536.0
                temp[:,:] = unscaledArray[:,:] * scaleFactor;
            if variable == "l1_flags_M":
                unscaledArray[unscaledArray<0.0] = 128.0

    #warp aatsr data and place into the nc file
    for variable in aatsrDS.variables:
        if variable.find("_nadir") > 0:
            temp = rootgrp.createVariable(variable + '_S','f4',('y','x'), zlib=True, complevel=1)
            temp[:,:] = warp(warpParams, aatsrDS.variables[variable][:,:], merisDS.variables['latitude'][:,:].shape, look='nadir')
        elif variable.find("_fward") > 0:
            temp = rootgrp.createVariable(variable + '_S','f4',('y','x'), zlib=True, complevel=1)
            temp[:,:] = warp(warpParams, aatsrDS.variables[variable][:,:], merisDS.variables['latitude'][:,:].shape, look='fward')

    #close the output
    rootgrp.close()


def round_to_base(valuestoRound, bucketSize):
    return bucketSize*np.round(valuestoRound/bucketSize)
    

            
