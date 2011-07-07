#!/usr/local/epd-7.0-2-rh5-x86_64/bin/python

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
    print "n          : ", n 
    print "JulianDay  : ", JulianDay 
    print "J          : ", J 

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
#    LST[:,:] = 12.0
    LST = 12.0

    ##Now we can calculate the Sun Zenith Angle (SZA):
    h = (12.0 - (LST)) / 12.0 * numpy.pi
    delta = -23.45 * (numpy.pi/180.0) * cos (2 * numpy.pi/365.0 * (DoY+10))
    SZArad = acos(sin(latitude) * sin(delta) + cos(latitude) * cos(delta) * cos(h))
    SZA = degrees( acos(sin(latitude) * sin(delta) + cos(latitude) * cos(delta) * cos(h)) )
    print "h         : ", h    
    print "delta     : ", delta    
    print "SZArad       : ", SZArad    
    print "SZA       : ", SZA    

    return (SZA, LST*24)

#====================================================================================

from IPython.Shell import IPShellEmbed
ipshell = IPShellEmbed([''], banner = 'Dropping into IPython', exit_msg = 'Leaving Interpreter, back to program.')

JulianDay = 129
latitude = 50.0
longitude = 0.0
print "Calculating SZAs for DoY, lat, lon: ", JulianDay, ", ", latitude, ", ", longitude
SZA, LST = SunPositionLocalNoon(JulianDay, longitude, latitude)
print "SZA: ", SZA
