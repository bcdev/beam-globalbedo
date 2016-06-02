package org.esa.beam.globalbedo.bbdr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MeteosatGeometryTest {

    @Test
    public void testComputeSunAngles() {
        // test case from README in QA4ECV_EUMETSAT_Geometry delivery by AL:
        double flDegLat = 42.0;
        double flDegLon = 23.0;
        double dbSSP = 0.0;
        int year = 2010;
        int iJulianDayInYear = 121;
        double flScanningTime = 12.0;

        MeteosatGeometry.MeteosatAngles meteosatAngles =
                MeteosatGeometry.computeSunAngles(flDegLat, flDegLon, dbSSP, iJulianDayInYear, year, flScanningTime);
        assertEquals(33.666382, meteosatAngles.getZenith(), 1.E-3);
        assertEquals(224.603928, meteosatAngles.getAzimuth(), 1.E-3);

        // test for 57deg sat position:
        flDegLat = 62.0;
        flDegLon = 3.0;
        dbSSP = 57.0;
        year = 2010;
        iJulianDayInYear = 121;
        flScanningTime = 12.0;

        meteosatAngles =
                MeteosatGeometry.computeSunAngles(flDegLat, flDegLon, dbSSP, iJulianDayInYear, year, flScanningTime);
        assertEquals(46.794971, meteosatAngles.getZenith(), 1.E-3);
        assertEquals(185.031372, meteosatAngles.getAzimuth(), 1.E-3);

        // test for 63deg sat position:
        flDegLat = 7.2;
        flDegLon = 73.0;
        dbSSP = 63.0;
        year = 2010;
        iJulianDayInYear = 121;
        flScanningTime = 12.0;

        meteosatAngles =
                MeteosatGeometry.computeSunAngles(flDegLat, flDegLon, dbSSP, iJulianDayInYear, year, flScanningTime);
        assertEquals(72.539825, meteosatAngles.getZenith(), 1.E-3);
        assertEquals(283.814850, meteosatAngles.getAzimuth(), 1.E-3);
    }

    @Test
    public void testComputeViewAngles() {
        // test case from README in QA4ECV_EUMETSAT_Geometry delivery by AL:
        double flSatLat = 0.0;
        double flDegLat = 42.0;
        double flDegLon = 23.0;
        double dbSSP = 0.0;

        MeteosatGeometry.MeteosatAngles meteosatAngles =
                MeteosatGeometry.computeViewAngles(flSatLat, dbSSP, flDegLat, flDegLon, MeteosatSensor.MVIRI);
        assertEquals(53.664982, meteosatAngles.getZenith(), 1.E-3);
        assertEquals(212.410950, meteosatAngles.getAzimuth(), 1.E-3);

        // test for 57deg sat position:
        flSatLat = 0.0;
        flDegLat = 62.0;
        flDegLon = 3.0;
        dbSSP = 57.0;

        meteosatAngles =
                MeteosatGeometry.computeViewAngles(flSatLat, dbSSP, flDegLat, flDegLon, MeteosatSensor.MVIRI);
        assertEquals(82.498001, meteosatAngles.getZenith(), 1.E-3);
        assertEquals(122.659012, meteosatAngles.getAzimuth(), 1.E-3);

        // test for 63deg sat position:
        flSatLat = 0.0;
        flDegLat = 7.2;
        flDegLon = 73.0;
        dbSSP = 63.0;

        meteosatAngles =
                MeteosatGeometry.computeViewAngles(flSatLat, dbSSP, flDegLat, flDegLon, MeteosatSensor.MVIRI);
        assertEquals(14.434147, meteosatAngles.getZenith(), 1.E-3);
        assertEquals(234.622391, meteosatAngles.getAzimuth(), 1.E-3);

        // test for SEVIRI:
        flSatLat = 0.0;
        flDegLat = 13.4;
        flDegLon = 67.3;
        dbSSP = 63.0;
        meteosatAngles =
                MeteosatGeometry.computeViewAngles(flSatLat, dbSSP, flDegLat, flDegLon, MeteosatSensor.SEVIRI);
        assertEquals(16.429684, meteosatAngles.getZenith(), 1.E-3);
        assertEquals(197.992401, meteosatAngles.getAzimuth(), 1.E-3);
    }
}
