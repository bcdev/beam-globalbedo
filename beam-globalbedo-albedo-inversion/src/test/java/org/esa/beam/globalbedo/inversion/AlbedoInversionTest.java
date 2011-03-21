package org.esa.beam.globalbedo.inversion;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;

import java.util.List;
import java.util.Set;

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
}
