package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInversionUtilsTest extends TestCase {

    public void testGetDateFromDoy() {
        int year = 2005;
        int doy = 129;
        assertEquals("20050509", AlbedoInversionUtils.getDateFromDoy(year, doy));
        assertEquals("2005_05_09", AlbedoInversionUtils.getDateFromDoy(year, doy, "yyyy_MM_dd"));
        doy = 1;
        assertEquals("20050101", AlbedoInversionUtils.getDateFromDoy(year, doy));
        assertEquals("2005-01-01", AlbedoInversionUtils.getDateFromDoy(year, doy, "yyyy-MM-dd"));
        doy = 365;
        assertEquals("20051231", AlbedoInversionUtils.getDateFromDoy(year, doy));
        assertEquals("2005_12_31", AlbedoInversionUtils.getDateFromDoy(year, doy, "yyyy_MM_dd"));

        year = 2004;
        doy = 129;
        assertEquals("20040508", AlbedoInversionUtils.getDateFromDoy(year, doy));
        assertEquals("2004-05-08", AlbedoInversionUtils.getDateFromDoy(year, doy, "yyyy-MM-dd"));
        doy = 366;
        assertEquals("20041231", AlbedoInversionUtils.getDateFromDoy(year, doy));
        assertEquals("2004-12-31", AlbedoInversionUtils.getDateFromDoy(year, doy, "yyyy-MM-dd"));
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

}
