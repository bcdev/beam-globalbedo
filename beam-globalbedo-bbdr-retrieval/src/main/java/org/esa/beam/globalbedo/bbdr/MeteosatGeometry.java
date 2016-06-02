package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.math.MathUtils;

import java.util.Calendar;

/**
 * Calculates the azimuth and zenith angles to the sun and the satellite, from the centre of the current segment,
 * given the time of day, the day of the year, and the segment position.
 *
 * @author olafd
 */
public class MeteosatGeometry {

    private static final double GEO_MAXANGLEIN = 85.0;
    private static final double GEO_DOUBLE_EPSILON = 1.0E-10;
    private static final double GEO_EQTIME1 = 229.18;
    private static final double GEO_EQTIME2 = 0.000075;
    private static final double GEO_EQTIME3 = 0.001868;
    private static final double GEO_EQTIME4 = 0.032077;
    private static final double GEO_EQTIME5 = 0.014615;
    private static final double GEO_EQTIME6 = 0.040849;
    private static final double GEO_DECL1 = 0.006918;
    private static final double GEO_DECL2 = 0.399912;
    private static final double GEO_DECL3 = 0.070257;
    private static final double GEO_DECL4 = 0.006758;
    private static final double GEO_DECL5 = 0.000907;
    private static final double GEO_DECL6 = 0.002697;
    private static final double GEO_DECL7 = 0.00148;

    private static final double TIME_ZONE = 0.0;

    // MVIRI:
    //    dbEarth_Polar_radius_meter =  6356.755 * 1000.;
    //    dbEarth_radius_meter       =  6378.140* 1000.;
    //    dbSat_Eight_meter          =  42164.0 * 1000.;
    // SEVIRI:
    //    dbEarth_Polar_radius_meter =  6356.5838 * 1000.;
    //    dbEarth_radius_meter       =  6378.1690 * 1000.;
    //    dbSat_Eight_meter          =  42164.0  * 1000.;
    private static final double EARTH_POLAR_RADIUS_METER_MVIRI = 6356775.0;
    private static final double EARTH_POLAR_RADIUS_METER_SEVIRI = 6356583.8;
    private static final double EARTH_RADIUS_METER_MVIRI = 6378140.0;
    private static final double EARTH_RADIUS_METER_SEVIRI = 6378169.0;
    private static final double SAT_EIGHT_METER = 42164000.0;

    /**
     * Calculates the azimuth and zenith angles to the sun, from the centre of the current segment,
     * given the year, time of day, the day of the year, pixel and satellite positions as input.
     * Follows original C implementation in Eumetsat processing, provided by A.Lattanzio.
     *
     * @param flDegLat         - pixel latitude in deg
     * @param flDegLon         - pixel longitude in deg
     * @param dbSSP            - sub satellite point longitude in deg
     * @param iJulianDayInYear - Julian day in year (doy)
     * @param iYear            - year
     * @param flScanningTime   - scanning time (decimal hour, usually 12.0)
     * @return MeteosatAngles (SZA, SAA)
     */
    public static MeteosatAngles computeSunAngles(double flDegLat,
                                                  double flDegLon,
                                                  double dbSSP,    // 0, 57 or 63 - take from filename
                                                  int iJulianDayInYear,    // doy
                                                  int iYear,
                                                  double flScanningTime) {

        /* Chack input parameters */
        if (Math.abs(flDegLat) > GEO_MAXANGLEIN) {
            throw new OperatorException("computeSunAngles: pixel latitude" + flDegLat +
                                                " out of range - must be <= " + GEO_MAXANGLEIN);
        }

        if (dbSSP < 0) {
            throw new OperatorException("computeSunAngles: lonSSP = " + dbSSP + " not supported - must be >= 0.0");
        } else {
            if (geoChecklonEast(dbSSP, flDegLon) > GEO_MAXANGLEIN) {
                throw new OperatorException("computeSunAngles: mismatch of lonSSP = " + dbSSP + " and  " +
                                                    "lonDeg = " + flDegLon + " - needs to be checked!");
            }
        }

        /* Evaluate the input lat and lon in radians */
        final double latRad = MathUtils.DTOR * flDegLat;

        /* According to the MPEF convention Eastward is positive  */
        /* Westward is negative while the following routine to calculate */
        /* the Sun Angles follows the opposite convention */
        flDegLon = -flDegLon;
//        final double lonRad = MathUtils.DTOR * flDegLon;     // from original code, but not used there

        /* Evaluate the fractional year in radians */
        // todo: do this outside this method to stay in line with C function signature for test purposes!
//        final int year = Integer.parseInt(datestring.substring(0, 4));
//        final int month = Integer.parseInt(datestring.substring(4, 6));
//        final int day = Integer.parseInt(datestring.substring(6, 8));
//        final int doy = getDoyFromYearMonthDay(year, month, day);
//        final double flScanningTime = hour + minutes / 60.0;
        final double hour = 12.0; // brf estimates are at local noon // TODO: 01.06.2016 : check if this is ok
        final double minutes = 0.0;
        final int daysInYear = isLeapYear(iYear) ? 366 : 365;
        final double dbGamma = 2. * Math.PI * (iJulianDayInYear * 1.0 + hour / 24.0) / daysInYear;
        final double dbEquTime = GEO_EQTIME1 * (GEO_EQTIME2 + GEO_EQTIME3 * Math.cos(dbGamma) -
                GEO_EQTIME4 * Math.sin(dbGamma) - GEO_EQTIME5 * Math.cos(2. * dbGamma) -
                GEO_EQTIME6 * Math.sin(2. * dbGamma));

        /* Evaluate the solar declination angle in radians */
        final double dbDecli = GEO_DECL1 - GEO_DECL2 * Math.cos(dbGamma) + GEO_DECL3 * Math.sin(dbGamma) -
                GEO_DECL4 * Math.cos(2 * dbGamma) + GEO_DECL5 * Math.sin(2 * dbGamma) -
                GEO_DECL6 * Math.cos(3 * dbGamma) + GEO_DECL7 * Math.sin(3 * dbGamma);

        /* Time offset in minutes */
        final double dbTimeOffset = dbEquTime - (4. * flDegLon) + (60. * TIME_ZONE);

        /* True solar time in minutes */
        final double dbTrueSolarTime = (flScanningTime * 60.) + dbTimeOffset;
//        final double dbHourFlag = dbTrueSolarTime / 60.;   // from original code, but not used there

        /* solar hour angle in degrees and in radians */
        final double dbHa = (dbTrueSolarTime / 4.) - 180.;
        final double dbHaRad = MathUtils.DTOR * dbHa;

        /* Evaluate the Solar local Coordinates */
        double dbCosZen = Math.sin(latRad) * Math.sin(dbDecli) + Math.cos(latRad) * Math.cos(dbDecli) * Math.cos(dbHaRad);

        if (geoDeqOne(dbCosZen)) {
            dbCosZen = 1.0;
        }
        if (geoDeqMOne(dbCosZen)) {
            dbCosZen = -1.0;
        }

        final double dbTmpZenRad = Math.acos(dbCosZen);
        final double dbTmpZen = MathUtils.RTOD * dbTmpZenRad;

        double dbCosAzi = -(Math.sin(latRad) * Math.cos(dbTmpZenRad) - Math.sin(dbDecli)) / (Math.cos(latRad) * Math.sin(dbTmpZenRad));

        if (geoDeqOne(dbCosAzi)) {
            dbCosAzi = 1.0;
        }
        if (geoDeqMOne(dbCosAzi)) {
            dbCosAzi = -1.0;
        }

        double dbTmpAzi = 360. - MathUtils.RTOD * Math.acos(dbCosAzi);           /* between 180 and 360*/

        if (dbTrueSolarTime < 720.) {
            dbTmpAzi = 360. - dbTmpAzi; /*Correct for Time < 12.00 ( -> in range 0 . 180 )*/
        }

        return new MeteosatAngles(dbTmpZen, dbTmpAzi);
    }

    /**
     * Calculates the azimuth and zenith angles to the satellite, from the centre of the current segment,
     * given the satellite and pixel positions as input.
     * Follows original C implementation in Eumetsat processing, provided by A.Lattanzio.
     *
     * @param flSatLatitude  - satellite latitude in deg
     * @param flSatLongitude - satellite longitude in deg
     * @param flPixelLat     - pixel latitude in deg
     * @param flPixelLon     - pixel longitude in deg
     * @return MeteosatAngles (VZA, VAA)
     */
    public static MeteosatAngles computeViewAngles(double flSatLatitude,    // should be always 0 ?!
                                                   double flSatLongitude,    // 0, 57 or 63 - take from filename
                                                   double flPixelLat,
                                                   double flPixelLon,
                                                   MeteosatSensor sensor) {

        double earthPolarRadiusMeter;
        double earthRadiusMeter;
        if (sensor == MeteosatSensor.MVIRI) {
            earthPolarRadiusMeter = EARTH_POLAR_RADIUS_METER_MVIRI;
            earthRadiusMeter = EARTH_RADIUS_METER_MVIRI;
        } else if (sensor == MeteosatSensor.SEVIRI) {
            earthPolarRadiusMeter = EARTH_POLAR_RADIUS_METER_SEVIRI;
            earthRadiusMeter = EARTH_RADIUS_METER_SEVIRI;
        } else {
            throw new OperatorException("Sensor '" + sensor.getName() + "' not supported.");
        }

        final double dbPixLonRad = MathUtils.DTOR * flPixelLon;
        final double dbPixLatRad = MathUtils.DTOR * flPixelLat;
//        final double dbSatLatRad = MathUtils.DTOR * flPixelLat;   // from original code, but not used there
        final double dbGeoSatLonRad = MathUtils.DTOR * flSatLongitude;

        double dbTmpVal;
        double dbTmpValAzi = 0.0;
        double dbSaph1;
        double dbSaph2;

     /* Calculate geocentric latitude of observation point */
        final double dbGeoDLat = geoEvalGeoDlat(flPixelLat, earthPolarRadiusMeter, earthRadiusMeter);
        final double dbGeoDLatRad = MathUtils.DTOR * dbGeoDLat;

     /* Earth radius (in meters) at geographical latitude of observation point */
        final double dbPixHeight = geoPixRadius(flPixelLat, earthPolarRadiusMeter, earthRadiusMeter);

     /* Transform satellite latitude from geographical to geocentric */
        final double dbGeoDSatLatRad = MathUtils.DTOR * geoEvalGeoDlat(flSatLatitude, earthPolarRadiusMeter, earthRadiusMeter);

     /* Terrestrial coordinates (meters) of satellite */
        final double dbXSat = SAT_EIGHT_METER * Math.cos(dbGeoDSatLatRad) * Math.cos(dbGeoSatLonRad);
        final double dbYSat = SAT_EIGHT_METER * Math.cos(dbGeoDSatLatRad) * Math.sin(dbGeoSatLonRad);
        final double dbZSat = SAT_EIGHT_METER * Math.sin(dbGeoDSatLatRad);

     /* Terrestrial coordinates (meters) of observation point */
        final double dbXPix = dbPixHeight * Math.cos(dbGeoDLatRad) * Math.cos(dbPixLonRad);
        final double dbYPix = dbPixHeight * Math.cos(dbGeoDLatRad) * Math.sin(dbPixLonRad);
        final double dbZPix = dbPixHeight * Math.sin(dbGeoDLatRad);

     /* Components of a vector orthogonal to the */
     /* surface of the earth geoid */
        double dbDro = 0.0;
        if (Math.abs(flPixelLon - flSatLongitude) == 90.0) {
            dbDro = 0.0;
        }
        if (Math.abs(flPixelLon - flSatLongitude) == 0.0) {
            dbDro = Math.sqrt(dbXPix * dbXPix + dbYPix * dbYPix);
        }

        if (Math.abs(flPixelLon - flSatLongitude) > 0.0 && Math.abs(flPixelLon - flSatLongitude) < 90.0) {
            dbDro = dbZPix / Math.tan(dbPixLatRad);
        }

        final double dbXGeo = dbDro * Math.cos(dbPixLonRad);
        final double dbYGeo = dbDro * Math.sin(dbPixLonRad);
        final double dbZGeo = dbZPix;

     /* Vector norms */
        // from original code, but not used there:
//        final double dbTGeo = Math.sqrt((dbXGeo * dbXGeo) + (dbYGeo * dbYGeo) + (dbZGeo * dbZGeo));
        final double dbTSat = Math.sqrt((dbXSat * dbXSat) + (dbYSat * dbYSat) + (dbZSat * dbZSat));
        final double dbTSatGeo = Math.sqrt((dbXSat - dbXPix) * (dbXSat - dbXPix) +
                                                   (dbYSat - dbYPix) * (dbYSat - dbYPix) +
                                                   (dbZSat - dbZPix) * (dbZSat - dbZPix));

     /* Geocentric angle between directions of satellite and observation point */
        double dbGeoSatDAngle = (dbXSat * dbXPix + dbYSat * dbYPix + dbZSat * dbZPix) / (dbTSat * dbPixHeight);
        dbGeoSatDAngle = MathUtils.RTOD * Math.acos(checkEpsilon(dbGeoSatDAngle));

     /* Check if geocentric directions of satellite and observer are coincident */
        double iLabSat = 0.0;
        if (dbGeoSatDAngle > 179.9 || dbGeoSatDAngle <= 0.) {
            iLabSat = 1.0;
        }

     /* Satellite zenith angle */
        double dbArg2 = (dbXPix * (dbXSat - dbXPix) + dbYPix * (dbYSat - dbYPix) +
                dbZPix * (dbZSat - dbZPix)) / (dbPixHeight * dbTSatGeo);
        dbTmpVal = MathUtils.RTOD * Math.acos(checkEpsilon(dbArg2));

        // FIRST RESULT:
        final double flZenith = (float) dbTmpVal;

     /* Satellite Azimuth angle */

     /* Define vector v0 in the meridional plane, perpendicular */
     /* to vertical of obervation point and pointing to north pole */
     /* [see that vertical direction is not necessarily geocentric direction] */
        double dbV90X;
        double dbV90Y;
        double dbV90Z;
        if (Math.abs(90.0 - Math.abs(dbGeoDLat)) < GEO_DOUBLE_EPSILON) {
            double iFac = -1;
            if (dbGeoDLat < 0.) {
                iFac = -iFac;
            }
            dbV90X = iFac * dbPixHeight * Math.cos(dbPixLonRad);
            dbV90Y = iFac * dbPixHeight * Math.sin(dbPixLonRad);
            dbV90Z = 0.;
        } else {
            dbV90X = -dbXGeo * dbZGeo;
            dbV90Y = -dbYGeo * dbZGeo;
            dbV90Z = (dbXGeo * dbXGeo) + (dbYGeo * dbYGeo);
        }
        final double dbV90 = Math.sqrt((dbV90X * dbV90X) + (dbV90Y * dbV90Y) + (dbV90Z * dbV90Z));


     /* Define vector dbY0 orthogonal to the vectors dbV90 and flGeo
        and forming with them a 3-axis right system (dbY0 points eastwards) */
        final double dbY0X = dbV90Y * dbZGeo - dbYGeo * dbV90Z;
        final double dbY0Y = dbV90Z * dbXGeo - dbV90X * dbZGeo;
        final double dbY0Z = dbV90X * dbYGeo - dbXGeo * dbV90Y;
        final double dbY0 = Math.sqrt((dbY0X * dbY0X) + (dbY0Y * dbY0Y) + (dbY0Z * dbY0Z));

     /* Define the vector dbSS pointing to the satellite at observation point */
        final double dbSSX = dbXSat - dbXPix;
        final double dbSSY = dbYSat - dbYPix;
        final double dbSSZ = dbZSat - dbZPix;
        // from original code, but not used there:
//        final double dbSS = Math.sqrt((dbSSX * dbSSX) + (dbSSY * dbSSY) + (dbSSZ * dbSSZ));

     /* Calculate vector on the plane tangent to the observation point
       and pointing to the satellite = flGeo x dbSS x flGeo */
        double[] dbTmpAr = new double[3];
        double dbProdX;
        double dbProdY;
        double dbProdZ;
        double dbProd;
        double dbC1Sat = 0.0;
        double dbC2Sat = 0.0;

        double flAzimuth = 0.0;

        if (iLabSat == 0) {
            dbTmpAr[0] = dbYGeo * dbSSZ - dbZGeo * dbSSY;
            dbTmpAr[1] = dbZGeo * dbSSX - dbXGeo * dbSSZ;
            dbTmpAr[2] = dbXGeo * dbSSY - dbYGeo * dbSSX;

            dbProdX = dbTmpAr[1] * dbZGeo - dbTmpAr[2] * dbYGeo;
            dbProdY = dbTmpAr[2] * dbXGeo - dbTmpAr[0] * dbZGeo;
            dbProdZ = dbTmpAr[0] * dbYGeo - dbTmpAr[1] * dbXGeo;
            dbProd = Math.sqrt((dbProdX * dbProdX) + (dbProdY * dbProdY) + (dbProdZ * dbProdZ));
            if (dbProd > 0.) {
                /* Calculate angles wrt vectors dbY0 and dbV90 */
                /* If angle wrt to y0 exceeds 90 deg => satellite azimuth > 180 deg */
                dbC1Sat = (dbV90X * dbProdX + dbV90Y * dbProdY + dbV90Z * dbProdZ) / (dbV90 * dbProd);
                dbC1Sat = checkEpsilon(dbC1Sat);
                dbC2Sat = (dbY0X * dbProdX + dbY0Y * dbProdY + dbY0Z * dbProdZ) / (dbY0 * dbProd);
                dbC2Sat = checkEpsilon(dbC2Sat);
            } else {
                /* Special case of exterior product = 0 <> dbSS and flGeo have same
                direction It occurs if the satellite is at the zenith (or nadir).
                Its azimuth is undetermined. For geosynchronous satellites we adopt the same solution as for Sun
                Minor modifications will be needed for non-geosynchronous platforms */
                if (dbZPix >= dbZSat) {
                    if (flZenith <= 90.) {
                        dbC1Sat = -1.;
                        dbC2Sat = 0.;
                    } else {
                        dbC1Sat = +1.;
                        dbC2Sat = 0.;
                    }
                }
                if (dbZPix < dbZSat) {
                    if (flZenith <= 90.) {
                        dbC1Sat = +1.;
                        dbC2Sat = 0.;
                    } else {
                        dbC1Sat = -1.;
                        dbC2Sat = 0.;
                    }
                }
            } /* endelse dbProd */
            dbSaph1 = MathUtils.RTOD * Math.acos(dbC1Sat);
            dbSaph2 = MathUtils.RTOD * Math.acos(dbC2Sat);

            if (dbSaph2 <= 90.) {
                dbTmpValAzi = dbSaph1;
            }
            if (dbSaph2 > 90.) {
                dbTmpValAzi = 360. - dbSaph1;
            }

            flAzimuth = (float) dbTmpValAzi;
        } else {
      /* Observation point and satellite directions are coincident
       These are special cases of the satellite azimuth
       The solutions used here are applicable to geosynchronous satellites only
       ... Minor modifications will be needed for non-geosynchronous platforms */
            if (dbZPix >= dbZSat && dbGeoSatDAngle < 90.) {
                dbTmpValAzi = 180.;
                if (dbZPix >= dbZSat && dbGeoSatDAngle > 90.) dbTmpValAzi = 0.;
                if (dbZPix < dbZSat && dbGeoSatDAngle < 90.) dbTmpValAzi = 0.;
                if (dbZPix < dbZSat && dbGeoSatDAngle > 90.) dbTmpValAzi = 180.;
                flAzimuth = (float) dbTmpValAzi;
            }
        }
        return new MeteosatAngles(flZenith, flAzimuth);
    }

    static int getDoyFromYearMonthDay(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        int doy = -1;
        try {
            cal.set(year, month, day);
            doy = cal.get(Calendar.DAY_OF_YEAR);
        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
        }
        return doy;
    }

    // only needed for sat positions < 0 deg longitude
//    private static double geoChecklonWest(double ssp, double lon) {
//        return Math.min(Math.abs(lon - ssp), 360. - (lon - ssp));

    private static double checkEpsilon(double dbArg2) {
        double result = dbArg2;
        if (Math.abs(1.0 - Math.abs(dbArg2)) < GEO_DOUBLE_EPSILON) {
            if (dbArg2 > 0.) {
                result = 1.;
            }
            if (dbArg2 < 0.) {
                result = -1.;
            }
        }
        return result;
    }

    private static boolean geoDeqOne(double v1) {
        return Math.abs(v1 - 1.0) < 1.0E-6;
    }

    private static boolean geoDeqMOne(double v1) {
        return Math.abs(v1 + 1.0) < 1.0E-6;
    }

//    }

    private static double geoChecklonEast(double ssp, double lon) {
        return Math.min(Math.abs(lon - ssp), (lon - ssp) + 360.);
    }

    private static double geoEvalGeoDlat(double lat, double earthPolarRadiusMeter, double earthPolarMeter) {
        double dbRadEq = (earthPolarMeter - earthPolarRadiusMeter) / earthPolarMeter;
        final double latRad = MathUtils.DTOR * lat;
        final double diff = Math.abs(0.5 * Math.PI - Math.abs(latRad));

        if (diff < GEO_DOUBLE_EPSILON) {
            return latRad;
        } else {
            dbRadEq = (1. - dbRadEq) * (1. - dbRadEq);
            return MathUtils.RTOD * Math.atan(Math.tan(latRad) * dbRadEq);
        }
    }

    private static double geoPixRadius(double lat, double earthPolarRadiusMeter, double earthPolarMeter) {
        final double dbExc = Math.sqrt((earthPolarMeter * earthPolarMeter) -
                                               (earthPolarRadiusMeter * earthPolarRadiusMeter)) / (earthPolarMeter);
        final double dbExc2 = dbExc * dbExc;
        final double dbExc4 = dbExc2 * dbExc2;

        final double dbLatRad = MathUtils.DTOR * lat;
        final double dbSp1 = Math.sin(dbLatRad);
        final double dbSp12 = dbSp1 * dbSp1;
        final double dbSp14 = dbSp12 * dbSp12;

        return EARTH_RADIUS_METER_MVIRI * (1. - (dbExc2 * dbSp12) / 2. + (dbExc4 * dbSp12) / 2. - (5. * dbSp14 * dbExc4) / 8.);
    }

    private static boolean isLeapYear(int year) {
        return ((year % 400) == 0) || (((year % 4) == 0) && ((year % 100) != 0));
    }


    static class MeteosatAngles {
        double zenith;
        double azimuth;

        public MeteosatAngles(double zenith, double azimuth) {
            this.azimuth = azimuth;
            this.zenith = zenith;
        }

        public double getZenith() {
            return zenith;
        }

        public void setZenith(double zenith) {
            this.zenith = zenith;
        }

        public double getAzimuth() {
            return azimuth;
        }

        public void setAzimuth(double azimuth) {
            this.azimuth = azimuth;
        }
    }


}
