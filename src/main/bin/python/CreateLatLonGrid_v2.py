#!/usr/local/epd-6.3-2-rh5-x86_64/bin/python

import os
import sys

import osgeo.gdal as gdal
from osgeo.gdalconst import *

from pyproj import Proj
from pyproj import transform

import numpy

def SunPositionLocalNoon ( DoY, longitude, latitude ):
    """
    Original code provided by: Jose Luis Gomez-Dans
    https://gist.github.com/733741

    Modified by: Gerardo Lopez-Saldana 

    Calculates the position of the sun given a position at local solar noon.
    Basically, all you need to know is here: 
    http://answers.google.com/answers/threadview/id/782886.html
    """
    import time
    from numpy import sin
    from numpy import cos
    from numpy import degrees
    from numpy import radians
    from numpy import arccos as acos
    from numpy import deg2rad

    #from math import sin, cos, degrees, radians, acos

    #DoY = int ( time.strftime( "%j", time.strptime( date, "%Y-%m-%d" ) ) )
    latitude = deg2rad ( latitude )
    #longitude = deg2rad ( longitude )

    # Calculate Local Solar Time LST
    n = numpy.round(DoY - 2451545 - 0.0009 - (longitude / 360.))
    J = 2451545. + 0.0009 + (longitude / 360.) + n
    J = (J - JulianDay) * 60

    #EoT
    M = (2*numpy.pi*JulianDay)/365.242
    EoT = -7.655 * numpy.sin(M) + 9.873*numpy.sin(2*M + 3.588)

    TimeZone = 0
    LST = (720-4*longitude-EoT+TimeZone*60)/1440

    longitude = deg2rad ( longitude )

    #LST_hours = int(LST*24.0)
    #LST_mins = int((LST*24.0 - LST_hours) * 60.)
    #LST_secs = (((LST*24.0 - LST_hours) * 60.) - LST_mins) * 60.
    #print "LST:", str(LST_hours), str(LST_mins), str(LST_secs)

    #( hh, mm )  = hour.split(":")
    #h = float(hh)
    #h = h + float(mm)/60.

    # To emulate MODIS products, set fixed LST = 12.00
    LST[:,:] = 12.0

    ##Now we can calculate the Sun Zenith Angle (SZA):
    h = (12.0 - (LST)) / 12.0 * numpy.pi
    delta = -23.45 * (numpy.pi/180.0) * cos (2 * numpy.pi/365.0 * (DoY+10))
    SZA = degrees( acos(sin(latitude) * sin(delta) + cos(latitude) * cos(delta) * cos(h)) )

    return (SZA, LST*24)

#====================================================================================

from IPython.Shell import IPShellEmbed
ipshell = IPShellEmbed([''], banner = 'Dropping into IPython', exit_msg = 'Leaving Interpreter, back to program.')

Tile = sys.argv[1]
ULC_X = float(sys.argv[2])
ULC_Y = float(sys.argv[3])

#Define projections
sinusoidal = Proj(proj='sinu', lon_0=0, x_0=0, y_0=0, a=6371007.181, b=6371007.181, units='m')
latlon = Proj(proj='latlon', ellps='WGS84', datum='WGS84')
#latlon = Proj(proj='latlon', a=6371007.181, b=6371007.181)

rows = 1200
cols = 1200
PixelSizeX = 926.625433055833355
PixelSizeY = -926.625433055000258

LatLonGrid = numpy.zeros((2,rows,cols), numpy.float32)

# The ULC_X and ULC_Y are the coordinated of the very ulc, to obtain the coordinates of the center of the pixel is necessary to add (PixelSize/2) 
X = ULC_X + (PixelSizeX/2.0)
Y = ULC_Y + (PixelSizeY/2.0)

for j in range(0, rows):
    for i in range(0, cols):

        X_latlon, Y_latlon = transform(sinusoidal,latlon,X,Y)
        LatLonGrid[0,j,i] = X_latlon
        LatLonGrid[1,j,i] = Y_latlon

        #print i, j, X, Y, X_latlon, Y_latlon

	X = X + PixelSizeX

    X = ULC_X + (PixelSizeX/2.0)
    Y = Y + PixelSizeY

JulianDay = 1
longitude = LatLonGrid[0,:,:]
latitude = LatLonGrid[1,:,:]

# For all DoY
DaysOfYear = range(1,365,8)
Angles = numpy.zeros((len(DaysOfYear),rows,cols), numpy.float32)

i = 0
for JulianDay in DaysOfYear:
    print 'Calculating SZAs for DoY', JulianDay
    SZA, LST = SunPositionLocalNoon(JulianDay, longitude, latitude)
    Angles[i,:,:] = SZA
    i += 1

i = 0
for JulianDay in DaysOfYear:
    # Output file
    format = "ENVI"
    driver = gdal.GetDriverByName(format)

    strJulianDay = str(JulianDay)
    if len(strJulianDay) == 1:
        strJulianDay = "00" + strJulianDay
    elif len(strJulianDay) == 2:
        strJulianDay = "0" + strJulianDay

    new_dataset = driver.Create( Tile + '.' + strJulianDay + '.SZA.bin', cols, rows, 3, GDT_Float32 )

    new_dataset.GetRasterBand(1).WriteArray(LatLonGrid[0,:,:])
    new_dataset.GetRasterBand(2).WriteArray(LatLonGrid[1,:,:])
    new_dataset.GetRasterBand(3).WriteArray(Angles[i,:,:])

    new_dataset = None

    i += 1
