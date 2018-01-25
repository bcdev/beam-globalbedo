#!/usr/bin/env python

import netCDF4 as nc

import matplotlib as mpl
mpl.use('Agg')

import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import numpy as np

import os
import sys
import copy
from datetime import datetime,date,time
#from enthought.pyface.ui.wx.grid.grid import Grid
#from matplotlib.colors import NP_CLIP_OUT
#from scikits.statsmodels.sandbox.regression.kernridgeregress_class import plt_closeall

dpi = 80.0
margin = 0.00

default_cmap='gist_stern'

creationYear='2014'

def readcdict(lutfile, minV, maxV):
    nbLines = 101 # number of lines in new clut 'color_lut_ga.txt'
    if (lutfile!=None):
        red=[]
        green=[]
        blue=[]
        lut=open(lutfile, 'r')
        i=0
        for line in lut:
                tab=line.split(',')
                tab[0]=tab[0].strip()
                tab[1]=tab[1].strip()
                tab[2]=tab[2].strip()
                #val= (i/256.0)
                val= minV+((i*1.0/(nbLines-1))*(maxV-minV))
                #print 'i, val: ', i, '//', val
                red.insert(i,(val , float(tab[0])/255.0, float(tab[0])/255.0))
                green.insert(i,(val, float(tab[1])/255.0, float(tab[1])/255.0))
                blue.insert(i,(val, float(tab[2])/255.0, float(tab[2])/255.0))

                i+=1
        return {'red':red, 'green':green, 'blue':blue}
    else:
        return None

def toDateStr(dateIn):
	
	year=date[0:4]
	
	month=-1
	doy=-1
	if(len(dateIn)==8):
		doy=dateIn[5:8]
	else:
		month=dateIn[5:7]
	
	out=year
		
	if(doy>-1):
		dt=datetime.strptime(dateIn, "%Y.%j")
                out+=" - "+dt.strftime('%d')+' '+dt.strftime('%B')+" (doy:"+doy+")"
	if(month>-1):
                dt=datetime.strptime(dateIn, "%Y.%m")
                out+=" - "+dt.strftime('%B')

	return out
    
def plotImag(array, outPNG, band, date, tickets, cdict):
    fig = plt.figure(figsize=figsize, dpi=dpi)
    ax1 = fig.add_axes([margin, margin, 1 - 2*margin, 1 - 2*margin])
    
    my_cmap=default_cmap
    if(cdict != None):
        my_cmap = mpl.colors.LinearSegmentedColormap('my_colormap',cdict,256)
    	
    im=ax1.imshow(array, cmap=my_cmap)
    
    plt.xticks([])
    plt.yticks([])
    #plt.title(date)
    res=360.0/float(len(array[0]))  
    #cbaxes = fig.add_axes([0.05, 0.1, 0.05, 0.6]) 
    #cbar=plt.colorbar(im, ticks=tickets, cax=cbaxes)
    #cbar.ax.yaxis.set_tick_params(color='black')
    #for label in cbar.ax.get_yticklabels():  
    #        label.set_color(colorTxt)
    #        label.set_size(int((4/res)))

    # horizontal:
    cbaxes = fig.add_axes([0.05, 0.9, 0.45, 0.05]) # left, bottom, width, height
    cbar=plt.colorbar(im, ticks=tickets, cax=cbaxes, orientation="horizontal")
    cbar.ax.xaxis.set_tick_params(color='black')
    for label in cbar.ax.get_xticklabels():
            label.set_color(colorTxt)
            label.set_size(int((4/res)))
    

    #print 'RESOLUTION: ', res
    ax1.text(0.67, 0.2, date,
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color=colorTxt, fontsize=int(7/res),fontstyle='oblique')
    
    #ax1.text(0.12, 0.72, band,
    ax1.text(0.05, 0.97, band,
        verticalalignment='bottom', horizontalalignment='left',
        transform=ax1.transAxes,
        color=colorTxt, fontsize=int(7/res), fontstyle='oblique')
        #color=colorTxt, fontsize=int(5/res), fontstyle='oblique')
  
    #ax1.text(0.2, 0.02, "ImagingGroup.MSSL.UCL("+creationYear+")",
    ax1.text(0.05, 0.85, "ImagingGroup.MSSL.UCL("+creationYear+"-2018)",
        verticalalignment='bottom', horizontalalignment='left',
        transform=ax1.transAxes,
        color='white', fontsize=int(2.5/res), fontstyle='oblique')
        #color='black', fontsize=int(2.5/res), fontstyle='oblique')

    #plt.show()                
    plt.savefig(outPNG)
                    
    plt.clf()
    plt.close()
    
inFile = sys.argv[1]
outDir = sys.argv[2]
bands = sys.argv[3].split(',') 
min_max = sys.argv[4].split(',');
lutColor = sys.argv[5];
size = sys.argv[6];
idxdate = int(sys.argv[7])
colorTxt = sys.argv[8]

bandsDis = sys.argv[9]

bandDisplay=copy.copy(bands)

if(bandsDis!='None' and  bandDisplay!='none'):
        bandDisplay=bandsDis.split(',')



numTicks=4;


if(lutColor=='None' or lutColor=='none'):
	lutColor=None


w, h = int(size.split("x")[0]), int(size.split("x")[1])
figsize =  (1 + margin) * w / dpi, (1 + margin) * h / dpi




max=[]
min=[]

i=0
for val in min_max:
    min.insert(i, float(val.split(':')[0]));
    max.insert(i, float(val.split(':')[1]));
    #print bands[i], 'min, max', min[i],max[i]  
    i+=1
name=inFile.split('/')[-1].replace('.nc','')    
date=name.split('.')[idxdate];    
print('inFile: ', inFile) 
print('name: ', name) 
ncfile = nc.Dataset(inFile,'r')
i=0
for band in bands:
    #data= ncfile.variables[band][0,:]
    # NOTE: Alex Loew requested a 3rd dimension (time). For old mosaic netcdf (2D) use this:
    data= ncfile.variables[band][:]
    w=np.where(np.isnan(data))
    #print 'data', data
    data[w]=0.0
    #print data[0,0]
    data=np.where(data<=max[i], data, max[i])
    data=np.where(data>min[i], data, 0.0)
    #print np.min(data), np.max(data)
    data[0,0]=min[i]
    data[-1,-1]=max[i]
    bin=(min[i]+max[i])/float(numTicks); 
    tickets=[min[i]+(j*bin) for j in range(numTicks+1)]
    date0=date[0:4]+'.'+date[4:]
    cdict=readcdict(lutColor, min[i], max[i])
    #print 'cdict: ', cdict
    #print 'min: ', min[i]
    #print 'max: ', max[i]
    outdir=outDir+'/'+bandDisplay[i]
    os.system('mkdir -p '+outdir)

    dateStr=toDateStr(date0)

    plotImag(data,outdir+'/'+name+"_"+bandDisplay[i]+".png", bandDisplay[i], dateStr, tickets, cdict)
    i+=1

ncfile.close()
exit(0)

