package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.AlbedoInput;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.FullAccumulator;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS;

/**
 * Utility class for Albedo Inversion I/O operations
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class IOUtils {

    public static Product[] getAccumulationInputProducts(String bbdrRootDir, String tile, int year, int doy) throws
            IOException {
        // todo: as we will get more and more sensors, make this configurable to use selected sensors only!
        final String daystring = AlbedoInversionUtils.getDateFromDoy(year, doy);

        final String merisBbdrDir = bbdrRootDir + File.separator + "MERIS" + File.separator + year + File.separator + tile;
        final String[] merisBbdrFiles = (new File(merisBbdrDir)).list();
        final List<String> merisBbdrFileList = getDailyBBDRFilenames(merisBbdrFiles, daystring);

        final String vgtBbdrDir = bbdrRootDir + File.separator + "VGT" + File.separator + year + File.separator + tile;
        final String[] vgtBbdrFiles = (new File(vgtBbdrDir)).list();
        final List<String> vgtBbdrFileList = getDailyBBDRFilenames(vgtBbdrFiles, daystring);

        final int numberOfInputProducts = merisBbdrFileList.size() + vgtBbdrFileList.size();
        Product[] bbdrProducts = new Product[numberOfInputProducts];

        int productIndex = 0;
        for (String merisBBDRFileName : merisBbdrFileList) {
            String sourceProductFileName = merisBbdrDir + File.separator + merisBBDRFileName;
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }
        for (String vgtBBDRFileName : vgtBbdrFileList) {
            String sourceProductFileName = vgtBbdrDir + File.separator + vgtBBDRFileName;
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }

        if (productIndex == 0) {
            BeamLogManager.getSystemLogger().log(Level.ALL, "No BBDR source products found for DoY " + IOUtils.getDoyString(doy) + " ...");
        }

        return bbdrProducts;
    }

    public static List<Product> getAccumulationSinglePixelInputProducts(String bbdrRootDir,
                                                                        String tile,
                                                                        int year,
                                                                        int iDay) throws IOException {

        final String merisBbdrDir = bbdrRootDir + File.separator + "MERIS" + File.separator + year + File.separator + tile;
        final String[] merisBbdrFiles = (new File(merisBbdrDir)).list();
        final List<String> merisBbdrFileList = getDailyBBDRFilenamesSinglePixel(merisBbdrFiles, year, iDay);

        final String vgtBbdrDir = bbdrRootDir + File.separator + "VGT" + File.separator + year + File.separator + tile;
        final String[] vgtBbdrFiles = (new File(vgtBbdrDir)).list();
        final List<String> vgtBbdrFileList = getDailyBBDRFilenamesSinglePixel(vgtBbdrFiles, year, iDay);

        List<Product> accInputProductList = new ArrayList<>();
        for (String merisBBDRFileName : merisBbdrFileList) {
            String sourceProductFileName = merisBbdrDir + File.separator + merisBBDRFileName;
            Product product = ProductIO.readProduct(sourceProductFileName);
            accInputProductList.add(product);
        }
        for (String vgtBBDRFileName : vgtBbdrFileList) {
            String sourceProductFileName = vgtBbdrDir + File.separator + vgtBBDRFileName;
            Product product = ProductIO.readProduct(sourceProductFileName);
            accInputProductList.add(product);
        }

        if (accInputProductList.size() == 0) {
            BeamLogManager.getSystemLogger().log(Level.ALL, "No BBDR source products found for year " + year + " ...");
        }

        return accInputProductList;
    }

    /**
     * Filters from a list of BBDR file names the ones which contain a given daystring
     *
     * @param bbdrFilenames - the list of filenames
     * @param daystring     - the daystring
     * @return List<String> - the filtered list
     */
    static List<String> getDailyBBDRFilenames(String[] bbdrFilenames, String daystring) {
        List<String> dailyBBDRFilenames = new ArrayList<>();
        if (bbdrFilenames != null && bbdrFilenames.length > 0 && StringUtils.isNotNullAndNotEmpty(daystring))
            for (String s : bbdrFilenames)
                if ((s.endsWith(".dim") || s.endsWith(".nc") ||
                        s.endsWith(".dim.gz") || s.endsWith(".nc.gz")) && s.contains(daystring)) {
                    dailyBBDRFilenames.add(s);
                }

        Collections.sort(dailyBBDRFilenames);
        return dailyBBDRFilenames;
    }

    static List<String> getDailyBBDRFilenamesSinglePixel(String[] bbdrFilenames, int year, int iDay) {
        List<String> dailyBBDRFilenames = new ArrayList<>();
        final String dateString = AlbedoInversionUtils.getDateFromDoy(year, iDay);
        if (bbdrFilenames != null && bbdrFilenames.length > 0)
            for (String s : bbdrFilenames)
                if (s.contains(dateString)) {
                    if ((s.endsWith(".dim") || s.endsWith(".nc") ||
                            s.endsWith(".dim.gz") || s.endsWith(".nc.gz"))) {
                        dailyBBDRFilenames.add(s);
                    }
                }

        Collections.sort(dailyBBDRFilenames);
        return dailyBBDRFilenames;
    }

    public static Product getPriorProduct(String priorDir, String priorFileNamePrefix, int doy, boolean computeSnow) throws IOException {

        final String[] priorFiles = (new File(priorDir)).list();
        final List<String> snowFilteredPriorList = getPriorProductNames(priorFiles, computeSnow);

        String doyString = getDoyString(doy);

        for (String priorFileName : snowFilteredPriorList) {
            if (priorFileName.startsWith(priorFileNamePrefix + "." + doyString)) {
                String sourceProductFileName = priorDir + File.separator + priorFileName;
                return ProductIO.readProduct(sourceProductFileName);
            }
        }

        return null;
    }

    public static void attachGeoCodingToPriorProduct(Product priorProduct, String tile,
                                                     Product tileInfoProduct) throws IOException {
        if (tileInfoProduct != null) {
            // we just need to copy the geooding, not to reproject...
            ProductUtils.copyGeoCoding(tileInfoProduct, priorProduct);
        } else {
            // create geocoding manually in case we do not have a 'tileInfo'...
            priorProduct.setGeoCoding(getSinusoidalTileGeocoding(tile));
        }
    }

    public static ModisTileGeoCoding getSinusoidalTileGeocoding(String tile) {
        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        int tileIndex = modisTileCoordinates.findTileIndex(tile);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tile + "");
        }
        final double easting = modisTileCoordinates.getUpperLeftX(tileIndex);
        final double northing = modisTileCoordinates.getUpperLeftY(tileIndex);
        final String crsString = AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING;
        final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X;
        final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y;
        ModisTileGeoCoding geoCoding;
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            geoCoding = new ModisTileGeoCoding(crs, easting, northing, pixelSizeX, pixelSizeY);
        } catch (Exception e) {
            throw new OperatorException("Cannot attach geocoding for tileName= ''" + tile + " : ", e);
        }
        return geoCoding;
    }

//    public static CrsGeoCoding getSinusoidalGlobalGeocoding(int downscalingFactor) {
//        final double easting = AlbedoInversionConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_X;
//        final double northing = AlbedoInversionConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_Y;
//        final String crsString = AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING;
//        final int imageWidth = AlbedoInversionConstants.MODIS_TILE_WIDTH * 36 / downscalingFactor;
//        final int imageHeight = AlbedoInversionConstants.MODIS_TILE_HEIGHT * 18 / downscalingFactor;
//        final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * downscalingFactor;
//        final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * downscalingFactor;
//        try {
//            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
//            return new CrsGeoCoding(crs, imageWidth, imageHeight, easting, northing, pixelSizeX, pixelSizeY);
//        } catch (Exception e) {
//            BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot attach mosaic geocoding : ", e);
//            return null;
//        }
//    }


    public static Product getBrdfProduct(String brdfDir, int year, int doy, boolean isSnow) throws IOException {
        final String[] brdfFiles = (new File(brdfDir)).list();
        final List<String> brdfFileList = getBrdfProductNames(brdfFiles, isSnow);

        String doyString = getDoyString(doy);

        for (String brdfFileName : brdfFileList) {
            if (brdfFileName.startsWith("GlobAlbedo.brdf." + Integer.toString(year) + doyString)) {
                String sourceProductFileName = brdfDir + File.separator + brdfFileName;
                return ProductIO.readProduct(sourceProductFileName);
            }
        }

        return null;
    }

    public static Product getBrdfSeaiceProduct(String brdfDir, int year, int doy) throws IOException {
        final String[] brdfFiles = (new File(brdfDir)).list();
        final List<String> brdfFileList = getBrdfSeaiceProductNames(brdfFiles);

        String doyString = getDoyString(doy);

        for (String brdfFileName : brdfFileList) {
            if (brdfFileName.startsWith("GlobAlbedo.brdf." + Integer.toString(year) + doyString)) {
                String sourceProductFileName = brdfDir + File.separator + brdfFileName;
                return ProductIO.readProduct(sourceProductFileName);
            }
        }

        return null;
    }

    public static String getDoyString(int doy) {
        if (doy < 0 || doy > 366) {
            return null;
        } else {
            return String.format("%03d", doy);
        }
    }

    public static String getMonthString(int month) {
        if (month < 0 || month > 12) {
            return null;
        } else {
            return String.format("%02d", month);
        }
    }

    static List<String> getPriorProductNames(String[] priorFiles, boolean computeSnow) {

        List<String> snowFilteredPriorList = new ArrayList<>();
        if (priorFiles != null && priorFiles.length > 0) {
            for (String s : priorFiles) {
                // CEMS: kernel.001.006.h18v04.Snow.1km.nc
                if ((computeSnow && s.endsWith(".Snow.hdr")) || (!computeSnow && s.endsWith(".NoSnow.hdr")) ||
                        (computeSnow && s.endsWith(".Snow.nc")) || (!computeSnow && s.endsWith(".NoSnow.nc")) ||
                        (computeSnow && s.endsWith(".Snow.1km.nc")) || (!computeSnow && s.endsWith(".NoSnow.1km.nc"))) {
                    snowFilteredPriorList.add(s);
                }
            }
        }
        Collections.sort(snowFilteredPriorList);
        return snowFilteredPriorList;
    }

    public static AlbedoInput getAlbedoInputProduct(String accumulatorRootDir,
                                                    boolean useBinaryFiles,
                                                    int doy, int year, String tile,
                                                    int wings,
                                                    boolean computeSnow,
                                                    boolean computeSeaice) {

        AlbedoInput inputProduct = null;

        final List<String> albedoInputProductList = getAlbedoInputProductFileNames(accumulatorRootDir, useBinaryFiles, doy, year,
                tile,
                wings,
                computeSnow, computeSeaice);

        if (albedoInputProductList.size() > 0) {
            inputProduct = new AlbedoInput();
            inputProduct.setReferenceYear(year);
            inputProduct.setReferenceDoy(doy);

            if (useBinaryFiles) {
                final List<String> albedoInputProductBinaryFileList = getAlbedoInputProductFileNames(accumulatorRootDir,
                        true, doy, year, tile,
                        wings,
                        computeSnow,
                        computeSeaice);
                String[] albedoInputProductBinaryFilenames = new String[albedoInputProductBinaryFileList.size()];
                int binaryProductIndex = 0;
                for (String albedoInputProductBinaryName : albedoInputProductBinaryFileList) {
                    String productYearRootDir;
                    // e.g. get '2006' from 'matrices_2006xxx.bin'...
                    final String thisProductYear = albedoInputProductBinaryName.substring(9, 13);
                    if (computeSnow) {
                        productYearRootDir = accumulatorRootDir.concat(
                                File.separator + thisProductYear + File.separator + tile + File.separator + "Snow");
                    } else if (computeSeaice) {
                        productYearRootDir = accumulatorRootDir.concat(
                                File.separator + thisProductYear + File.separator + tile);
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
        }

        return inputProduct;
    }

    static List<String> getAlbedoInputProductFileNames(String accumulatorRootDir, final boolean isBinaryFiles, int doy,
                                                       final int year, String tile,
                                                       int wings,
                                                       boolean computeSnow, boolean computeSeaice) {
        List<String> albedoInputProductList = new ArrayList<>();

        final FilenameFilter yearFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // accept only years between 1995 and 2010 (GA period), but allow 2011 wings!
                // we assume that wings is max. 540, so they may lap just into previous or next year
                int startYear = 1995;
                for (int i = 0; i <= 16; i++) {
                    String thisYear = (new Integer(startYear + i)).toString();
//                    if (name.equals(thisYear)) {
                    if (name.equals(thisYear) && Math.abs((startYear + i) - year) <= 1) {
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

        final String[] accumulatorYears = (new File(accumulatorRootDir)).list(yearFilter);

        doy = doy + 8; // 'MODIS day'

        // fill the name list year by year...
        for (String accProductYear : accumulatorYears) {
            String thisYearsRootDir;
            if (computeSnow) {
                thisYearsRootDir = accumulatorRootDir.concat(
                        File.separator + accProductYear + File.separator + tile + File.separator + "Snow");
            } else if (computeSeaice) {
                thisYearsRootDir = accumulatorRootDir.concat(
                        File.separator + accProductYear + File.separator + tile);
            } else {
                thisYearsRootDir = accumulatorRootDir.concat(
                        File.separator + accProductYear + File.separator + tile + File.separator + "NoSnow");
            }
            final String[] thisYearAlbedoInputFiles = (new File(thisYearsRootDir)).list(inputProductNameFilter);

            if (thisYearAlbedoInputFiles != null && thisYearAlbedoInputFiles.length > 0) {
                for (String s : thisYearAlbedoInputFiles) {
                    if (s.startsWith("matrices_" + accProductYear) && !albedoInputProductList.contains(s)) {
                        if (isInWingsInterval(wings, year, doy, tile, s)) {
                            albedoInputProductList.add(s);
                        }
                    }
                }
            }
        }

        Collections.sort(albedoInputProductList);
        return albedoInputProductList;
    }

    static boolean isInWingsInterval(int wings, int processYear, int processDoy, String tile, String accName) {
        // check the 'wings' condition...
        boolean isInWingsInterval = false;
        try {
            // e.g. matrices_2005100.bin
            final int accYear = Integer.parseInt(accName.substring(9, 13));
            final int accDoy = Integer.parseInt(accName.substring(13, 16));
            // make sure that we always cover a period with daylight at the poles
            int offset = isPolarTile(tile) ? Math.max(180, wings/2) : wings/2;
            //    # Left wing
            if (365 + (processDoy - wings) <= 366) {
                int firstLeftDoy = Math.min(365, Math.max(1, 366 - offset));
                if (accDoy >= firstLeftDoy && accDoy >= 366 + (processDoy - offset) && accYear < processYear) {
                    isInWingsInterval = true;
                }
            }
            //    # Center
            if (!isInWingsInterval) {
                if ((accDoy < processDoy + offset) && (accDoy >= processDoy - offset) && (accYear == processYear)) {
                    isInWingsInterval = true;
                }
            }
            //    # Right wing
            if (!isInWingsInterval) {
                if ((processDoy + wings) - 365 > 0) {
                    int lastRightDoy = Math.max(1, Math.min(365, offset));
                    if (accDoy <= lastRightDoy && accDoy <= (processDoy + offset - 365) && accYear > processYear) {
                        isInWingsInterval = true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            BeamLogManager.getSystemLogger().log(Level.ALL,
                    "Cannot determine wings for accumulator '" + accName + "' - skipping.");
        }
        return isInWingsInterval;
    }

    private static boolean isPolarTile(String tile) {
        return tile.endsWith("00") || tile.endsWith("01") || tile.endsWith("16") || tile.endsWith("17");
    }

    public static final Map<Integer, String> waveBandsOffsetMap = new HashMap<>();

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
        bandNames[index] = AlbedoInversionConstants.ACC_MASK_NAME;

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

//    public static String[][] getNewPriorCovarianceBandNames() {
//        String bandNames[][] = new String[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS]
//                [3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS];
//        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
//            // only UR triangle matrix
//            for (int j = i; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
//                bandNames[i][j] = "Cov_" + waveBandsOffsetMap.get(i / 3) + "_f" + (i % 3) + "_" +
//                        waveBandsOffsetMap.get(j / 3) + "_f" + (j % 3);
//            }
//        }
//        return bandNames;
//
//    }

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

    public static String[] getAlbedoDhrAlphaBandNames() {
        return new String[]{
                "DHR_alpha_VIS_NIR", "DHR_alpha_VIS_SW", "DHR_alpha_NIR_SW"
        };
    }

    public static String[] getAlbedoBhrAlphaBandNames() {
        return new String[]{
                "BHR_alpha_VIS_NIR", "BHR_alpha_VIS_SW", "BHR_alpha_NIR_SW"
        };
    }

    public static String[] getAlbedoDhrSigmaBandNames() {
        String bandNames[] = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            bandNames[i] = "DHR_sigma_" + waveBandsOffsetMap.get(i);
        }
        return bandNames;
    }

    public static String[] getAlbedoBhrSigmaBandNames() {
        String bandNames[] = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            bandNames[i] = "BHR_sigma_" + waveBandsOffsetMap.get(i);
        }
        return bandNames;
    }

    public static String[] getPriorMeanBandNames() {
        String bandNames[] = new String[NUM_ALBEDO_PARAMETERS * NUM_ALBEDO_PARAMETERS];
        int index = 0;
        for (int i = 0; i < NUM_ALBEDO_PARAMETERS; i++) {
            for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                bandNames[index++] = "MEAN__BAND________" + i + "_PARAMETER_F" + j;
            }
        }
        return bandNames;
    }

    public static FullAccumulator getAccumulatorFromBinaryFile(int year, int doy, String filename, int numBands,
                                                               int rasterWidth, int rasterHeight, boolean isFullAcc) {
//        int size = numBands * rasterWidth * rasterHeight;         // OLD
        int size = numBands * rasterWidth * rasterHeight * 4; // NEW
        final File accumulatorBinaryFile = new File(filename);
        FileInputStream f;
        try {
            f = new FileInputStream(accumulatorBinaryFile);
        } catch (FileNotFoundException e) {
            BeamLogManager.getSystemLogger().log(Level.ALL, "No accumulator file found for year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " - will use data from MODIS priors...");
            return null;
        }
        FileChannel ch = f.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(size);
        FloatBuffer floatBuffer = bb.asFloatBuffer();

        float[][] daysToTheClosestSample = new float[rasterWidth][rasterHeight];
        float[][][] sumMatrices = new float[numBands][rasterWidth][rasterHeight];

        FullAccumulator accumulator = null;

        try {
            long t1 = System.currentTimeMillis();

            ch.read(bb);
            for (int ii = 0; ii < numBands - 1; ii++) {
                for (int jj = 0; jj < rasterWidth; jj++) {
                    floatBuffer.get(sumMatrices[ii][jj]);
                }
            }
            if (isFullAcc) {
                // skip this for 'daily' accumulators
                for (int jj = 0; jj < rasterWidth; jj++) {
                    floatBuffer.get(daysToTheClosestSample[jj]);
                }
            }
            bb.clear();
            floatBuffer.clear();
            ch.close();
            f.close();

            long t2 = System.currentTimeMillis();
            BeamLogManager.getSystemLogger().log(Level.INFO, "Full accumulator read in: " + (t2 - t1) + " ms");

            accumulator = new FullAccumulator(year, doy, sumMatrices, daysToTheClosestSample);

        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.SEVERE, "Could not read full accumulator file '" + filename +
                    "':  " + e.getMessage());
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

            ByteBuffer bb = ByteBuffer.allocateDirect(dim1 * dim2 * dim3 * 4);
            FloatBuffer floatBuffer = bb.asFloatBuffer();
            for (float[][] value : values) {
                for (int j = 0; j < dim2; j++) {
                    floatBuffer.put(value[j], 0, dim3);
                }
            }
            wChannel.write(bb);

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

            ByteBuffer bb = ByteBuffer.allocateDirect((dim1 + 1) * dim2 * dim3 * 4);
            FloatBuffer floatBuffer = bb.asFloatBuffer();
            for (float[][] sumMatrice : sumMatrices) {
                for (int j = 0; j < dim2; j++) {
                    floatBuffer.put(sumMatrice[j], 0, dim3);
                }
            }
            for (int j = 0; j < dim2; j++) {
                floatBuffer.put(daysClosestSample[j], 0, dim3);
            }
            wChannel.write(bb);

            // Close file when finished with it..
            wChannel.close();
            file_output.close();
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Could not write full accumulator to file:  " + e + " // " + e.getMessage() +
                    " // buffer index =  " + index);
        }
    }

    public IOUtils() {

    }

    public static int getDoyFromAlbedoProductName(String productName) {
        String doyString;
        if (productName.startsWith("GlobAlbedo.albedo.")) {
            doyString = productName.substring(22, 25);
        } else if (productName.startsWith("GlobAlbedo.")) {
            doyString = productName.substring(15, 18);
        } else {
            return -1;
        }
        int doy = Integer.parseInt(doyString);
        if (doy < 0 || doy > 366) {
            return -1;
        }
        return doy;
    }

    public static Product getTileInfoProduct(String dailyAccumulatorDir, String defaultTileInfoFilename) throws IOException {
        String tileInfoFilePath = dailyAccumulatorDir + File.separator + defaultTileInfoFilename;
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
                return null;
            }
        }
        return ProductIO.readProduct(tileInfoFilePath);
    }


    public static void copyLandmask(String gaRootDir, String tile, Product targetProduct) {
        try {
            final Product seaiceLandmaskProduct = IOUtils.getSeaiceLandmaskProduct(gaRootDir, tile);
            if (seaiceLandmaskProduct != null) {
                final Band landmaskBand = seaiceLandmaskProduct.getBand("land_water_fraction");
                ProductUtils.copyBand(landmaskBand.getName(), seaiceLandmaskProduct,
                        "landmask", targetProduct, true);
            }
        } catch (IOException e) {
            System.out.println("Warning: cannot open landmask product for tile '" + tile + "': " +
                    e.getMessage());
        }
    }

    public static Product getSeaiceLandmaskProduct(String gaRootDir, String tile) throws IOException {
        String defaultLandmaskFilename = "GlobAlbedo.landmask." + tile + ".dim";
        String landmaskFilePath = gaRootDir + File.separator + "landmask" + File.separator +
                defaultLandmaskFilename;
        return ProductIO.readProduct(landmaskFilePath);
    }

    public static int getDayDifference(int doy, int year, int referenceDoy, int referenceYear) {
        final int difference = 365 * (year - referenceYear) + (doy - referenceDoy);
        return Math.abs(difference);
    }

    public static Product[] getAlbedo8DayTileProducts(String gaRootDir, final String year, final String tile) {

        final FilenameFilter inputProductNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return ((name.length() == 36 && name.startsWith("GlobAlbedo.albedo.") && name.endsWith(tile + ".dim")) ||
                        (name.length() == 29 && name.startsWith("GlobAlbedo.") && name.endsWith(tile + ".dim")));
            }
        };

        final String albedoDir = gaRootDir + File.separator + "Albedo" + File.separator + year + File.separator + tile + File.separator;
        final String[] albedoFiles = (new File(albedoDir)).list(inputProductNameFilter);

        if (albedoFiles != null && albedoFiles.length > 0) {
            Product[] albedoProducts = new Product[albedoFiles.length];

            int productIndex = 0;
            for (String albedoFile : albedoFiles) {
                String albedoProductFileName = albedoDir + File.separator + albedoFile;

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
        } else {
            return null;
        }

    }

    public static Product[] getAlbedo8DayMosaicProducts(String gaRootDir, final int monthIndex,
                                                        final String year, final String mosaicScaling) {

        final FilenameFilter inputProductNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // todo maybe we need this for optional SIN mosaics?!
                // e.g. GlobAlbedo.albedo.2005129.05.PC.dim
                // final boolean isCorrectSuffix = name.endsWith(reprojection + ".dim") || name.endsWith(reprojection + ".nc");

                // e.g. GlobAlbedo.albedo.2005129.05.dim
                final boolean isCorrectSuffix = name.endsWith(".dim") || name.endsWith(".nc");
                final boolean isCorrectPattern = name.length() > 30 &&
                        name.substring(0, 29).matches("GlobAlbedo.albedo.[0-9]{7}.[0-9]{2}.");
                if (isCorrectSuffix && isCorrectPattern) {
                    final String doy = name.substring(22, 25);
                    for (String doyOfMonth : AlbedoInversionConstants.doysOfMonth[monthIndex - 1]) {
                        if (doy.equals(doyOfMonth)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };

        final String albedoDir = gaRootDir + File.separator + "Mosaic" + File.separator + "albedo" +
                File.separator + year + File.separator + mosaicScaling + File.separator;
        final String[] albedoFiles = (new File(albedoDir)).list(inputProductNameFilter);

        if (albedoFiles != null && albedoFiles.length > 0) {
            Product[] albedoProducts = new Product[albedoFiles.length];

            int productIndex = 0;
            for (String albedoFile : albedoFiles) {
                String albedoProductFileName = albedoDir + File.separator + albedoFile;

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
        } else {
            return null;
        }

    }

    public static File[] getTileDirectories(String rootDirString) {
        final Pattern finalPattern = Pattern.compile("h(\\d\\d)v(\\d\\d)");
        FileFilter tileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && finalPattern.matcher(file.getName()).matches();
            }
        };

        File rootDir = new File(rootDirString);
        return rootDir.listFiles(tileFilter);
    }

    private static List<String> getBrdfProductNames(String[] brdfFiles, boolean snow) {
        List<String> brdfFileList = new ArrayList<>();
        for (String s : brdfFiles) {
            if ((!snow && s.contains(".NoSnow") && (s.endsWith(".dim") || s.endsWith(".nc"))) ||
                    (snow && s.contains(".Snow") && (s.endsWith(".dim") || s.endsWith(".nc")))) {
                brdfFileList.add(s);
            }
        }
        Collections.sort(brdfFileList);
        return brdfFileList;
    }

    private static List<String> getBrdfSeaiceProductNames(String[] brdfFiles) {
        List<String> brdfFileList = new ArrayList<>();
        for (String s : brdfFiles) {
            if (s.contains(".Seaice") && (s.endsWith(".dim") || s.endsWith(".nc"))) {
                brdfFileList.add(s);
            }
        }
        Collections.sort(brdfFileList);
        return brdfFileList;
    }
}
