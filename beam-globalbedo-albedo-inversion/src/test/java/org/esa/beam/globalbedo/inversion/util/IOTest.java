package org.esa.beam.globalbedo.inversion.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class IOTest extends TestCase {

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

    public void testGetInversionParameterBandnames() throws Exception {
        String[] bandNames = IOUtils.getInversionParameterBandNames();
        assertNotNull(bandNames);
        assertEquals(9, bandNames.length);
        assertEquals("mean_VIS_f0", bandNames[0]);
        assertEquals("mean_VIS_f1", bandNames[1]);
        assertEquals("mean_VIS_f2", bandNames[2]);
        assertEquals("mean_NIR_f0", bandNames[3]);
        assertEquals("mean_NIR_f1", bandNames[4]);
        assertEquals("mean_NIR_f2", bandNames[5]);
        assertEquals("mean_SW_f0", bandNames[6]);
        assertEquals("mean_SW_f1", bandNames[7]);
        assertEquals("mean_SW_f2", bandNames[8]);
    }

    public void testGetInversionUncertaintyBandnames() throws Exception {
        String[][] bandNames = IOUtils.getInversionUncertaintyBandNames();
        assertNotNull(bandNames);
        assertEquals(9, bandNames.length);
        assertEquals(9, bandNames[0].length);

        assertEquals("VAR_VIS_f0_VIS_f0", bandNames[0][0]);
        assertEquals("VAR_VIS_f0_VIS_f1", bandNames[0][1]);
        assertEquals("VAR_VIS_f0_VIS_f2", bandNames[0][2]);
        assertEquals("VAR_VIS_f0_NIR_f0", bandNames[0][3]);
        assertEquals("VAR_VIS_f0_NIR_f1", bandNames[0][4]);
        assertEquals("VAR_VIS_f0_NIR_f2", bandNames[0][5]);
        assertEquals("VAR_VIS_f0_SW_f0", bandNames[0][6]);
        assertEquals("VAR_VIS_f0_SW_f1", bandNames[0][7]);
        assertEquals("VAR_VIS_f0_SW_f2", bandNames[0][8]);

        assertEquals("VAR_VIS_f1_VIS_f1", bandNames[1][1]);
        assertEquals("VAR_VIS_f1_VIS_f2", bandNames[1][2]);
        assertEquals("VAR_VIS_f1_NIR_f0", bandNames[1][3]);
        assertEquals("VAR_VIS_f1_NIR_f1", bandNames[1][4]);
        assertEquals("VAR_VIS_f1_NIR_f2", bandNames[1][5]);
        assertEquals("VAR_VIS_f1_SW_f0", bandNames[1][6]);
        assertEquals("VAR_VIS_f1_SW_f1", bandNames[1][7]);
        assertEquals("VAR_VIS_f1_SW_f2", bandNames[1][8]);

        assertEquals("VAR_VIS_f2_VIS_f2", bandNames[2][2]);
        assertEquals("VAR_VIS_f2_NIR_f0", bandNames[2][3]);
        assertEquals("VAR_VIS_f2_NIR_f1", bandNames[2][4]);
        assertEquals("VAR_VIS_f2_NIR_f2", bandNames[2][5]);
        assertEquals("VAR_VIS_f2_SW_f0", bandNames[2][6]);
        assertEquals("VAR_VIS_f2_SW_f1", bandNames[2][7]);
        assertEquals("VAR_VIS_f2_SW_f2", bandNames[2][8]);

        assertEquals("VAR_NIR_f0_NIR_f0", bandNames[3][3]);
        assertEquals("VAR_NIR_f0_NIR_f1", bandNames[3][4]);
        assertEquals("VAR_NIR_f0_NIR_f2", bandNames[3][5]);
        assertEquals("VAR_NIR_f0_SW_f0", bandNames[3][6]);
        assertEquals("VAR_NIR_f0_SW_f1", bandNames[3][7]);
        assertEquals("VAR_NIR_f0_SW_f2", bandNames[3][8]);

        assertEquals("VAR_NIR_f1_NIR_f1", bandNames[4][4]);
        assertEquals("VAR_NIR_f1_NIR_f2", bandNames[4][5]);
        assertEquals("VAR_NIR_f1_SW_f0", bandNames[4][6]);
        assertEquals("VAR_NIR_f1_SW_f1", bandNames[4][7]);
        assertEquals("VAR_NIR_f1_SW_f2", bandNames[4][8]);

        assertEquals("VAR_NIR_f2_NIR_f2", bandNames[5][5]);
        assertEquals("VAR_NIR_f2_SW_f0", bandNames[5][6]);
        assertEquals("VAR_NIR_f2_SW_f1", bandNames[5][7]);
        assertEquals("VAR_NIR_f2_SW_f2", bandNames[5][8]);

        assertEquals("VAR_SW_f0_SW_f0", bandNames[6][6]);
        assertEquals("VAR_SW_f0_SW_f1", bandNames[6][7]);
        assertEquals("VAR_SW_f0_SW_f2", bandNames[6][8]);

        assertEquals("VAR_SW_f1_SW_f1", bandNames[7][7]);
        assertEquals("VAR_SW_f1_SW_f2", bandNames[7][8]);

        assertEquals("VAR_SW_f2_SW_f2", bandNames[8][8]);

        // we expect nulls for j < i
        assertNull(bandNames[4][2]);
        assertNull(bandNames[5][4]);
        assertNull(bandNames[6][1]);
    }

    private File setTestfile(String filename) throws UnsupportedEncodingException {
        final URL url = IOUtils.class.getResource(filename);
        assertNotNull(url);
        assertEquals("file", url.getProtocol());

        final String path = URLDecoder.decode(url.getPath(), "UTF-8");
        assertTrue(path.endsWith(filename));

        final File file = new File(path);

        assertEquals(file.getName(), filename);
        return file;
    }

    public void testByteBufferWriteRead() throws Exception {

        final File file = setTestfile("testDailyAccBin.acc");
        if (!file.exists()) {
            file.createNewFile();
        }
//        // write double array to binary file...
        final int dim1 = 92;
        final int dim2 = 120;
        final int dim3 = 120;
        double[][][] testArray = new double[dim1][dim2][dim3];

        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                for (int k = 0; k < dim3; k++) {
                    testArray[i][j][k] = i * 1.0 + j * 2.0 + k * 3.0;
                }
            }
        }

        long time1 = System.currentTimeMillis();

        IOUtils.writeDoubleArrayToFile(file, testArray);
        long time2 = System.currentTimeMillis();
        System.out.println("time for writing doubles: " + (time2 - time1) / 1000.0);

        // read 3D binary array back from file...
        long time3 = System.currentTimeMillis();
        double[] result = IOUtils.readDoubleArrayFromFile(file, dim1, dim2, dim3);
        long time4 = System.currentTimeMillis();
        System.out.println("time for reading doubles: " + (time4 - time3) / 1000.0);
        assertEquals(dim1 * dim2 * dim3, result.length);
        int index = 0;
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                for (int k = 0; k < dim3; k++) {
                    assertEquals(i * 1.0 + j * 2.0 + k * 3.0, result[index++]);
                }
            }
        }
    }

    public void testByteBufferWriteReadFloats() throws Exception {

        final File file = setTestfile("testDailyAccBinFloats.acc");
        if (!file.exists()) {
            file.createNewFile();
        }
//        // write double array to binary file...
        final int dim1 = 92;
        final int dim2 = 120;
        final int dim3 = 120;
        float[][][] testArray = new float[dim1][dim2][dim3];

        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                for (int k = 0; k < dim3; k++) {
                    testArray[i][j][k] = (i * 1.0f + j * 2.0f + k * 3.0f)*1.E-10f;
                }
            }
        }

        long time1 = System.currentTimeMillis();

        IOUtils.writeFloatArrayToFile(file, testArray);
        long time2 = System.currentTimeMillis();
        System.out.println("time for writing floats: " + (time2 - time1) / 1000.0);

        // read 3D binary array back from file...
        long time3 = System.currentTimeMillis();
        float[] result = IOUtils.readFloatArrayFromFile(file, dim1, dim2, dim3);
        long time4 = System.currentTimeMillis();
        System.out.println("time for reading floats: " + (time4 - time3) / 1000.0);
        assertEquals(dim1 * dim2 * dim3, result.length);
        int index = 0;
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                for (int k = 0; k < dim3; k++) {
                    assertEquals((i * 1.0f + j * 2.0f + k * 3.0f)*1.E-10f, result[index++]);
                }
            }
        }
    }
}
