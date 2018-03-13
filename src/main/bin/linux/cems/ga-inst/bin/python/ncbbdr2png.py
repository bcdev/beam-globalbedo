#!/usr/bin/env python

import netCDF4 as nc

import matplotlib as mpl
mpl.use('Agg')

import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import numpy as np

#############
# parameters: ${stagingNc2browseFile} ${stagingNc2browseResultDir} ${datestring} ${band} ${MINMAX} ${LUT} ${SIZE} ${COLORTXT}
#############


import os
import sys
import copy
from datetime import datetime,date,time

dpi = 80.0
margin = 0.00

default_cmap = 'gist_stern'
creationYear = '2014'

##########################################################################################
def readcdict(lutfile, minV, maxV):

    if (lutfile!=None):
        red = []
        green = []
        blue = []
        lut = open(lutfile, 'r')
        red.insert(0,(0,0,0))
        green.insert(0, (0,0,0))
        blue.insert(0, (0,0,0))
        i = 1
        for line in lut:
            tab = line.split(',')
            tab[0] = tab[0].strip()
            tab[1] = tab[1].strip()
            tab[2] = tab[2].strip()
            val = (i/256.0)
            red.insert(i,(val , float(tab[0])/255.0, float(tab[0])/255.0))
            green.insert(i,(val, float(tab[1])/255.0, float(tab[1])/255.0))
            blue.insert(i,(val, float(tab[2])/255.0, float(tab[2])/255.0))
            i += 1

        return {'red':red, 'green':green, 'blue':blue}
    else:
        return None

##########################################################################################
def toDateStr(dateIn):
	
	year = dateIn[0:4]
	
	month = -1
	doy = -1
	if (len(dateIn) == 8):
	    doy = dateIn[5:8]
	else:
	    month = dateIn[5:7]
	
	out = year
		
	if (doy > -1):
            dt = datetime.strptime(dateIn, "%Y.%j")
            out += " - "+dt.strftime('%d')+' '+dt.strftime('%B')+" (doy:"+doy+")"
	if (month > -1):
            dt = datetime.strptime(dateIn, "%Y.%m")
            out += " - "+dt.strftime('%B')

	return out

##########################################################################################
def plotImag(array, outPNG, band, date, tickets, cdict):
    
    fig = plt.figure(figsize=figsize, dpi=dpi)
    ax1 = fig.add_axes([margin, margin, 1 - 2*margin, 1 - 2*margin])
    
    my_cmap = default_cmap
    if (cdict != None):
        print 'bla'
    	my_cmap = mpl.colors.LinearSegmentedColormap('my_colormap',cdict,256)
    	
    im = ax1.imshow(array, cmap=my_cmap)
    
    plt.xticks([])
    plt.yticks([])
    #plt.title(date)
    #res = 360.0/float(len(array[0]))  
    res = 0.5
    cbaxes = fig.add_axes([0.05, 0.1, 0.05, 0.6]) 
    
    cbar = plt.colorbar(im, ticks=tickets, cax=cbaxes)
    cbar.ax.yaxis.set_tick_params(color='black')
    for label in cbar.ax.get_yticklabels():  
        label.set_color(colorTxt)
        label.set_size(int((4/res)))
    
    ax1.text(0.67, 0.2, date,
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color=colorTxt, fontsize=int(7/res),fontstyle='oblique')
    
    ax1.text(0.12, 0.72, band,
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color=colorTxt, fontsize=int(5/res), fontstyle='oblique')
  
    ax1.text(0.2, 0.02, "ImagingGroup.MSSL.UCL("+creationYear+")",
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color='black', fontsize=int(2.5/res), fontstyle='oblique')

    #plt.show()                
    plt.savefig(outPNG)
                    
    plt.clf()
    plt.close()

##########################################################################################
##########################################################################################
    
inFile = sys.argv[1]
outDir = sys.argv[2]
dateString = sys.argv[3] 
band = sys.argv[4] 
min_max = sys.argv[5]
lutColor = sys.argv[6];
size = sys.argv[7];
colorTxt = sys.argv[8]

numTicks=4;

if (lutColor == 'None' or lutColor == 'none'):
    lutColor = None

w, h = int(size.split("x")[0]), int(size.split("x")[1])
figsize =  (1 + margin) * w / dpi, (1 + margin) * h / dpi

min = float(min_max.split(':')[0]);
max = float(min_max.split(':')[1]);

ncfile = nc.Dataset(inFile,'r')
i = 0
#data= ncfile.variables[band][0,:]
# NOTE: Alex Loew requested a 3rd dimension (time). For old mosaic netcdf (2D) use this:
data = ncfile.variables[band][:]
w = np.where(np.isnan(data))
data[w] = 0.0
data = np.where(data<=max, data, max)
data = np.where(data>min, data, 0.0)
data[0,0] = min
data[-1,-1] = max
bin = (min + max)/float(numTicks); 
tickets=[min+(j*bin) for j in range(numTicks+1)]

cdict = readcdict(lutColor, min, max)
outdir = outDir + '/' + band
outFileName = inFile.split('/')[-1].replace('.nc','') + '_' + band + '.png'

os.system('mkdir -p '+outdir)

#print 'min: ', min
#print 'max: ', max
#print 'bin: ', bin
#print 'outdir: ', outdir
#print 'inFile: ', inFile
#print 'band: ', band
#print 'dateString: ', dateString
#print 'tickets: ', tickets
#print 'outFileName: ', outFileName

plotImag(data, outdir + '/' + outFileName, band, dateString, tickets, cdict)

ncfile.close()
exit(0)

