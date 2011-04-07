package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import junit.framework.TestCase;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

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

    public void testGetDoyFromPriorName() throws Exception {
        String priorName = "Kernels_105_005_h18v04_backGround_NoSnow.hdr";
        assertEquals(105, AlbedoInversionUtils.getDoyFromPriorName(priorName));

        priorName = "bla.hdr";
        try {
            AlbedoInversionUtils.getDoyFromPriorName(priorName);
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid prior name bla.hdr", e.getMessage());
        }

        priorName = "Kernels_999_005_h18v04_backGround_Snow.hdr";
        try {
            AlbedoInversionUtils.getDoyFromPriorName(priorName);
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid doy 999 retrieved from prior name Kernels_999_005_h18v04_backGround_Snow.hdr",
                         e.getMessage());
        }
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
        m.set(1,2, Double.NaN);
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

}
