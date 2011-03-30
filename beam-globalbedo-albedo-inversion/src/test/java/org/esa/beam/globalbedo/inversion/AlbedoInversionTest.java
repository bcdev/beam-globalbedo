package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import junit.framework.TestCase;

import java.util.List;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInversionTest extends TestCase {

    public void testSomething() {
        // todo: implement tests
        assertTrue(true);
    }

    public void testGetDailyBBDRFiles() {
        String[] bbdrFilenames = new String[]{
                "Meris_20050101_080302_BBDR.dim",
                "Meris_20050317_072032_BBDR.dim",
                "Meris_20050317_130356_BBDR.dim",
                "Meris_20050317_151302_BBDR.dim",
                "Meris_20051113_182241_BBDR.dim",
                "Meris_20051113_080302_BBDR.dim"
        };

        String day1String = "20050101";
        List<String> day1ProductNames = AlbedoInversionUtils.getDailyBBDRFilenames(bbdrFilenames, day1String);
        assertNotNull(day1ProductNames);
        assertEquals(1, day1ProductNames.size());
        assertEquals("Meris_20050101_080302_BBDR.dim", day1ProductNames.get(0));

        String day2String = "20050317";
        List<String> day2ProductNames = AlbedoInversionUtils.getDailyBBDRFilenames(bbdrFilenames, day2String);
        assertNotNull(day2ProductNames);
        assertEquals(3, day2ProductNames.size());
        assertEquals("Meris_20050317_072032_BBDR.dim", day2ProductNames.get(0));
        assertEquals("Meris_20050317_130356_BBDR.dim", day2ProductNames.get(1));
        assertEquals("Meris_20050317_151302_BBDR.dim", day2ProductNames.get(2));

        String day3String = "20051113";
        List<String> day3ProductNames = AlbedoInversionUtils.getDailyBBDRFilenames(bbdrFilenames, day3String);
        assertNotNull(day3ProductNames);
        assertEquals(2, day3ProductNames.size());
        assertEquals("Meris_20051113_182241_BBDR.dim", day3ProductNames.get(0));
        assertEquals("Meris_20051113_080302_BBDR.dim", day3ProductNames.get(1));

        String day4String = "20051231";
        List<String> day4ProductNames = AlbedoInversionUtils.getDailyBBDRFilenames(bbdrFilenames, day4String);
        assertNotNull(day4ProductNames);
        assertEquals(0, day4ProductNames.size());
    }

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

    public void testGetPriorProductNames() throws Exception {
        // todo: implement, write test
    }

    public void testGetInputProductNames() throws Exception {
        // todo: implement, write test
    }
}
