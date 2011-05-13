package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;

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

    public void testGetSunZenith() {
        // run some test data with CreateLatLonGrid_v2_work.py and compare...

        int doy = 001;
        GeoPos geoPos = new GeoPos(47.3958f, 30.0f);
        double sunZenith = AlbedoInversionUtils.computeSza(geoPos, doy);
        assertEquals(70.426647, sunZenith, 0.3);

        doy = 129;
        geoPos = new GeoPos(35.3958f, 76.0f);
        sunZenith = AlbedoInversionUtils.computeSza(geoPos, doy);
        assertEquals(18.2188, sunZenith, 0.3);

        doy = 289;
        geoPos = new GeoPos(85.3958f, -178.5f);
        sunZenith = AlbedoInversionUtils.computeSza(geoPos, doy);
        assertEquals(95.2706, sunZenith, 0.3);
    }

}
