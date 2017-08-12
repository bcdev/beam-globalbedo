package org.esa.beam.globalbedo.inversion.util;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Ignore;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class IOUtilsTest extends TestCase {

    private int dim1;
    private int dim2;
    private File testfile;
    private File file_h18v04;
    private File file_h25v06;
    private String rootDir;

    @Override
    public void setUp() throws Exception {
        rootDir = System.getProperty("user.home");
        dim1 = 1200;
        dim2 = 1200;
        testfile = new File(System.getProperty("user.home") + File.separator + "muell.txt");
        testfile.createNewFile();

        file_h18v04 = new File(rootDir + File.separator + "h18v04");
        file_h18v04.mkdir();
        file_h25v06 = new File(rootDir + File.separator + "h25v06");
        file_h25v06.mkdir();
    }

    @Override
    public void tearDown() throws Exception {
        if (testfile != null && testfile.exists()) {
            testfile.deleteOnExit();
        }
        if (file_h18v04 != null && file_h18v04.exists()) {
            file_h18v04.deleteOnExit();
        }
        if (file_h25v06 != null && file_h25v06.exists()) {
            file_h25v06.deleteOnExit();
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

    public void testGetPriorProductNamesCollection5() throws Exception {
        String[] priorDirContent = new String[]{
                "Kernels.105.005.h18v04.backGround.NoSnow.bin",
                "Kernels.105.005.h18v04.backGround.NoSnow.hdr",
                "Kernels.117.005.h18v04.backGround.NoSnow.bin",
                "Kernels.117.005.h18v04.backGround.NoSnow.hdr",
                "Kernels.129.005.h18v04.backGround.NoSnow.bin",
                "Kernels.129.005.h18v04.backGround.NoSnow.hdr",
                "Kernels.136.005.h18v04.backGround.Snow.bin",
                "Kernels.148.005.h18v04.backGround.Snow.bin",
                "Kernels.160.005.h18v04.backGround.Snow.bin",
                "Kernels.136.005.h18v04.backGround.Snow.hdr",
                "Kernels.148.005.h18v04.backGround.Snow.hdr",
                "blubb.txt",
                "bla.dat"
        };

        final int priorVersion = 5;
        List<String> priorProductNames = IOUtils.getPriorProductNames(priorVersion, priorDirContent, false);
        assertNotNull(priorProductNames);
        assertEquals(3, priorProductNames.size());
        assertEquals("Kernels.105.005.h18v04.backGround.NoSnow.hdr", priorProductNames.get(0));
        assertEquals("Kernels.117.005.h18v04.backGround.NoSnow.hdr", priorProductNames.get(1));
        assertEquals("Kernels.129.005.h18v04.backGround.NoSnow.hdr", priorProductNames.get(2));
    }

    public void testGetPriorProductNamesCollection6() throws Exception {
        String[] priorDirContent = new String[]{
                "prior.modis.c6.121.h18v04.snownosnow.stage2.nc",
                "prior.modis.c6.121.h18v04.snownosnow.stage2.dim",
                "prior.modis.c6.121.h18v04.nosnow.stage2.nc",
                "prior.modis.c6.121.h18v04.snow.stage2.nc",
                "Kernels.105.005.h18v04.backGround.NoSnow.hdr",
                "blubb.txt",
                "bla.dat"
        };

        final int priorVersion = 6;
        List<String> priorProductNames = IOUtils.getPriorProductNames(priorVersion, priorDirContent, false);
        assertNotNull(priorProductNames);
        assertEquals(1, priorProductNames.size());
        assertEquals("prior.modis.c6.121.h18v04.nosnow.stage2.nc", priorProductNames.get(0));
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

    public void testGetDayDifference() throws Exception {
        int year = 2005;
        int refYear = 2005;
        int doy = 129;
        int refDoy = 123;
        assertEquals(6, IOUtils.getDayDifference(doy, year, refDoy, refYear));
        doy = 119;
        refDoy = 139;
        assertEquals(20, IOUtils.getDayDifference(doy, year, refDoy, refYear));
        refYear = 2003;
        assertEquals(710, IOUtils.getDayDifference(doy, year, refDoy, refYear));
    }

    public void testGetDoyFromAlbedoProductName() throws Exception {
        String productName = "GlobAlbedo.albedo.2005129.h18v04.dim";
        assertEquals(129, IOUtils.getDoyFromAlbedoProductName(productName));

        productName = "GlobAlbedo.albedo.2005001.h18v04.dim";
        assertEquals(1, IOUtils.getDoyFromAlbedoProductName(productName));

        productName = "GlobAlbedo.albedo.2005999.h18v04.dim";
        assertEquals(-1, IOUtils.getDoyFromAlbedoProductName(productName));
    }

    public void testWingsCoverage() {
        int wings = 180;
        int processYear = 2005; // the reference year to process

        int processDoy = 121; // the reference doy to process
        String tile = "h18v04";
        String accName = "matrices_2005100.bin"; // the accumulator product name
        assertTrue(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2005169.bin"; // the accumulator product name
        assertTrue(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2004264.bin"; // the accumulator product name
        assertFalse(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2006002.bin"; // the accumulator product name
        assertFalse(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));

        processDoy = 1; // the reference doy to process
        accName = "matrices_2005088.bin"; // the accumulator product name
        assertTrue(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2004277.bin"; // the accumulator product name
        assertTrue(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2004176.bin"; // the accumulator product name
        assertFalse(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));

        processDoy = 361; // the reference doy to process
        accName = "matrices_2005288.bin"; // the accumulator product name
        assertTrue(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2006086.bin"; // the accumulator product name
        assertTrue(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2006187.bin"; // the accumulator product name
        assertFalse(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));

        tile = "h18v00";
        processDoy = 185;
        accName = "matrices_2005100.bin"; // the accumulator product name
        assertTrue(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2005009.bin"; // the accumulator product name
        assertTrue(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));
        accName = "matrices_2005001.bin"; // the accumulator product name
        assertFalse(IOUtils.isInWingsInterval(wings, processYear, processDoy, tile, accName));

    }

    public void testGetDoyString() throws Exception {
        int doy = 123;
        assertEquals("123", IOUtils.getDoyString(doy));
        doy = 59;
        assertEquals("059", IOUtils.getDoyString(doy));
        doy = 7;
        assertEquals("007", IOUtils.getDoyString(doy));
        doy = -1;
        assertNull(IOUtils.getDoyString(doy));
        doy = 367;
        assertNull(IOUtils.getDoyString(doy));
    }

//    public void testSortAccumulatorFileList_test30wingsfiles() {
//        List<String> inputList = new ArrayList<>();
//        for (int doy=275; doy<=365; doy++) {
//            inputList.add("matrices_2004" + String.format("%03d", doy) + ".bin");
//        }
//        for (int doy=0; doy<=365; doy++) {
//            inputList.add("matrices_2005" + String.format("%03d", doy) + ".bin");
//        }
//        for (int doy=0; doy<=90; doy++) {
//            inputList.add("matrices_2006" + String.format("%03d", doy) + ".bin");
//        }
//
//        List<String> result = IOUtils.sortAccumulatorFileList(inputList, 2005, 121);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2005106.bin", result.get(0));
//        assertEquals("matrices_2005135.bin", result.get(29));
//
//        result = IOUtils.sortAccumulatorFileList(inputList, 2005, 9);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2004359.bin", result.get(0));
//        assertEquals("matrices_2005023.bin", result.get(29));
//
//        result = IOUtils.sortAccumulatorFileList(inputList, 2005, 353);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2005338.bin", result.get(0));
//        assertEquals("matrices_2006001.bin", result.get(29));
//        System.out.println();
//
//        List<String> inputListWinter = new ArrayList<>();
//        for (int doy=150; doy<=270; doy++) {
//            inputListWinter.add("matrices_2004" + String.format("%03d", doy) + ".bin");
//        }
//        for (int doy=90; doy<=270; doy++) {
//            inputListWinter.add("matrices_2005" + String.format("%03d", doy) + ".bin");
//        }
//        result = IOUtils.sortAccumulatorFileList(inputListWinter, 2005, 121);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2005106.bin", result.get(0));
//        assertEquals("matrices_2005135.bin", result.get(29));
//
//        result = IOUtils.sortAccumulatorFileList(inputListWinter, 2005, 9);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2004267.bin", result.get(0));
//        assertEquals("matrices_2005115.bin", result.get(29));
//
//        result = IOUtils.sortAccumulatorFileList(inputListWinter, 2005, 353);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2005241.bin", result.get(0));
//        assertEquals("matrices_2005270.bin", result.get(29));
//        System.out.println();
//
//        List<String> inputListSummer = new ArrayList<>();
//        for (int doy=275; doy<=365; doy++) {
//            inputListSummer.add("matrices_2004" + String.format("%03d", doy) + ".bin");
//        }
//        for (int doy=0; doy<=90; doy++) {
//            inputListSummer.add("matrices_2005" + String.format("%03d", doy) + ".bin");
//        }
//        for (int doy=270; doy<=365; doy++) {
//            inputListSummer.add("matrices_2005" + String.format("%03d", doy) + ".bin");
//        }
//        for (int doy=0; doy<=90; doy++) {
//            inputListSummer.add("matrices_2006" + String.format("%03d", doy) + ".bin");
//        }
//
//        result = IOUtils.sortAccumulatorFileList(inputListSummer, 2005, 121);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2005061.bin", result.get(0));
//        assertEquals("matrices_2005090.bin", result.get(29));
//
//        result = IOUtils.sortAccumulatorFileList(inputListSummer, 2005, 9);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2004359.bin", result.get(0));
//        assertEquals("matrices_2005023.bin", result.get(29));
//
//        result = IOUtils.sortAccumulatorFileList(inputListSummer, 2005, 353);
//        assertNotNull(result);
//        assertEquals(30, result.size());
//        assertEquals("matrices_2005338.bin", result.get(0));
//        assertEquals("matrices_2006001.bin", result.get(29));
//        System.out.println();
//
//        for (int i=0; i<180; i+=2) {
//            System.out.println("i, weight = " + i + ", " + AlbedoInversionUtils.getWeight(i));
//        }
//    }

    public void testSortAccumulatorFileList() {
        List<String> inputList = new ArrayList<>();
        for (int doy=275; doy<=365; doy++) {
            inputList.add("matrices_2004" + String.format("%03d", doy) + ".bin");
        }
        for (int doy=0; doy<=365; doy++) {
            inputList.add("matrices_2005" + String.format("%03d", doy) + ".bin");
        }
        for (int doy=0; doy<=90; doy++) {
            inputList.add("matrices_2006" + String.format("%03d", doy) + ".bin");
        }

        List<String> result = IOUtils.sortAccumulatorFileList(inputList, 2005, 121);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2005091.bin", result.get(0));
        assertEquals("matrices_2005150.bin", result.get(59));

        result = IOUtils.sortAccumulatorFileList(inputList, 2005, 9);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2004344.bin", result.get(0));
        assertEquals("matrices_2005038.bin", result.get(59));

        result = IOUtils.sortAccumulatorFileList(inputList, 2005, 353);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2005323.bin", result.get(0));
        assertEquals("matrices_2006016.bin", result.get(59));
        System.out.println();

        List<String> inputListWinter = new ArrayList<>();
        for (int doy=150; doy<=270; doy++) {
            inputListWinter.add("matrices_2004" + String.format("%03d", doy) + ".bin");
        }
        for (int doy=90; doy<=270; doy++) {
            inputListWinter.add("matrices_2005" + String.format("%03d", doy) + ".bin");
        }
        result = IOUtils.sortAccumulatorFileList(inputListWinter, 2005, 121);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2005091.bin", result.get(0));
        assertEquals("matrices_2005150.bin", result.get(59));

        result = IOUtils.sortAccumulatorFileList(inputListWinter, 2005, 9);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2004252.bin", result.get(0));
        assertEquals("matrices_2005130.bin", result.get(59));

        result = IOUtils.sortAccumulatorFileList(inputListWinter, 2005, 353);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2005211.bin", result.get(0));
        assertEquals("matrices_2005270.bin", result.get(59));
        System.out.println();

        List<String> inputListSummer = new ArrayList<>();
        for (int doy=275; doy<=365; doy++) {
            inputListSummer.add("matrices_2004" + String.format("%03d", doy) + ".bin");
        }
        for (int doy=0; doy<=90; doy++) {
            inputListSummer.add("matrices_2005" + String.format("%03d", doy) + ".bin");
        }
        for (int doy=270; doy<=365; doy++) {
            inputListSummer.add("matrices_2005" + String.format("%03d", doy) + ".bin");
        }
        for (int doy=0; doy<=90; doy++) {
            inputListSummer.add("matrices_2006" + String.format("%03d", doy) + ".bin");
        }

        result = IOUtils.sortAccumulatorFileList(inputListSummer, 2005, 121);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2005031.bin", result.get(0));
        assertEquals("matrices_2005090.bin", result.get(59));

        result = IOUtils.sortAccumulatorFileList(inputListSummer, 2005, 9);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2004344.bin", result.get(0));
        assertEquals("matrices_2005038.bin", result.get(59));

        result = IOUtils.sortAccumulatorFileList(inputListSummer, 2005, 353);
        assertNotNull(result);
        assertEquals(60, result.size());
        assertEquals("matrices_2005323.bin", result.get(0));
        assertEquals("matrices_2006016.bin", result.get(59));
        System.out.println();

        for (int i=0; i<180; i+=2) {
            System.out.println("i, weight = " + i + ", " + AlbedoInversionUtils.getWeight(i));
        }
    }


//    public void testWriteFloatArray1() throws Exception {
//        float[][] fArray = new float[dim1][dim2];
//
//        for (int i = 0; i < dim1; i++) {
//            for (int j = 0; j < dim2; j++) {
//                fArray[i][j] = i * j * 1.0f;
//            }
//        }
//
//        long t1 = System.currentTimeMillis();
//        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * 4);
//        FloatBuffer floatBuffer = bb.asFloatBuffer();
//
//        // Create an output stream to the file.
//        FileOutputStream file_output = new FileOutputStream(testfile);
//        // Create a writable file channel
//        FileChannel ch = file_output.getChannel();
//        for (int i = 0; i < dim1; i++) {
//            floatBuffer.put(fArray[i], 0, dim2);
//        }
//        ch.write(bb);
//        long t2 = System.currentTimeMillis();
//        System.out.println("write test 1 time: = " + (t2 - t1));
//        file_output.close();
//        ch.close();
//    }
//
//    public void testWriteFloatArray2() throws Exception {
//        float[][] fArray = new float[dim1][dim2];
//
//        for (int i = 0; i < dim1; i++) {
//            for (int j = 0; j < dim2; j++) {
//                fArray[i][j] = (i * dim1 + j) * 1.0f;
//            }
//        }
//
//        long t1 = System.currentTimeMillis();
//        // Create an output stream to the file.
//        FileOutputStream file_output = new FileOutputStream(testfile);
//        // Create a writable file channel
//        FileChannel ch = file_output.getChannel();
//        ByteBuffer bb = ByteBuffer.allocate(dim1 * dim2 * 4);
//
//        int index = 0;
//        for (int i = 0; i < dim1; i++) {
//            for (int j = 0; j < dim2; j++) {
//                bb.putFloat(index, fArray[i][j]);
//                index += 4;
//            }
//        }
//
//        // Write the ByteBuffer contents; the bytes between the ByteBuffer's
//        // position and the limit is written to the file
//        ch.write(bb);
//
//        long t2 = System.currentTimeMillis();
//        file_output.close();
//        ch.close();
//        System.out.println("write test 2 time: = " + (t2 - t1));
//    }

    public void testGetTileDirectories() throws Exception {
        File[] tileDirs = IOUtils.getTileDirectories(rootDir);
        assertNotNull(tileDirs);
        assertEquals(2, tileDirs.length);
        assertEquals("h18v04", tileDirs[0].getName());
        assertEquals("h25v06", tileDirs[1].getName());
    }

    public void testIsBadAvhrrProduct() throws Exception {
        // e.g. AVH_20051219_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04.nc

        // uses auxdata file 'avhrr_brf_blacklist.txt'
        assertFalse(IOUtils.isBlacklistedAvhrrProduct("AVH_19700101_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04.nc"));
        assertFalse(IOUtils.isBlacklistedAvhrrProduct("AVH_19781207_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04.nc"));
        assertFalse(IOUtils.isBlacklistedAvhrrProduct("AVH_20170116_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04.nc"));

        assertTrue(IOUtils.isBlacklistedAvhrrProduct("AVH_19820728_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04.nc"));
        assertTrue(IOUtils.isBlacklistedAvhrrProduct("AVH_19860825_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04.nc"));
        assertTrue(IOUtils.isBlacklistedAvhrrProduct("AVH_19951008_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04.nc"));
    }

    public void testGetAvhrrMaskProduct() {
        String productName_1 = "AVH_20010321_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04";
        final int year = 2001;
        final String tile = "h18v04";
        final String maskProductDir = IOUtils.class.getResource("avhrr_mask").getPath();
        Product avhrrMaskProduct = IOUtils.getAvhrrMaskProduct(maskProductDir, productName_1, year, tile);
        assertNotNull(avhrrMaskProduct);
        avhrrMaskProduct.getBands();
        assertEquals(200, avhrrMaskProduct.getSceneRasterWidth());
        assertEquals(200, avhrrMaskProduct.getSceneRasterHeight());
        assertEquals(4, avhrrMaskProduct.getNumBands());
        assertEquals("probability", avhrrMaskProduct.getBandAt(0).getName());
        assertEquals("mask", avhrrMaskProduct.getBandAt(1).getName());

        String productName_2 = "AVH_20040513_001D_900S900N1800W1800E_0005D_BBDR_N16_h18v04";
        avhrrMaskProduct = IOUtils.getAvhrrMaskProduct(maskProductDir, productName_2, year, tile);
        assertNull(avhrrMaskProduct);

        String productName_2a = "AVHRR3_NOAA16_20010321_20010321_L1_BRF_900S900N1800W1800E_PLC_0005D_v03_h18v04";
        avhrrMaskProduct = IOUtils.getAvhrrMaskProduct(maskProductDir, productName_2a, year, tile);
        assertNotNull(avhrrMaskProduct);
        avhrrMaskProduct.getBands();
        assertEquals(200, avhrrMaskProduct.getSceneRasterWidth());
        assertEquals(200, avhrrMaskProduct.getSceneRasterHeight());
        assertEquals(4, avhrrMaskProduct.getNumBands());
        assertEquals("probability", avhrrMaskProduct.getBandAt(0).getName());
        assertEquals("mask", avhrrMaskProduct.getBandAt(1).getName());

        String productName_3 =
                "W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,GO08+IMAGER_VIS02_-75_C_BBDR_EUMP_20010321000000";
        avhrrMaskProduct = IOUtils.getAvhrrMaskProduct(maskProductDir, productName_3, year, tile);
        assertNotNull(avhrrMaskProduct);
        avhrrMaskProduct.getBands();
        assertEquals(200, avhrrMaskProduct.getSceneRasterWidth());
        assertEquals(200, avhrrMaskProduct.getSceneRasterHeight());
        assertEquals(4, avhrrMaskProduct.getNumBands());
        assertEquals("probability", avhrrMaskProduct.getBandAt(0).getName());
        assertEquals("mask", avhrrMaskProduct.getBandAt(1).getName());

        String productName_4 = "W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,GO08+IMAGER_VIS02_-75_C_BBDR_EUMP_20040718000000";
        avhrrMaskProduct = IOUtils.getAvhrrMaskProduct(maskProductDir, productName_4, year, tile);
        assertNull(avhrrMaskProduct);
    }

    public void testSomething()  {
        new Color(220, 230, 240);
        new Color(230, 253, 200);
        new Color(250, 250, 210);
        new Color(240, 240, 180);
        new Color(246, 235, 155);
        new Color(246, 225, 135);
        new Color(240, 220, 120);
        new Color(231, 210, 113);
        new Color(216, 193,  99);
        new Color(201, 176,  85);
        new Color(186, 159,  71);
        new Color(171, 142,  56);
        new Color(156, 125,  42);
        new Color(141, 108,  28);
        new Color(126,  91,  14);
        new Color(120,  82,   7);
        new Color(111,  75,   0);
        new Color(120,  80,   0);
        new Color(125,  99,   0);
        new Color(130, 110,   0);
        new Color(140, 120,   0);
        new Color(120, 140,  10);
        new Color(110, 150,  10);
        new Color(100, 160,  20);
        new Color( 80, 180,  30);
        new Color( 60, 160,  50);
        new Color( 50, 140,  50);
        new Color( 40, 120,  40);
        new Color( 30, 100,  30);
        new Color( 20,  80,  20);
        new Color( 60,  80,  40);
        new Color( 80,  60,  40);
        new Color(100,  50,  30);
        new Color(100,  30,  20);
        new Color( 80,  20,  10);
        new Color(0,     0, 255);
        new Color(0,     0, 200);
        new Color(0,     0, 100);
        new Color(0,     0, 50);
    }
}
