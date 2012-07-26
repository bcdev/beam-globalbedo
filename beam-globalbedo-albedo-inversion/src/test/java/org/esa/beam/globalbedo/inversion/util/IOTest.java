package org.esa.beam.globalbedo.inversion.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class IOTest extends TestCase {

    private int dim1;
    private int dim2;
    private File testfile;
    File existingTileInfoFile;

    @Override
    public void setUp() throws Exception {
        dim1 = 1200;
        dim2 = 1200;
        testfile = new File(System.getProperty("user.home") + File.separator + "muell.txt");
        testfile.createNewFile();
    }

    @Override
    public void tearDown() throws Exception {
        if (existingTileInfoFile != null && existingTileInfoFile.exists()) {
            existingTileInfoFile.delete();
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

        List<String> priorProductNames = IOUtils.getPriorProductNames(priorDirContent, false);
        assertNotNull(priorProductNames);
        assertEquals(3, priorProductNames.size());
        assertEquals("Kernels.105.005.h18v04.backGround.NoSnow.hdr", priorProductNames.get(0));
        assertEquals("Kernels.117.005.h18v04.backGround.NoSnow.hdr", priorProductNames.get(1));
        assertEquals("Kernels.129.005.h18v04.backGround.NoSnow.hdr", priorProductNames.get(2));
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

    public void testWriteFloatArray1() throws Exception {
        float[][] fArray = new float[dim1][dim2];

        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                fArray[i][j] = i * j * 1.0f;
            }
        }

        long t1 = System.currentTimeMillis();
        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * 4);
        FloatBuffer floatBuffer = bb.asFloatBuffer();

        // Create an output stream to the file.
        FileOutputStream file_output = new FileOutputStream(testfile);
        // Create a writable file channel
        FileChannel ch = file_output.getChannel();
        for (int i = 0; i < dim1; i++) {
            floatBuffer.put(fArray[i], 0, dim2);
//            floatBuffer.clear();
        }
        ch.write(bb);
        long t2 = System.currentTimeMillis();
        System.out.println("write test 1 time: = " + (t2 - t1));
        file_output.close();
        ch.close();
    }

    public void testWriteFloatArray2() throws Exception {
        float[][] fArray = new float[dim1][dim2];

        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                fArray[i][j] = (i * dim1 + j) * 1.0f;
            }
        }

        long t1 = System.currentTimeMillis();
        // Create an output stream to the file.
        FileOutputStream file_output = new FileOutputStream(testfile);
        // Create a writable file channel
        FileChannel ch = file_output.getChannel();
        ByteBuffer bb = ByteBuffer.allocate(dim1 * dim2 * 4);

        int index = 0;
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                bb.putFloat(index, fArray[i][j]);
                index += 4;
            }
        }

        // Write the ByteBuffer contents; the bytes between the ByteBuffer's
        // position and the limit is written to the file
        ch.write(bb);

        long t2 = System.currentTimeMillis();
        file_output.close();
        ch.close();
        System.out.println("write test 2 time: = " + (t2 - t1));
    }

    public void testReadFloatArray_2() throws Exception {
        long t1 = System.currentTimeMillis();
        FileInputStream finStream = new FileInputStream(testfile);
        FileChannel ch = finStream.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2);

        float[][] fArray = new float[dim1][dim2];
        int ii = 0;
        int jj = 0;

        int nRead;
        while ((nRead = ch.read(bb)) != -1) {
            if (nRead == 0) {
                continue;
            }
            bb.position(0);
            bb.limit(nRead);
            while (bb.hasRemaining()) {
                final float value = bb.getFloat();
                fArray[ii][jj] = value;
                jj++;
                if (jj == dim1) {
                    ii++;
                    jj = 0;
                }
            }
            bb.clear();
        }
        ch.close();
        finStream.close();
        long t2 = System.currentTimeMillis();
        System.out.println("read test 2 time: = " + (t2 - t1));
        float expected = 1.0f;
        assertEquals(expected, fArray[0][1]);
        expected = 34 * dim1 + 27.0f;
        assertEquals(expected, fArray[34][27]);
        expected = 673 * dim1 + 1158.0f;
        assertEquals(expected, fArray[673][1158]);
        expected = (dim1 - 1) * dim1 + (dim2 - 1) * 1.0f;
        assertEquals(expected, fArray[dim1 - 1][dim2 - 1]);
    }

    public void testReadFloatArray_3() throws Exception {
        long t1 = System.currentTimeMillis();
        FileInputStream finStream = new FileInputStream(testfile);
        FileChannel ch = finStream.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * 4);
        FloatBuffer floatBuffer = bb.asFloatBuffer();

        ch.read(bb);

        float[][] fArray = new float[dim1][dim2];

        for (int i = 0; i < dim1; i++) {
            floatBuffer.get(fArray[i]);
        }

        bb.clear();
        floatBuffer.clear();
        long t2 = System.currentTimeMillis();
        System.out.println("read test 3 time: = " + (t2 - t1));
        float expected = 1.0f;
        assertEquals(expected, fArray[0][1]);
        expected = 34 * dim1 + 27.0f;
        assertEquals(expected, fArray[34][27]);
        expected = 673 * dim1 + 1158.0f;
        assertEquals(expected, fArray[673][1158]);
        expected = (dim1 - 1) * dim1 + (dim2 - 1) * 1.0f;
        assertEquals(expected, fArray[dim1 - 1][dim2 - 1]);
    }

    public void testReadFloatArray_4() throws Exception {
        long t1 = System.currentTimeMillis();
        FileInputStream finStream = new FileInputStream(testfile);
        FileChannel ch = finStream.getChannel();

        float[][] fArray = new float[dim1][dim2];

        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * 4);
        FloatBuffer floatBuffer = bb.asFloatBuffer();
        for (int i = 0; i < dim1; i++) {
            ch.read(bb);
            floatBuffer.get(fArray[i]);
        }

        long t2 = System.currentTimeMillis();
        System.out.println("read test 4 time: = " + (t2 - t1));
        float expected = 1.0f;
        assertEquals(expected, fArray[0][1]);
        expected = 34 * dim1 + 27.0f;
        assertEquals(expected, fArray[34][27]);
        expected = 673 * dim1 + 1158.0f;
        assertEquals(expected, fArray[673][1158]);
        expected = (dim1 - 1) * dim1 + (dim2 - 1) * 1.0f;
        assertEquals(expected, fArray[dim1 - 1][dim2 - 1]);
    }

    public void testReadFloatArray_5() throws Exception {
        long t1 = System.currentTimeMillis();
        FileInputStream finStream = new FileInputStream(testfile);
        FileChannel ch = finStream.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * 4);
        byte[] barray = new byte[1200];
        int nRead, nGet;
        while ((nRead = ch.read(bb)) != -1) {
            if (nRead == 0)
                continue;
            bb.position(0);
            bb.limit(nRead);
            while (bb.hasRemaining()) {
                nGet = Math.min(bb.remaining(), 1200);
                bb.get(barray, 0, nGet);
            }
            bb.clear();
        }
        long t2 = System.currentTimeMillis();
        System.out.println("read test 5 time: = " + (t2 - t1));
    }

    public void testReadFloatArray_6() throws Exception {
        long t1 = System.currentTimeMillis();
        FileInputStream finStream = new FileInputStream(testfile);
        FileChannel ch = finStream.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * 4);
        byte[] barray = new byte[dim1];
        int nRead, nGet;
        while ((nRead = ch.read(bb)) != -1) {
            if (nRead == 0)
                continue;
            bb.position(0);
            bb.limit(nRead);
            while (bb.hasRemaining()) {
                nGet = Math.min(bb.remaining(), dim1);
                bb.get(barray, 0, nGet);
            }
            bb.clear();
        }
        long t2 = System.currentTimeMillis();
        System.out.println("read test 6 time: = " + (t2 - t1));
    }

    public void testReadFloatArray_7() throws Exception {
        long t1 = System.currentTimeMillis();
        FileInputStream finStream = new FileInputStream(testfile);
        FileChannel ch = finStream.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2);

        float[][] fArray = new float[dim1][dim2];
        int ii = 0;
        int jj = 0;

        int nRead;
        while ((nRead = ch.read(bb)) != -1) {
            if (nRead == 0) {
                continue;
            }
            bb.position(0);
            bb.limit(nRead);
            while (bb.hasRemaining()) {
                final float value = bb.getFloat();
                fArray[ii][jj] = value;
                jj++;
                if (jj == dim1) {
                    ii++;
                    jj = 0;
                }
            }
            bb.clear();
        }
        ch.close();
        finStream.close();
        long t2 = System.currentTimeMillis();
        System.out.println("read test 2 time: = " + (t2 - t1));
        float expected = 1.0f;
        assertEquals(expected, fArray[0][1]);
        expected = 34 * dim1 + 27.0f;
        assertEquals(expected, fArray[34][27]);
        expected = 673 * dim1 + 1158.0f;
        assertEquals(expected, fArray[673][1158]);
        expected = (dim1 - 1) * dim1 + (dim2 - 1) * 1.0f;
        assertEquals(expected, fArray[dim1 - 1][dim2 - 1]);
    }


    public void testReadFloatArray_8() throws Exception {
        long t1 = System.currentTimeMillis();
        FileInputStream finStream = new FileInputStream(testfile);
        FileChannel ch = finStream.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * 4);
        FloatBuffer floatBuffer = bb.asFloatBuffer();

        ch.read(bb);

        float[][][] fArray = new float[1][dim1][dim2];

        for (int i = 0; i < dim1; i++) {
            floatBuffer.get(fArray[0][i]);
        }

        bb.clear();
        floatBuffer.clear();
        long t2 = System.currentTimeMillis();
        System.out.println("read test 3 time: = " + (t2 - t1));
        float expected = 1.0f;
        assertEquals(expected, fArray[0][0][1]);
        expected = 34 * dim1 + 27.0f;
        assertEquals(expected, fArray[0][34][27]);
        expected = 673 * dim1 + 1158.0f;
        assertEquals(expected, fArray[0][673][1158]);
        expected = (dim1 - 1) * dim1 + (dim2 - 1) * 1.0f;
        assertEquals(expected, fArray[0][dim1 - 1][dim2 - 1]);
    }

    public void testGetTileDirectories() throws Exception {
        String rootDir = System.getProperty("user.home");
        File h18v04 = new File(rootDir + File.separator + "h18v04");
        if (!h18v04.exists()) h18v04.mkdir();
        File h25v06 = new File(rootDir + File.separator + "h25v06");
        if (!h25v06.exists()) h25v06.mkdir();

        File[] tileDirs = IOUtils.getTileDirectories(rootDir);
        assertNotNull(tileDirs);
        assertEquals(2, tileDirs.length);
        assertEquals("h18v04", tileDirs[0].getName());
        assertEquals("h25v06", tileDirs[1].getName());

    }
}
