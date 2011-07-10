package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.inversion.AlbedoInput;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.FullAccumulator;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Level;

/**
 * Utility class for Albedo Inversion I/O operations
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class IOUtils {

    public static Product[] getAccumulationInputProducts(String bbdrRootDir, String tile, int year, int doy) throws
            IOException {
        final String daystring = AlbedoInversionUtils.getDateFromDoy(year, doy);

        final String merisBbdrDir = bbdrRootDir + File.separator + "MERIS" + File.separator + year + File.separator + tile;
        final String[] merisBbdrFiles = (new File(merisBbdrDir)).list();
        final List<String> merisBbdrFileList = getDailyBBDRFilenames(merisBbdrFiles, daystring);

        final String aatsrBbdrDir = bbdrRootDir + File.separator + "AATSR" + File.separator + year + File.separator + tile;
        final String[] aatsrBbdrFiles = (new File(aatsrBbdrDir)).list();
        final List<String> aatsrBbdrFileList = getDailyBBDRFilenames(aatsrBbdrFiles, daystring);

        final String vgtBbdrDir = bbdrRootDir + File.separator + "VGT" + File.separator + year + File.separator + tile;
        final String[] vgtBbdrFiles = (new File(vgtBbdrDir)).list();
        final List<String> vgtBbdrFileList = getDailyBBDRFilenames(vgtBbdrFiles, daystring);

        final int numberOfInputProducts = merisBbdrFileList.size() + aatsrBbdrFileList.size() + vgtBbdrFileList.size();
        Product[] bbdrProducts = new Product[numberOfInputProducts];

        int productIndex = 0;
        for (String aMerisBbdrFileList : merisBbdrFileList) {
            String sourceProductFileName = merisBbdrDir + File.separator + aMerisBbdrFileList;
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }
        for (String anAatsrBbdrFileList : aatsrBbdrFileList) {
            String sourceProductFileName = aatsrBbdrDir + File.separator + anAatsrBbdrFileList;
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }
        for (String aVgtBbdrFileList : vgtBbdrFileList) {
            String sourceProductFileName = vgtBbdrDir + File.separator + aVgtBbdrFileList;
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }

        if (productIndex == 0) {
            BeamLogManager.getSystemLogger().log(Level.ALL, "No BBDR source products found for DoY " + IOUtils.getDoyString(doy) + " ...");
        }

        return bbdrProducts;
    }

    /**
     * Filters from a list of BBDR file names the ones which contain a given daystring
     *
     * @param bbdrFilenames - the list of filenames
     * @param daystring     - the daystring
     * @return List<String> - the filtered list
     */
    static List<String> getDailyBBDRFilenames(String[] bbdrFilenames, String daystring) {
        List<String> dailyBBDRFilenames = new ArrayList<String>();
        if (bbdrFilenames != null && bbdrFilenames.length > 0 && StringUtils.isNotNullAndNotEmpty(daystring)) {
            for (String s : bbdrFilenames) {
                if (s.endsWith(".dim") && s.contains(daystring)) {
                    dailyBBDRFilenames.add(s);
                }
            }
        }

        Collections.sort(dailyBBDRFilenames);
        return dailyBBDRFilenames;
    }

    public static Product getPriorProduct(String priorDir, int doy, boolean computeSnow) throws IOException {

        final String[] priorFiles = (new File(priorDir)).list();
        final List<String> snowFilteredPriorList = getPriorProductNames(priorFiles, computeSnow);

        String doyString = getDoyString(doy);

        for (String priorFileName : snowFilteredPriorList) {
            if (priorFileName.startsWith("Kernels." + doyString)) {
                String sourceProductFileName = priorDir + File.separator + priorFileName;
                Product product = ProductIO.readProduct(sourceProductFileName);
                return product;
            }
        }

        return null;
    }

    public static Product getReprojectedPriorProduct(Product priorProduct, String tile,
                                                     Product bbdrProduct) throws IOException {
        Product geoCodingReferenceProduct = bbdrProduct;
        ProductUtils.copyGeoCoding(geoCodingReferenceProduct, priorProduct);
        double easting = AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[0];
        double northing = AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[1];
        Product reprojectedProduct = AlbedoInversionUtils.reprojectToSinusoidal(priorProduct, easting,
                northing);
        return reprojectedProduct;
    }

    public static Product getBrdfProduct(String brdfDir, int year, int doy, boolean isSnow) throws IOException {
        final String[] brdfFiles = (new File(brdfDir)).list();
        final List<String> brdfFileList = getBrdfProductNames(brdfFiles, isSnow);

        String doyString = getDoyString(doy);

        for (String brdfFileName : brdfFileList) {
            if (brdfFileName.startsWith("GlobAlbedo." + Integer.toString(year) + doyString)) {
                String sourceProductFileName = brdfDir + File.separator + brdfFileName;
                Product product = ProductIO.readProduct(sourceProductFileName);
                return product;
            }
        }

        return null;
    }

    public static String getDoyString(int doy) {
        String doyString = Integer.toString(doy);
        if (doy < 0 || doy > 366) {
            return null;
        }
        if (doy < 10) {
            doyString = "00" + doyString;
        } else if (doy < 100) {
            doyString = "0" + doyString;
        }
        return doyString;
    }

    static List<String> getPriorProductNames(String[] priorFiles, boolean computeSnow) {

        List<String> snowFilteredPriorList = new ArrayList<String>();
        if (priorFiles != null && priorFiles.length > 0) {
            for (String s : priorFiles) {
                if ((computeSnow && s.endsWith(".Snow.hdr")) || (!computeSnow && s.endsWith(".NoSnow.hdr"))) {
                    snowFilteredPriorList.add(s);
                }
            }
        }
        Collections.sort(snowFilteredPriorList);
        return snowFilteredPriorList;
    }

    private static List<String> getBrdfProductNames(String[] brdfFiles, boolean snow) {
        List<String> brdfFileList = new ArrayList<String>();
        for (String s : brdfFiles) {
            if ((!snow && s.contains(".NoSnow") && s.endsWith(".dim")) || (snow && s.contains(".Snow") && s.endsWith(".dim"))) {
                brdfFileList.add(s);
            }
        }
        Collections.sort(brdfFileList);
        return brdfFileList;
    }

    public static AlbedoInput getAlbedoInputProduct(String accumulatorRootDir,
                                                    boolean useBinaryFiles,
                                                    int doy, int year, String tile,
                                                    int wings,
                                                    boolean computeSnow) {

        final List<String> albedoInputProductList = getAlbedoInputProductFileNames(accumulatorRootDir, useBinaryFiles, doy, year,
                tile,
                wings,
                computeSnow);

        String[] albedoInputProductFilenames = new String[albedoInputProductList.size()];

        int[] albedoInputProductDoys = new int[albedoInputProductList.size()];
        int[] albedoInputProductYears = new int[albedoInputProductList.size()];

        int productIndex = 0;

        for (String albedoInputProductName : albedoInputProductList) {

            String productYearRootDir;
            // e.g. get '2006' from 'matrices_2006_doy.dim'...
            final String thisProductYear = albedoInputProductName.substring(9, 13);
            final String thisProductDoy = albedoInputProductName.substring(13, 16); // changed to 'matrices_yyyydoy.dim'
            if (computeSnow) {
                productYearRootDir = accumulatorRootDir.concat(
                        File.separator + thisProductYear + File.separator + tile + File.separator + "Snow");
            } else {
                productYearRootDir = accumulatorRootDir.concat(
                        File.separator + thisProductYear + File.separator + tile + File.separator + "NoSnow");
            }

            String sourceProductFileName = productYearRootDir + File.separator + albedoInputProductName;
            albedoInputProductFilenames[productIndex] = sourceProductFileName;
            albedoInputProductDoys[productIndex] = Integer.parseInt(
                    thisProductDoy) - (doy + 8) - 365 * (year - Integer.parseInt(thisProductYear));
            albedoInputProductYears[productIndex] = Integer.parseInt(thisProductYear);
            productIndex++;
        }

        AlbedoInput inputProduct = new AlbedoInput();
        inputProduct.setProductFilenames(albedoInputProductFilenames);
        inputProduct.setProductDoys(albedoInputProductDoys);
        inputProduct.setProductYears(albedoInputProductYears);
        inputProduct.setReferenceYear(year);
        inputProduct.setReferenceDoy(doy);

        if (useBinaryFiles) {
            final List<String> albedoInputProductBinaryFileList = getAlbedoInputProductFileNames(accumulatorRootDir,
                    true, doy, year, tile,
                    wings,
                    computeSnow);
            String[] albedoInputProductBinaryFilenames = new String[albedoInputProductBinaryFileList.size()];
            int binaryProductIndex = 0;
            for (String albedoInputProductBinaryName : albedoInputProductBinaryFileList) {
                String productYearRootDir;
                // e.g. get '2006' from 'matrices_2006xxx.bin'...
                final String thisProductYear = albedoInputProductBinaryName.substring(9, 13);
                if (computeSnow) {
                    productYearRootDir = accumulatorRootDir.concat(
                            File.separator + thisProductYear + File.separator + tile + File.separator + "Snow");
                } else {
                    productYearRootDir = accumulatorRootDir.concat(
                            File.separator + thisProductYear + File.separator + tile + File.separator + "NoSnow");
                }

                String sourceProductBinaryFileName = productYearRootDir + File.separator + albedoInputProductBinaryName;
                albedoInputProductBinaryFilenames[binaryProductIndex] = sourceProductBinaryFileName;
                binaryProductIndex++;
            }
            inputProduct.setProductBinaryFilenames(albedoInputProductBinaryFilenames);
        }

        return inputProduct;
    }

    /**
     * Returns the filename for an inversion output product
     *
     * @param year        - year
     * @param doy         - day of interest
     * @param tile        - tile
     * @param computeSnow - boolean
     * @param usePrior    - boolean
     * @return String
     */
    public static String getInversionTargetFileName(int year, int doy, String tile, boolean computeSnow,
                                                    boolean usePrior) {
        String targetFileName;

        //  build up a name like this: GlobAlbedo.2005129.h18v04.NoSnow.bin
        if (computeSnow) {
            if (usePrior) {
                targetFileName = "GlobAlbedo." + year + IOUtils.getDoyString(doy) + "." + tile + ".Snow.bin";
            } else {
                targetFileName = "GlobAlbedo." + year + IOUtils.getDoyString(doy) + "." + tile + ".Snow.NoPrior.bin";
            }
        } else {
            if (usePrior) {
                targetFileName = "GlobAlbedo." + year + IOUtils.getDoyString(doy) + "." + tile + ".NoSnow.bin";
            } else {
                targetFileName = "GlobAlbedo." + year + IOUtils.getDoyString(doy) + "." + tile + ".NoSnow.NoPrior.bin";
            }
        }
        return targetFileName;
    }

    static List<String> getAlbedoInputProductFileNames(String accumulatorRootDir, final boolean isBinaryFiles, int doy,
                                                       int year, String tile,
                                                       int wings,
                                                       boolean computeSnow) {
        List<String> albedoInputProductList = new ArrayList<String>();

        final FilenameFilter yearFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // accept only years between 1995 and 2010 (GA period)
                int startYear = 1995;
                for (int i = 0; i < 16; i++) {
                    String thisYear = (new Integer(startYear + i)).toString();
                    if (name.equals(thisYear)) {
                        return true;
                    }
                }
                return false;
            }
        };

        final FilenameFilter inputProductNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // accept only filenames like 'matrices_2005123.dim', 'matrices_2005123.bin'...
                if (isBinaryFiles) {
                    return (name.length() == 20 && name.startsWith("matrices") && name.endsWith("bin"));
                } else {
                    return (name.length() == 20 && name.startsWith("matrices") && name.endsWith("dim"));
                }
            }
        };

        final String[] allYears = (new File(accumulatorRootDir)).list(yearFilter);

        doy = doy + 8; // 'MODIS day'

        // fill the name list year by year...
        for (String thisYear : allYears) {
            String thisYearsRootDir;
            if (computeSnow) {
                thisYearsRootDir = accumulatorRootDir.concat(
                        File.separator + thisYear + File.separator + tile + File.separator + "Snow");
            } else {
                thisYearsRootDir = accumulatorRootDir.concat(
                        File.separator + thisYear + File.separator + tile + File.separator + "NoSnow");
            }
            final String[] thisYearAlbedoInputFiles = (new File(thisYearsRootDir)).list(inputProductNameFilter);

            for (String s : thisYearAlbedoInputFiles) {
                if (s.startsWith("matrices_" + thisYear)) {
                    if (!albedoInputProductList.contains(s)) {
                        // check the 'wings' condition...
                        try {
                            final int dayOfYear = Integer.parseInt(
                                    s.substring(13, 16));
                            //    # Left wing
                            if (365 + (doy - wings) <= 366) {
                                if (!albedoInputProductList.contains(s)) {
                                    if (dayOfYear >= 366 + (doy - wings) && Integer.parseInt(thisYear) < year) {
                                        albedoInputProductList.add(s);
                                    }
                                }
                            }
                            //    # Center
                            if ((dayOfYear < doy + wings) && (dayOfYear >= doy - wings) &&
                                    (Integer.parseInt(thisYear) == year)) {
                                albedoInputProductList.add(s);
                            }
                            //    # Right wing
                            if ((doy + wings) - 365 > 0) {
                                if (!albedoInputProductList.contains(s)) {
                                    if (dayOfYear <= (doy + wings - 365) && Integer.parseInt(thisYear) > year) {
                                        albedoInputProductList.add(s);
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            BeamLogManager.getSystemLogger().log(Level.ALL, "Cannot determine wings for accumulator " + s + " - skipping.");
                        }
                    }
                }
            }
        }

        Collections.sort(albedoInputProductList);
        return albedoInputProductList;
    }

    private static final Map<Integer, String> waveBandsOffsetMap = new HashMap<Integer, String>();

    static {
        waveBandsOffsetMap.put(0, "VIS");
        waveBandsOffsetMap.put(1, "NIR");
        waveBandsOffsetMap.put(2, "SW");
    }

    public static String[] getDailyAccumulatorBandNames() {
        String[] bandNames = new String[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS *
                3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS +
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + 2];

        int index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                bandNames[index++] = "M_" + i + "" + j;
            }
        }

        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            bandNames[index++] = "V_" + i;
        }

        bandNames[index++] = AlbedoInversionConstants.ACC_E_NAME;
        bandNames[index++] = AlbedoInversionConstants.ACC_MASK_NAME;

        return bandNames;
    }

    public static String[] getInversionParameterBandNames() {
        String bandNames[] = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS *
                AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS];
        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                bandNames[index] = "mean_" + waveBandsOffsetMap.get(i) + "_f" + j;
                index++;
            }
        }
        return bandNames;
    }

    public static String[][] getInversionUncertaintyBandNames() {
        String bandNames[][] = new String[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS]
                [3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS];
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            // only UR triangle matrix
            for (int j = i; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                bandNames[i][j] = "VAR_" + waveBandsOffsetMap.get(i / 3) + "_f" + (i % 3) + "_" +
                        waveBandsOffsetMap.get(j / 3) + "_f" + (j % 3);
            }
        }
        return bandNames;

    }

    public static String[] getAlbedoDhrBandNames() {
        String bandNames[] = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            bandNames[i] = "DHR_" + waveBandsOffsetMap.get(i);
        }
        return bandNames;
    }

    public static String[] getAlbedoBhrBandNames() {
        String bandNames[] = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            bandNames[i] = "BHR_" + waveBandsOffsetMap.get(i);
        }
        return bandNames;
    }

    public static String[] getAlbedoDhrSigmaBandNames() {
        String bandNames[] = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            bandNames[i] = "DHR_sigma" + waveBandsOffsetMap.get(i);
        }
        return bandNames;
    }

    public static String[] getAlbedoBhrSigmaBandNames() {
        String bandNames[] = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            bandNames[i] = "BHR_sigma" + waveBandsOffsetMap.get(i);
        }
        return bandNames;
    }

    public static FullAccumulator getFullAccumulatorFromBinaryFile(int year, int doy, String filename, int numBands) {
        int rasterWidth = AlbedoInversionConstants.MODIS_TILE_WIDTH;
        int rasterHeight = AlbedoInversionConstants.MODIS_TILE_HEIGHT;
//        int size = numBands * rasterWidth * rasterHeight;         // OLD
        int size = numBands * rasterWidth * rasterHeight * 4; // NEW
        final File fullAccumulatorBinaryFile = new File(filename);
        FileInputStream f = null;
        try {
            f = new FileInputStream(fullAccumulatorBinaryFile);
        } catch (FileNotFoundException e) {
            BeamLogManager.getSystemLogger().log(Level.ALL, "No full accumulator file found for year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " - will use data from MODIS priors...");
        }
        FileChannel ch = f.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(size);
        FloatBuffer floatBuffer = bb.asFloatBuffer();

        float[][] daysToTheClosestSample = new float[rasterWidth][rasterHeight];
        float[][][] sumMatrices = new float[numBands][rasterWidth][rasterHeight];

        FullAccumulator accumulator = null;

        try {
            long t1 = System.currentTimeMillis();
            // OLD
//            int nRead;
//            int ii = 0;
//            int jj = 0;
//            int kk = 0;
//            while ((nRead = ch.read(bb)) != -1) {
//                if (nRead == 0) {
//                    continue;
//                }
//                bb.position(0);
//                bb.limit(nRead);
//                while (bb.hasRemaining()) {
//                    final float value = bb.getFloat();
//                    // last band is the dayClosestSample. extract array dayClosestSample[jj][kk]...
//                    if (ii == numBands-1) {
//                        daysToTheClosestSample[jj][kk] = value;
//                    } else {
//                        sumMatrices[ii][jj][kk] = value;
//                    }
//                    // find the right indices for sumMatrices array...
//                    kk++;
//                    if (kk == rasterHeight) {
//                        jj++;
//                        kk = 0;
//                        if (jj == rasterWidth) {
//                            ii++;
//                            jj = 0;
//                        }
//                    }
//                }
//                bb.clear();
//            }
//            ch.close();
//            f.close();

            // NEW, faster
            int nRead = ch.read(bb);
            for (int ii = 0; ii < numBands - 1; ii++) {
                for (int jj = 0; jj < AlbedoInversionConstants.MODIS_TILE_WIDTH; jj++) {
                    floatBuffer.get(sumMatrices[ii][jj]);
                }
            }
            for (int jj = 0; jj < AlbedoInversionConstants.MODIS_TILE_WIDTH; jj++) {
                floatBuffer.get(daysToTheClosestSample[jj]);
            }
            bb.clear();
            floatBuffer.clear();
            ch.close();
            f.close();
            // end NEW
            long t2 = System.currentTimeMillis();
            BeamLogManager.getSystemLogger().log(Level.INFO, "Full accumulator read in: " + (t2 - t1) + " ms");

            accumulator = new FullAccumulator(year, doy, sumMatrices, daysToTheClosestSample);

        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }
        return accumulator;
    }

    public static void writeFloatArrayToFile(File file, float[][][] values) {
        int index = 0;
        try {
            // Create an output stream to the file.
            FileOutputStream file_output = new FileOutputStream(file);
            // Create a writable file channel
            FileChannel wChannel = file_output.getChannel();

            final int dim1 = values.length;
            final int dim2 = values[0].length;
            final int dim3 = values[0][0].length;
            final int size = dim1 * dim2 * dim3 * 4;
            // OLD
//            ByteBuffer bb = ByteBuffer.allocateDirect(size);
//
//            for (int i = 0; i < dim1; i++) {
//                for (int j = 0; j < dim2; j++) {
//                    for (int k = 0; k < dim3; k++) {
//                        bb.putFloat(index, values[i][j][k]);
//                        index += 4;
//                    }
//                }
//            }
//
//            // Write the ByteBuffer contents; the bytes between the ByteBuffer's
//            // position and the limit is written to the file
//            wChannel.write(bb);
            // end OLD

            // NEW, faster:
            ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * dim3 * 4);
            FloatBuffer floatBuffer = bb.asFloatBuffer();
            for (int i = 0; i < dim1; i++) {
                for (int j = 0; j < dim2; j++) {
                    floatBuffer.put(values[i][j], 0, dim3);
                }
            }
            wChannel.write(bb);
            // END NEW

            // Close file when finished with it..
            wChannel.close();
            file_output.close();
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Could not write array to file:  " + e + " // " + e.getMessage() +
                    " // buffer index =  " + index);
        }
    }

    public static void writeFullAccumulatorToFile(File file, float[][][] sumMatrices, float[][] daysClosestSample) {
        int index = 0;
        try {
            // Create an output stream to the file.
            FileOutputStream file_output = new FileOutputStream(file);
            // Create a writable file channel
            FileChannel wChannel = file_output.getChannel();

            final int dim1 = sumMatrices.length;
            final int dim2 = sumMatrices[0].length;
            final int dim3 = sumMatrices[0][0].length;
            final int size = (dim1 + 1) * dim2 * dim3 * 4;
            // OLD
//            ByteBuffer bb = ByteBuffer.allocateDirect(size);
//
//            for (int i = 0; i < dim1; i++) {
//                for (int j = 0; j < dim2; j++) {
//                    for (int k = 0; k < dim3; k++) {
//                        bb.putFloat(index, sumMatrices[i][j][k]);
//                        index += 4;
//                    }
//                }
//            }
//
//            for (int j = 0; j < dim2; j++) {
//                for (int k = 0; k < dim3; k++) {
//                    bb.putFloat(index, daysClosestSample[j][k]);
//                    index += 4;
//                }
//            }
//
//            // Write the ByteBuffer contents; the bytes between the ByteBuffer's
//            // position and the limit is written to the file
//            wChannel.write(bb);
            // end OLD

            // NEW, faster:
            ByteBuffer bb = ByteBuffer.allocateDirect((dim1 + 1) * dim2 * dim3 * 4);
            FloatBuffer floatBuffer = bb.asFloatBuffer();
            for (int i = 0; i < dim1; i++) {
                for (int j = 0; j < dim2; j++) {
                    floatBuffer.put(sumMatrices[i][j], 0, dim3);
                }
            }
            for (int j = 0; j < dim2; j++) {
                floatBuffer.put(daysClosestSample[j], 0, dim3);
            }
            wChannel.write(bb);
            // END NEW

            // Close file when finished with it..
            wChannel.close();
            file_output.close();
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Could not write full accumulator to file:  " + e + " // " + e.getMessage() +
                    " // buffer index =  " + index);
        }
    }

    public static int getDoyFromAlbedoProductName(String productName) {
        String doyString = productName.substring(15, 18);
        int doy = Integer.parseInt(doyString);
        if (doy < 0 || doy > 366) {
            return -1;
        }
        return doy;
    }


    public static String getTileInfoFilePath(String dailyAccumulatorDir, String defaultTileInfoFilename) {
        String tileInfoFilePath = dailyAccumulatorDir + File.separator + defaultTileInfoFilename;
//        System.out.println("tileInfoFilePath = " + tileInfoFilePath);
        File tileInfoFile = new File(tileInfoFilePath);
        if (!tileInfoFile.exists()) {
            int index = 1;
            boolean exists = false;
            while (!exists && index < 365) {
                final String newTileInfoFilename = defaultTileInfoFilename.substring(0, 9) + index + ".dim";
                tileInfoFilePath = dailyAccumulatorDir + File.separator + newTileInfoFilename;
                final File newTileInfoFile = new File(tileInfoFilePath);
                exists = newTileInfoFile.exists();
                index++;
            }
            if (!exists) {
                throw new OperatorException("No info file found for given tile - cannot proceed.");
            }
        }
        return tileInfoFilePath;
    }


    public static boolean isLeapYear(int year) {
        if (year < 0) {
            return false;
        }

        if (year % 400 == 0) {
            return true;
        } else if (year % 100 == 0) {
            return false;
        } else if (year % 4 == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static int getDayDifference(int doy, int year, int referenceDoy, int referenceYear) {
        final int difference = 365 * (year - referenceYear) + (doy - referenceDoy);
        // todo: consider leap years
        return Math.abs(difference);
    }

    public static Product[] getAlbedo8DayProducts(String albedoDir, final String tile) {

        final FilenameFilter inputProductNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // accept only filenames like 'GlobAlbedo_2005129.h18v04.dim'...
                return (name.length() == 29 && name.startsWith("GlobAlbedo") && name.endsWith(tile + ".dim"));
            }
        };

        final String[] albedoFiles = (new File(albedoDir)).list(inputProductNameFilter);

        Product[] albedoProducts = new Product[albedoFiles.length];

        int productIndex = 0;
        for (int i = 0; i < albedoFiles.length; i++) {
            String albedoProductFileName = albedoDir + File.separator + albedoFiles[i];

            if ((new File(albedoProductFileName)).exists()) {
                Product product;
                try {
                    product = ProductIO.readProduct(albedoProductFileName);
                    albedoProducts[productIndex] = product;
                    productIndex++;
                } catch (IOException e) {
                    throw new OperatorException("Cannot load Albedo 8-day product " + albedoProductFileName + ": "
                            + e.getMessage());
                }
            }
        }

        return albedoProducts;
    }

}
