package org.esa.beam.globalbedo.inversion.util;

import junit.framework.TestCase;
import org.esa.beam.globalbedo.inversion.GlobalbedoLevel3Albedo;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class IOTest extends TestCase {

    private String testResourcePath;

    protected void setUp() {
        try {
            final URL url = GlobalbedoLevel3Albedo.class.getResource("");
            testResourcePath = URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (IOException e) {
            fail("Test resource data could not be loaded: " + e.getMessage());
        }
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
        List<String> day1ProductNames = IOUtils.getDailyBBDRFilenames(bbdrFilenames, day1String);
        assertNotNull(day1ProductNames);
        assertEquals(1, day1ProductNames.size());
        assertEquals("Meris_20050101_080302_BBDR.dim", day1ProductNames.get(0));

        String day2String = "20050317";
        List<String> day2ProductNames = IOUtils.getDailyBBDRFilenames(bbdrFilenames, day2String);
        assertNotNull(day2ProductNames);
        assertEquals(3, day2ProductNames.size());
        assertEquals("Meris_20050317_072032_BBDR.dim", day2ProductNames.get(0));
        assertEquals("Meris_20050317_130356_BBDR.dim", day2ProductNames.get(1));
        assertEquals("Meris_20050317_151302_BBDR.dim", day2ProductNames.get(2));

        String day3String = "20051113";
        List<String> day3ProductNames = IOUtils.getDailyBBDRFilenames(bbdrFilenames, day3String);
        assertNotNull(day3ProductNames);
        assertEquals(2, day3ProductNames.size());
        assertEquals("Meris_20051113_080302_BBDR.dim", day3ProductNames.get(0));
        assertEquals("Meris_20051113_182241_BBDR.dim", day3ProductNames.get(1));

        String day4String = "20051231";
        List<String> day4ProductNames = IOUtils.getDailyBBDRFilenames(bbdrFilenames, day4String);
        assertNotNull(day4ProductNames);
        assertEquals(0, day4ProductNames.size());
    }

    public void testGetPriorProductNames() throws Exception {
        String[] priorDirContent = new String[]{
                "Kernels_105_005_h18v04_backGround_NoSnow.bin",
                "Kernels_105_005_h18v04_backGround_NoSnow.hdr",
                "Kernels_117_005_h18v04_backGround_NoSnow.bin",
                "Kernels_117_005_h18v04_backGround_NoSnow.hdr",
                "Kernels_129_005_h18v04_backGround_NoSnow.bin",
                "Kernels_129_005_h18v04_backGround_NoSnow.hdr",
                "Kernels_136_005_h18v04_backGround_Snow.bin",
                "Kernels_148_005_h18v04_backGround_Snow.bin",
                "Kernels_160_005_h18v04_backGround_Snow.bin",
                "Kernels_136_005_h18v04_backGround_Snow.hdr",
                "Kernels_148_005_h18v04_backGround_Snow.hdr",
                "blubb.txt",
                "bla.dat"
        };

        List<String> priorProductNames = IOUtils.getPriorProductNames(priorDirContent, false);
        assertNotNull(priorProductNames);
        assertEquals(3, priorProductNames.size());
        assertEquals("Kernels_105_005_h18v04_backGround_NoSnow.hdr", priorProductNames.get(0));
        assertEquals("Kernels_117_005_h18v04_backGround_NoSnow.hdr", priorProductNames.get(1));
        assertEquals("Kernels_129_005_h18v04_backGround_NoSnow.hdr", priorProductNames.get(2));
    }

    public void testGetInversionTargetFileName() throws Exception {
        final int year = 2005;
        final int doy = 123;
        final String tile = "h18v04";

        boolean computeSnow = true;
        boolean usePrior = true;

        String targetFileName = IOUtils.getInversionTargetFileName(year, doy, tile, computeSnow, usePrior);
        assertNotNull(targetFileName);
        assertEquals("GlobAlbedo.2005123.h18v04.Snow.bin", targetFileName);

        computeSnow = false;
        targetFileName = IOUtils.getInversionTargetFileName(year, doy, tile, computeSnow, usePrior);
        assertEquals("GlobAlbedo.2005123.h18v04.NoSnow.bin", targetFileName);

        usePrior = false;
        targetFileName = IOUtils.getInversionTargetFileName(year, doy, tile, computeSnow, usePrior);
        assertEquals("GlobAlbedo.2005123.h18v04.NoSnow.NoPrior.bin", targetFileName);

        computeSnow = true;
        targetFileName = IOUtils.getInversionTargetFileName(year, doy, tile, computeSnow, usePrior);
        assertEquals("GlobAlbedo.2005123.h18v04.Snow.NoPrior.bin", targetFileName);
    }
}
