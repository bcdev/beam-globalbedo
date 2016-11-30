package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.AccumulatorHolder;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.07.2016
 * Time: 15:50
 *
 * @author olafd
 */
public class SpectralIOUtils {

    public static String[] getSpectralInversionParameterBandNames(int numSdrBands) {
        String bandNames[] = new String[numSdrBands * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS];
        int index = 0;
        for (int i = 0; i < numSdrBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                bandNames[index] = "mean_b" + (i + 1) + "_f" + j;
                index++;
            }
        }
        return bandNames;
    }

    public static String[][] getSpectralInversionUncertaintyBandNames(int numSdrBands,
                                                                      Map<Integer, String> spectralWaveBandsMap) {
        String bandNames[][] = new String[3 * numSdrBands][3 * numSdrBands];

        for (int i = 0; i < 3 * numSdrBands; i++) {
            // only UR triangle matrix: 0.5*(21*21 - diag) + diag = 0.5*420 + 21 = 231
            for (int j = i; j < 3 *  numSdrBands; j++) {
                bandNames[i][j] = "VAR_" + spectralWaveBandsMap.get(i/3) + "_f" + (i % 3) + "_" +
                        spectralWaveBandsMap.get(j/3) + "_f" + (j % 3);
            }
        }

        return bandNames;
    }


    public static Product[] getSpectralAccumulationInputProducts(String sdrRootDir, String[] sensors,
                                                                 int subStartX, int subStartY,
                                                                 String tile, int year, int doy) throws IOException {
        final String daystring_yyyymmdd = AlbedoInversionUtils.getDateFromDoy(year, doy);
        final String daystring_yyyy_mm_dd = AlbedoInversionUtils.getDateFromDoy(year, doy, "yyyy_MM_dd");  // for AVHRR products

        List<Product> bbdrProductList = new ArrayList<>();
        if (StringUtils.isNotNullAndNotEmpty(daystring_yyyymmdd)) {
            for (String sensor : sensors) {
                int numProducts = 0;
                final String subTileDir = "SUB_" + Integer.toString(subStartX) + "_" + Integer.toString(subStartY);
                final String sensorBbdrDirName =
                        sdrRootDir + File.separator + sensor + File.separator + year + File.separator +
                                tile + File.separator + subTileDir;
                final File sensorBbdrDir = new File(sensorBbdrDirName);
                if (sensorBbdrDir.exists()) {
                    final String[] sensorBbdrFiles = sensorBbdrDir.list();
                    for (String sensorBbdrFile : sensorBbdrFiles) {
                        if ((sensorBbdrFile.endsWith(".nc") || sensorBbdrFile.endsWith(".nc.gz")) &&
                                sensorBbdrFile.contains(subTileDir) &&
                                (sensorBbdrFile.contains(daystring_yyyymmdd) || sensorBbdrFile.contains(daystring_yyyy_mm_dd))) {
                            final String sourceProductFileName = sensorBbdrDirName + File.separator + sensorBbdrFile;
                            Product product = ProductIO.readProduct(sourceProductFileName);
                            if (product != null) {
                                bbdrProductList.add(product);
                                numProducts++;
                            }
                        }
                    }
                }
                BeamLogManager.getSystemLogger().log
                        (Level.INFO, "Collecting Daily accumulation BBDR/SDR products for tile/year/doy: " + tile + "/" + year + "/" + IOUtils.getDoyString(doy) + ": ");
                BeamLogManager.getSystemLogger().log
                        (Level.INFO, "      Sensor '" + sensor + "': " + numProducts + " products added.");
            }
        }
        return bbdrProductList.toArray(new Product[bbdrProductList.size()]);
    }

    public static AccumulatorHolder getDailyAccumulator(String accumulatorRootDir,
                                                        int doy, int year, String tile,
                                                        int subStartX, int subStartY,
                                                        int wings,
                                                        boolean computeSnow,
                                                        boolean computeSeaice) {

        AccumulatorHolder accumulatorHolder = null;

        final List<String> albedoInputProductBinaryFileList = getDailyAccumulatorBinaryFileNames(accumulatorRootDir,
                                                                                                 doy, year, tile,
                                                                                                 subStartX, subStartY,
                                                                                                 wings,
                                                                                                 computeSnow);

        final String subTileDir = "SUB_" + Integer.toString(subStartX) + "_" + Integer.toString(subStartY);

        if (albedoInputProductBinaryFileList.size() > 0) {
            accumulatorHolder = new AccumulatorHolder();
            accumulatorHolder.setReferenceYear(year);
            accumulatorHolder.setReferenceDoy(doy);
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
                productYearRootDir = productYearRootDir.concat(File.separator + subTileDir);

                String sourceProductBinaryFileName = productYearRootDir + File.separator + albedoInputProductBinaryName;
                albedoInputProductBinaryFilenames[binaryProductIndex] = sourceProductBinaryFileName;
                binaryProductIndex++;
            }
            accumulatorHolder.setProductBinaryFilenames(albedoInputProductBinaryFilenames);
        }

        return accumulatorHolder;
    }

    static List<String> getDailyAccumulatorBinaryFileNames(String accumulatorRootDir, final int doy,
                                                           final int year, String tile,
                                                           final int subStartX, final int subStartY,
                                                           int wings,
                                                           boolean computeSnow) {
        List<String> accumulatorNameList = new ArrayList<>();

        final FilenameFilter yearFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // for QA4ECV, accept years between 1982 and 2016 (more than GA period!)
                int startYear = 1982;
                for (int i = 0; i <= 35; i++) {
                    String thisYear = (new Integer(startYear + i)).toString();
                    if (name.equals(thisYear) && Math.abs((startYear + i) - year) <= 1) {
                        return true;
                    }
                }
                return false;
            }
        };

        final FilenameFilter accumulatorNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // accept only filenames like 'matrices_2005123_SUB_300_600.bin'...
                return name.startsWith("matrices_") && name.endsWith(".bin");
            }
        };

        final String[] accumulatorYears = (new File(accumulatorRootDir)).list(yearFilter);

        // keep SAME day now, as we support daily albedos!
//        int doy = doyIn + 8; // 'MODIS day'
//        doy = Math.min(doy + 8, 365); // at least we need to set this limit

        final String subTileDir = "SUB_" + Integer.toString(subStartX) + "_" + Integer.toString(subStartY);

        // fill the name list year by year...
        for (String accProductYear : accumulatorYears) {
            String thisYearsRootDir;
            if (computeSnow) {
                thisYearsRootDir = accumulatorRootDir.concat(
                        File.separator + accProductYear + File.separator + tile + File.separator + "Snow");
            } else {
                thisYearsRootDir = accumulatorRootDir.concat(
                        File.separator + accProductYear + File.separator + tile + File.separator + "NoSnow");
            }
            thisYearsRootDir = thisYearsRootDir.concat(File.separator + subTileDir);
            final String[] thisYearAccumulatorFiles = (new File(thisYearsRootDir)).list(accumulatorNameFilter);

            // matrices_2004273_SUB_300_600.bin, matrices_2004365_SUB_300_600.bin,
            // matrices_2005001_SUB_300_600.bin, matrices_2005002_SUB_300_600.bin, ... , matrices_2005365_SUB_300_600.bin,
            // matrices_2006001_SUB_300_600.bin, matrices_2006090_SUB_300_600.bin
            if (thisYearAccumulatorFiles != null && thisYearAccumulatorFiles.length > 0) {
                for (String s : thisYearAccumulatorFiles) {
                    if (s.startsWith("matrices_" + accProductYear) && !accumulatorNameList.contains(s)) {
                        if (IOUtils.isInWingsInterval(wings, year, doy, tile, s)) {
                            accumulatorNameList.add(s);
                        }
                    }
                }
            }
        }

        return sortAccumulatorFileList(accumulatorNameList, year, doy, subTileDir);
    }

    static List<String> sortAccumulatorFileList(List<String> accumulatorNameList, int refYear, int refDoy,
                                                String subTileDir) {
        // we want to sort from center:
        // e.g. reference date: 2005121
        // -->  matrices_2005121.bin, matrices_2005122.bin, matrices_2005120.bin, matrices_2005123.bin, , matrices_2005119.bin...
        // and stop if distance weight is lower than some minimum (e.g. 5%) and we have enough (e.g. 60) accumulators...

        List<String> accumulatorNameSortedList = new ArrayList<>();

        int doyPlus = refDoy;
        int doyMinus = refDoy;

        while (doyMinus > 0 && doyPlus < 366) {
            String accName = "matrices_" + Integer.toString(refYear) + String.format("%03d", doyMinus) + "_" + subTileDir + ".bin";
            if (accumulatorNameList.contains(accName) && !accumulatorNameSortedList.contains(accName) &&
                    (AlbedoInversionUtils.getWeight(refDoy - doyMinus) > 0.05 ||
                            accumulatorNameSortedList.size() < 60)) {
                accumulatorNameSortedList.add(accName);
            }
            doyMinus--;

            accName = "matrices_" + Integer.toString(refYear) + String.format("%03d", doyPlus) + "_" + subTileDir + ".bin";
            if (accumulatorNameList.contains(accName) && !accumulatorNameSortedList.contains(accName) &&
                    (AlbedoInversionUtils.getWeight(doyPlus - refDoy) > 0.05 ||
                            accumulatorNameSortedList.size() < 60)) {
                accumulatorNameSortedList.add(accName);
            }
            doyPlus++;
        }

        if (accumulatorNameSortedList.size() < 60) {
            if (doyMinus == 0) {
                int doyMinus2 = 365;
                while (doyMinus2 > 180 && doyPlus < 366) {
                    String accName = "matrices_" + Integer.toString(refYear - 1) + String.format("%03d", doyMinus2) + "_" + subTileDir + ".bin";
                    if (accumulatorNameList.contains(accName) && !accumulatorNameSortedList.contains(accName) &&
                            (AlbedoInversionUtils.getWeight(refDoy + 365 - doyMinus2) > 0.05 ||
                                    accumulatorNameSortedList.size() < 60)) {
                        accumulatorNameSortedList.add(accName);
                    }
                    doyMinus2--;

                    accName = "matrices_" + Integer.toString(refYear) + String.format("%03d", doyPlus) + "_" + subTileDir + ".bin";
                    if (accumulatorNameList.contains(accName) && !accumulatorNameSortedList.contains(accName) &&
                            (AlbedoInversionUtils.getWeight(doyPlus - refDoy) > 0.05 ||
                                    accumulatorNameSortedList.size() < 60)) {
                        accumulatorNameSortedList.add(accName);
                    }
                    doyPlus++;
                }
            } else if (doyPlus == 366) {
                int doyPlus2 = 0;
                while (doyPlus2 < 180 && doyMinus > 0) {
                    String accName = "matrices_" + Integer.toString(refYear) + String.format("%03d", doyMinus) + "_" + subTileDir + ".bin";
                    if (accumulatorNameList.contains(accName) && !accumulatorNameSortedList.contains(accName) &&
                            (AlbedoInversionUtils.getWeight(refDoy - doyMinus) > 0.05 ||
                                    accumulatorNameSortedList.size() < 60)) {
                        accumulatorNameSortedList.add(accName);
                    }
                    doyMinus--;

                    accName = "matrices_" + Integer.toString(refYear + 1) + String.format("%03d", doyPlus2) + "_" + subTileDir + ".bin";
                    if (accumulatorNameList.contains(accName) && !accumulatorNameSortedList.contains(accName) &&
                            (AlbedoInversionUtils.getWeight(doyPlus2 + 365 - refDoy) > 0.05 ||
                                    accumulatorNameSortedList.size() < 60)) {
                        accumulatorNameSortedList.add(accName);
                    }
                    doyPlus2++;
                }
            }
        }

        Collections.sort(accumulatorNameSortedList);
        return accumulatorNameSortedList;
    }

    public static String[] getSpectralDailyAccumulatorBandNames(int numSdrBands) {

        // from daily accumulation:
        // we have:
        // (3*7) * (3*7) + 3*7 + 1 + 1= 464 elements to store in daily acc :-(

        String[] bandNames = new String[3 * numSdrBands * 3 * numSdrBands +
                3 * numSdrBands + 1 + 1];

        int index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = 0; j < 3 * numSdrBands; j++) {
                bandNames[index++] = "M_" + i + "" + j;
            }
        }

        for (int i = 0; i < 3 * numSdrBands; i++) {
            bandNames[index++] = "V_" + i;
        }

        bandNames[index++] = AlbedoInversionConstants.ACC_E_NAME;
        bandNames[index] = AlbedoInversionConstants.ACC_MASK_NAME;

        return bandNames;
    }

    public static String[] getSpectralAlbedoDhrBandNames(int numSdrBands,
                                                         Map<Integer, String> spectralWaveBandsMap) {
        String bandNames[] = new String[numSdrBands];
        for (int i = 0; i < numSdrBands; i++) {
            bandNames[i] = "DHR_" + spectralWaveBandsMap.get(i);
        }
        return bandNames;
    }

    public static String[] getSpectralAlbedoBhrBandNames(int numSdrBands,
                                                         Map<Integer, String> spectralWaveBandsMap) {
        String bandNames[] = new String[numSdrBands];
        for (int i = 0; i < numSdrBands; i++) {
            bandNames[i] = "BHR_" + spectralWaveBandsMap.get(i);
        }
        return bandNames;
    }

    public static String[][] getSpectralAlbedoAlphaBandNames(String type, int numSdrBands,
                                                             Map<Integer, String> spectralWaveBandsMap) {
        // we need:
        // b1_b2  b1_b3  b1_b4  b1_b5  b1_b6  b1_b7
        //        b2_b3  b1_b4  b1_b5  b1_b6  b1_b7
        //               b3_b4  b1_b5  b1_b6  b1_b7
        //                      b4_b5  b1_b6  b1_b7
        //                             b5_b6  b1_b7
        //                                    b6_b7

        String bandNames[][] = new String[numSdrBands-1][numSdrBands-1];
        for (int i = 0; i < numSdrBands-1; i++) {
            for (int j = i; j < numSdrBands-1; j++) {
                bandNames[i][j] = type + "_alpha_" + spectralWaveBandsMap.get(i) + "_" + spectralWaveBandsMap.get(j);
            }
        }

        return bandNames;
    }

    public static String[] getSpectralAlbedoDhrSigmaBandNames(int numSdrBands,
                                                              Map<Integer, String> spectralWaveBandsMap) {
        String bandNames[] = new String[numSdrBands];
        for (int i = 0; i < numSdrBands; i++) {
            bandNames[i] = "DHR_sigma_" + spectralWaveBandsMap.get(i);
        }
        return bandNames;
    }

    public static String[] getSpectralAlbedoBhrSigmaBandNames(int numSdrBands,
                                                              Map<Integer, String> spectralWaveBandsMap) {
        String bandNames[] = new String[numSdrBands];
        for (int i = 0; i < numSdrBands; i++) {
            bandNames[i] = "BHR_sigma_" + spectralWaveBandsMap.get(i);
        }
        return bandNames;
    }

    public static ModisTileGeoCoding getSinusoidalSubtileGeocoding(String tile, int startX, int startY) {
        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        int tileIndex = modisTileCoordinates.findTileIndex(tile);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tile + "");
        }

        final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X;
        final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y;
        final double easting = modisTileCoordinates.getUpperLeftX(tileIndex) + startX * pixelSizeX;
        final double northing = modisTileCoordinates.getUpperLeftY(tileIndex) - startY * pixelSizeY;
        final String crsString = AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING;
        ModisTileGeoCoding geoCoding;
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            geoCoding = new ModisTileGeoCoding(crs, easting, northing, pixelSizeX, pixelSizeY);
        } catch (Exception e) {
            throw new OperatorException("Cannot attach geocoding for tileName= ''" + tile + " : ", e);
        }
        return geoCoding;
    }

    public static Product getSpectralBrdfProduct(String brdfDir, int year, int doy, boolean isSnow,
                                                 int subStartX, int subStartY) throws IOException {
        final String[] brdfFiles = (new File(brdfDir)).list();
        final String subTileString = Integer.toString(subStartX) + "_" + Integer.toString(subStartY);
        final List<String> brdfFileList = getSpectralBrdfProductNames(brdfFiles, isSnow, subTileString);

        final String doyString = IOUtils.getDoyString(doy);

        for (String brdfFileName : brdfFileList) {
            if (brdfFileName.startsWith("GlobAlbedo.brdf.spectral." + Integer.toString(year) + doyString)) {
                String sourceProductFileName = brdfDir + File.separator + brdfFileName;
                return ProductIO.readProduct(sourceProductFileName);
            }
        }

        return null;
    }

    private static List<String> getSpectralBrdfProductNames(String[] brdfFiles, boolean snow, String subTileString) {
        List<String> brdfFileList = new ArrayList<>();
        for (String s : brdfFiles) {
            if ((!snow && s.contains(".NoSnow") && s.contains(subTileString) && s.endsWith(".nc")) ||
                    (snow && s.contains(".Snow") && s.contains(subTileString) && s.endsWith(".nc"))) {
                brdfFileList.add(s);
            }
        }
        Collections.sort(brdfFileList);
        return brdfFileList;
    }


}
