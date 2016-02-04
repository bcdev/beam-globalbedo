package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;
import org.junit.Test;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInversionTest extends TestCase {

    public void testGetDateFromDoy() {
        int year = 2005;
        int doy = 129;
        assertEquals("20050509", AlbedoInversionUtils.getDateFromDoy(year, doy));
        doy = 1;
        assertEquals("20050101", AlbedoInversionUtils.getDateFromDoy(year, doy));
        doy = 365;
        assertEquals("20051231", AlbedoInversionUtils.getDateFromDoy(year, doy));

        year = 2004;
        doy = 129;
        assertEquals("20040508", AlbedoInversionUtils.getDateFromDoy(year, doy));
        doy = 366;
        assertEquals("20041231", AlbedoInversionUtils.getDateFromDoy(year, doy));
    }

    public void testGetDiagonalMatrix() {
        Matrix m = new Matrix(3, 3);
        m.set(0, 0, 2.0);
        m.set(1, 0, 4.0);
        m.set(2, 0, 6.0);
        m.set(0, 1, 8.0);
        m.set(1, 1, 3.0);
        m.set(2, 1, 5.0);
        m.set(0, 2, 7.0);
        m.set(1, 2, 9.0);
        m.set(2, 2, 12.0);

        Matrix diag = AlbedoInversionUtils.getRectangularDiagonalMatrix(m);
        assertNotNull(diag);
        assertEquals(3, diag.getRowDimension());
        assertEquals(3, diag.getColumnDimension());
        assertEquals(2.0, diag.get(0, 0));
        assertEquals(0.0, diag.get(1, 0));
        assertEquals(0.0, diag.get(2, 0));
        assertEquals(0.0, diag.get(0, 1));
        assertEquals(3.0, diag.get(1, 1));
        assertEquals(0.0, diag.get(2, 1));
        assertEquals(0.0, diag.get(0, 2));
        assertEquals(0.0, diag.get(1, 2));
        assertEquals(12.0, diag.get(2, 2));
    }

    public void testGetDiagonalFlatMatrix() {
        Matrix m = new Matrix(3, 3);
        m.set(0, 0, 2.0);
        m.set(1, 0, 4.0);
        m.set(2, 0, 6.0);
        m.set(0, 1, 8.0);
        m.set(1, 1, 3.0);
        m.set(2, 1, 5.0);
        m.set(0, 2, 7.0);
        m.set(1, 2, 9.0);
        m.set(2, 2, 12.0);

        Matrix diagFlat = AlbedoInversionUtils.getRectangularDiagonalFlatMatrix(m);
        assertNotNull(diagFlat);
        assertEquals(3, diagFlat.getRowDimension());
        assertEquals(1, diagFlat.getColumnDimension());
        assertEquals(2.0, diagFlat.get(0, 0));
        assertEquals(3.0, diagFlat.get(1, 0));
        assertEquals(12.0, diagFlat.get(2, 0));
    }

    public void testGetReciprocalMatrix() {
        Matrix m = new Matrix(3, 3);
        m.set(0, 0, 2.0);
        m.set(1, 0, 4.0);
        m.set(2, 0, 6.0);
        m.set(0, 1, 8.0);
        m.set(1, 1, 3.0);
        m.set(2, 1, 5.0);
        m.set(0, 2, 7.0);
        m.set(1, 2, 9.0);
        m.set(2, 2, 12.0);

        Matrix recip = AlbedoInversionUtils.getReciprocalMatrix(m);
        assertNotNull(recip);
        assertEquals(3, recip.getRowDimension());
        assertEquals(3, recip.getColumnDimension());
        assertEquals(0.5, recip.get(0, 0));
        assertEquals(0.125, recip.get(0, 1));
        assertEquals(0.11111, recip.get(1, 2), 1.E-4);
    }

    public void testGetMatrixAllElementsProduct() {
        Matrix m = new Matrix(3, 3);
        m.set(0, 0, 2.0);
        m.set(1, 0, 4.0);
        m.set(2, 0, 6.0);
        m.set(0, 1, 8.0);
        m.set(1, 1, 3.0);
        m.set(2, 1, 5.0);
        m.set(0, 2, 7.0);
        m.set(1, 2, 9.0);
        m.set(2, 2, 12.0);

        double product = AlbedoInversionUtils.getMatrixAllElementsProduct(m);
        assertEquals(4354560.0, product);
    }


    public void testMatrixHasNaNElements() throws Exception {
        Matrix m = new Matrix(3, 3);
        m.set(0, 0, 2.0);
        m.set(1, 0, 4.0);
        m.set(2, 0, 6.0);
        m.set(0, 1, 8.0);
        m.set(1, 1, 3.0);
        m.set(2, 1, 5.0);
        m.set(0, 2, 7.0);
        m.set(1, 2, 9.0);
        m.set(2, 2, 12.0);

        assertFalse(AlbedoInversionUtils.matrixHasNanElements(m));
        m.set(1, 2, Double.NaN);
        assertTrue(AlbedoInversionUtils.matrixHasNanElements(m));
    }

    public void testMatrixHasZerosInDiagonale() throws Exception {
        Matrix m = new Matrix(3, 3);
        m.set(0, 0, 2.0);
        m.set(1, 1, 4.0);
        m.set(2, 2, 6.0);

        assertFalse(AlbedoInversionUtils.matrixHasZerosInDiagonale(m));
        m.set(2, 2, 0.0);
        assertTrue(AlbedoInversionUtils.matrixHasZerosInDiagonale(m));
    }

    public void testGetUpperLeftCornerOfModisTiles() throws Exception {
        String tile = "h18v04";
        assertEquals(0.0, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[0], 1.E-3);
        assertEquals(5559752.598333000205457, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[1], 1.E-3);

        tile = "h19v08";
        assertEquals(1111950.519667000044137, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[0], 1.E-3);
        assertEquals(1111950.519667000044137, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[1], 1.E-3);

        tile = "h22v02";
        assertEquals(4447802.078666999936104, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[0], 1.E-3);
        assertEquals(7783653.637667000293732, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[1], 1.E-3);

        tile = "h25v06";
        assertEquals(7783653.637667000293732, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[0], 1.E-3);
        assertEquals(3335851.558999999891967, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[1], 1.E-3);

        tile = "h13v14";
        assertEquals(-5559752.598333000205457, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[0], 1.E-3);
        assertEquals(-5559752.598333000205457, AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[1], 1.E-3);
    }

    @Test
    public void testGetModisTileFromLatLon() {
        // Texas
        float lat = 34.2f;
        float lon = -101.71f;
        assertEquals("h09v05", AlbedoInversionUtils.getModisTileFromLatLon(lat, lon));

        // MeckPomm
        lat = 53.44f;
        lon = 10.57f;
        assertEquals("h18v03", AlbedoInversionUtils.getModisTileFromLatLon(lat, lon));

        // Barrow (a left edge tile)
        lat = 65.0f;
        lon = -175.0f;
        assertEquals("h10v02", AlbedoInversionUtils.getModisTileFromLatLon(lat, lon));

        // New Zealand (a right edged tile)
        lat = -39.5f;
        lon = 176.71f;
        assertEquals("h31v12", AlbedoInversionUtils.getModisTileFromLatLon(lat, lon));


        // Antarctica
        lat = -84.2f;
        lon = 160.71f;
        assertEquals("h19v17", AlbedoInversionUtils.getModisTileFromLatLon(lat, lon));

        // Siberia
        lat = 65.2f;
        lon = 111.71f;
        assertEquals("h22v02", AlbedoInversionUtils.getModisTileFromLatLon(lat, lon));

        // Madagascar
        lat = -28.0f;
        lon = 46.1f;
        assertEquals("h22v11", AlbedoInversionUtils.getModisTileFromLatLon(lat, lon));
    }

    public void testGetSunZenith() {
        // run some test data with CreateLatLonGrid_v2_work.py and compare...

        int doy = 1;
        GeoPos geoPos = new GeoPos(47.3958f, 30.0f);
        double sunZenith = AlbedoInversionUtils.computeSza(geoPos, doy);
        assertEquals(70.426647, sunZenith, 0.3);

        doy = 129;
        geoPos = new GeoPos(35.3958f, 76.0f);
        sunZenith = AlbedoInversionUtils.computeSza(geoPos, doy);
        assertEquals(18.2188, sunZenith, 0.3);

        doy = 129;
        geoPos = new GeoPos(50.0f, 0.0f);
        sunZenith = AlbedoInversionUtils.computeSza(geoPos, doy);
        assertEquals(32.823, sunZenith, 0.3);

        doy = 289;
        geoPos = new GeoPos(85.3958f, -178.5f);
        sunZenith = AlbedoInversionUtils.computeSza(geoPos, doy);
        assertEquals(95.2706, sunZenith, 0.3);
    }

    public void testCheckValidValues() {
        double a = -9999.0;
        assertFalse(AlbedoInversionUtils.isValid(a));
        a = Double.NaN;
        assertFalse(AlbedoInversionUtils.isValid(a));
        a = 123.4;
        assertTrue(AlbedoInversionUtils.isValid(a));

        float b = -9999.0f;
        assertFalse(AlbedoInversionUtils.isValid(b));
        b = Float.NaN;
        assertFalse(AlbedoInversionUtils.isValid(b));
        b = 123.4f;
        assertTrue(AlbedoInversionUtils.isValid(b));
    }

    public void testComputeDayOffset() {
        int year = 2005;
        int doy = 13;
        int referenceYear = 2005;
        int referenceDoy = 121;
        int[] referenceDate = new int[]{referenceYear, referenceDoy};
        int doyOffset = AlbedoInversionUtils.computeDayOffset(referenceDate, year, doy);
        assertEquals(108, doyOffset);

        year = 2006;
        doy = 27;
        doyOffset = AlbedoInversionUtils.computeDayOffset(referenceDate, year, doy);
        assertEquals(271, doyOffset);

        year = 2004;
        doy = 279;
        doyOffset = AlbedoInversionUtils.computeDayOffset(referenceDate, year, doy);
        assertEquals(207, doyOffset);
    }

    public void testGetReferenceDate() throws Exception {
        int doy = 123;
        int year = 2005;
        int[] referenceDate = AlbedoInversionUtils.getReferenceDate(year, doy);
        assertEquals(2005, referenceDate[0]);
        assertEquals(123, referenceDate[1]);

        doy = -73;
        referenceDate = AlbedoInversionUtils.getReferenceDate(year, doy);
        assertEquals(2004, referenceDate[0]);
        assertEquals(292, referenceDate[1]);

        doy = 403;
        referenceDate = AlbedoInversionUtils.getReferenceDate(year, doy);
        assertEquals(2006, referenceDate[0]);
        assertEquals(38, referenceDate[1]);
    }

    public void testGetReferenceDate2() throws Exception {
        int doy = 123;
        int year = 2005;
        int dayOffset = 3;
        int[] referenceDate = AlbedoInversionUtils.getReferenceDate(year, doy, dayOffset);
        assertEquals(2005, referenceDate[0]);
        assertEquals(126, referenceDate[1]);

        dayOffset = -240;
        referenceDate = AlbedoInversionUtils.getReferenceDate(year, doy, dayOffset);
        assertEquals(2004, referenceDate[0]);
        assertEquals(248, referenceDate[1]);

        dayOffset = 240;
        referenceDate = AlbedoInversionUtils.getReferenceDate(year, doy, dayOffset);
        assertEquals(2005, referenceDate[0]);
        assertEquals(363, referenceDate[1]);

        doy = 1;
        dayOffset = -240;
        referenceDate = AlbedoInversionUtils.getReferenceDate(year, doy, dayOffset);
        assertEquals(2004, referenceDate[0]);
        assertEquals(126, referenceDate[1]);

        doy = 361;
        dayOffset = 240;
        referenceDate = AlbedoInversionUtils.getReferenceDate(year, doy, dayOffset);
        assertEquals(2006, referenceDate[0]);
        assertEquals(236, referenceDate[1]);
    }

}
