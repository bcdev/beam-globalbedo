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




#from enthought.pyface.ui.wx.grid.grid import Grid
#from matplotlib.colors import NP_CLIP_OUT
#from scikits.statsmodels.sandbox.regressi):
    #matplotlib.rcParams.update({'font.size': 8})

    #imgplot = plt.imshow(array)
    #imgplot.set_cmap('Set1')


from datetime import datetime,date,time
dpi = 80.0
margin = 0.00

default_cmap='gist_stern'
creationYear='2014'



default_cmap='gist_stern'


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




def plotImag(array, outPNG, band, date):
    fig = plt.figure(figsize=figsize, dpi=dpi)
    ax1 = fig.add_axes([margin, margin, 1 - 2*margin, 1 - 2*margin])

    res=360.0/float(len(array[0][0]))   
    c=np.dstack([array[0], array[1], array[2]])
	
    im=ax1.imshow(c, interpolation='nearest')
    
    plt.xticks([])
    plt.yticks([])
    #plt.title(date)
   
    #print 'RESOLUTION: ', res
    ax1.text(0.67, 0.2, date,
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color=colorTxt, fontsize=int(7/res),fontstyle='oblique')
    
    ax1.text(0.1, 0.4, band[0],
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color='red', fontsize=int(6/res), fontstyle='oblique')

    ax1.text(0.1, 0.35, band[1],
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color='green', fontsize=int(6/res), fontstyle='oblique')

    ax1.text(0.1, 0.30, band[2],
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color='blue', fontsize=int(6/res), fontstyle='oblique')
    

    ax1.text(0.2, 0.02, "ImagingGroup.MSSL.UCL("+creationYear+")",
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color='black', fontsize=int(2.5/res), fontstyle='oblique')

    #plt.show()                
    plt.savefig(outPNG)
                    
    plt.clf()
    plt.close()
    

inFile = sys.argv[1]
outDir = sys.argv[2]
bands = sys.argv[3].split(',') 
min_max = sys.argv[4].split(',');
size = sys.argv[5];
idxdate = int(sys.argv[6])
colorTxt = sys.argv[7]

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
ncfile = nc.Dataset(inFile,'r')
i=0
dataTab=[]


for band in bands:
    #data= ncfile.variables[band][0,:]
    data= ncfile.variables[band][:]
    w=np.where(np.isnan(data))
    #print data[0,0]
    data[w]=0.0
    #print data[0,0]
    data=np.where(data<=max[i], data, max[i])
    data=np.where(data>min[i], data, 0)
    dataTab.insert(i,data)
    i+=1

outdir=outDir+'/'+bands[0]+'.'+bands[1]+'.'+bands[2]
os.system('mkdir -p '+outdir)
date0=date[0:4]+'.'+date[4:]
dateStr=toDateStr(date0)
plotImag(dataTab,outdir+'/'+name+"_"+bands[0]+'__'+bands[1]+'__'+bands[2]+".png", bands, dateStr)

ncfile.close()

