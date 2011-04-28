package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.inversion.AlbedoInput;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            throw new OperatorException("No source products found - check contents of BBDR directory!");
        }

        return bbdrProducts;
    }

    /**
     * Filters from a list of BBDR file names the ones which contain a given daystring
     *
     * @param bbdrFilenames - the list of filenames
     * @param daystring     - the daystring
     *
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

    public static Product[] getPriorProducts(String priorDir, boolean computeSnow) throws IOException {

        final String[] priorFiles = (new File(priorDir)).list();
        final List<String> snowFilteredPriorList = getPriorProductNames(priorFiles, computeSnow);

        Product[] priorProducts = new Product[snowFilteredPriorList.size()];

        int productIndex = 0;
        for (String aSnowFilteredPriorList : snowFilteredPriorList) {
            String sourceProductFileName = priorDir + File.separator + aSnowFilteredPriorList;
            Product product = ProductIO.readProduct(sourceProductFileName);
            priorProducts[productIndex] = product;
            productIndex++;
        }

        return priorProducts;
    }

    public static Product[] getReprojectedPriorProducts(Product[] priorProducts, String tile,
                                                        String sourceProductFileName) throws IOException {
        Product[] reprojectedProducts = new Product[priorProducts.length];
        for (int i = 0; i < priorProducts.length; i++) {
            Product priorProduct = priorProducts[i];
            Product geoCodingReferenceProduct = ProductIO.readProduct(sourceProductFileName);
            ProductUtils.copyGeoCoding(geoCodingReferenceProduct, priorProduct);
            double easting = AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[0];
            double northing = AlbedoInversionUtils.getUpperLeftCornerOfModisTiles(tile)[1];
            reprojectedProducts[i] = AlbedoInversionUtils.reprojectToSinusoidal(priorProduct, easting,
                                                                                northing);
        }
        return reprojectedProducts;
    }

    static List<String> getPriorProductNames(String[] priorFiles, boolean computeSnow) {

        List<String> snowFilteredPriorList = new ArrayList<String>();
        for (String s : priorFiles) {
            if (computeSnow) {
                if (s.endsWith("_Snow.hdr")) {
                    // check if binary file is present:
                    snowFilteredPriorList.add(s);
                }
            } else {
                if (s.endsWith("_NoSnow.hdr")) {
                    snowFilteredPriorList.add(s);
                }
            }
        }
        Collections.sort(snowFilteredPriorList);
        return snowFilteredPriorList;
    }

    public static AlbedoInput getAlbedoInputProducts(String accumulatorRootDir, int doy, int year, String tile,
                                                     int wings,
                                                     boolean computeSnow) throws IOException {

        final List<String> albedoInputProductList = getAlbedoInputProductNames(accumulatorRootDir, doy, year, tile,
                                                                               wings,
                                                                               computeSnow);

        String[] albedoInputProductFilenames = new String[albedoInputProductList.size()];
        int[] albedoInputProductDoys = new int[albedoInputProductList.size()];
        int[] albedoInputProductYears = new int[albedoInputProductList.size()];

        int productIndex = 0;
        for (String anAlbedoInputProductList : albedoInputProductList) {
            String productYearRootDir;
            // e.g. get '2006' from 'matrices_2006_xxx.dim'...
            final String thisProductYear = anAlbedoInputProductList.substring(9, 13);
            final String thisProductDoy = anAlbedoInputProductList.substring(14, 17);
            if (computeSnow) {
                productYearRootDir = accumulatorRootDir.concat(
                        File.separator + thisProductYear + File.separator + tile + File.separator + "Snow");
            } else {
                productYearRootDir = accumulatorRootDir.concat(
                        File.separator + thisProductYear + File.separator + tile + File.separator + "NoSnow");
            }

            String sourceProductFileName = productYearRootDir + File.separator + anAlbedoInputProductList;
            albedoInputProductFilenames[productIndex] = sourceProductFileName;
            albedoInputProductDoys[productIndex] = Integer.parseInt(thisProductDoy) - (doy + 8);
            albedoInputProductYears[productIndex] = Integer.parseInt(thisProductYear);
            productIndex++;
        }

        AlbedoInput inputProduct = new AlbedoInput();
        inputProduct.setProductFilenames(albedoInputProductFilenames);
        inputProduct.setProductDoys(albedoInputProductDoys);
        inputProduct.setProductYears(albedoInputProductYears);

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
     *
     * @return String
     */
    public static String getInversionTargetFileName(int year, int doy, String tile, boolean computeSnow,
                                                    boolean usePrior) {
        String targetFileName;

        //  build up a name like this: GlobAlbedo.2005129.h18v04.NoSnow.bin
        if (computeSnow) {
            if (usePrior) {
                targetFileName = "GlobAlbedo." + year + doy + "." + tile + ".Snow.bin";
            } else {
                targetFileName = "GlobAlbedo." + year + doy + "." + tile + ".Snow.NoPrior.bin";
            }
        } else {
            if (usePrior) {
                targetFileName = "GlobAlbedo." + year + doy + "." + tile + ".NoSnow.bin";
            } else {
                targetFileName = "GlobAlbedo." + year + doy + "." + tile + ".NoSnow.NoPrior.bin";
            }
        }
        return targetFileName;
    }


    static List<String> getAlbedoInputProductNames(String accumulatorRootDir, int doy, int year, String tile, int wings,
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
                // accept only filenames like 'matrices_2005_123.dim'...
                return (name.length() == 21 && name.startsWith("matrices") && name.endsWith("dim"));
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
                            final int dayOfYear = Integer.parseInt(s.substring(14, 17));
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
                            // todo: logging
                            System.out.println("Cannot determine wings for accumulator " + s + " - skipping.");
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

    public static String[] getInversionParameterBandNames() {
        String bandNames[] = new String[AlbedoInversionConstants.numBBDRWaveBands *
                                        AlbedoInversionConstants.numAlbedoParameters];
        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numAlbedoParameters; j++) {
                bandNames[index] = "mean_" + waveBandsOffsetMap.get(i) + "_f" + j;
                index++;
            }
        }
        return bandNames;
    }

    public static String[][] getInversionUncertaintyBandNames() {
        String bandNames[][] = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
                [3 * AlbedoInversionConstants.numAlbedoParameters];
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            // only UR triangle matrix
            for (int j = i; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                bandNames[i][j] = "VAR_" + waveBandsOffsetMap.get(i / 3) + "_f" + (i % 3) + "_" +
                                  waveBandsOffsetMap.get(j / 3) + "_f" + (j % 3);
            }
        }
        return bandNames;

    }

    private static int[] getDaysOfYear(String[] thisYearAlbedoInputFiles) {
        int[] daysOfYear = new int[thisYearAlbedoInputFiles.length];
        for (int i = 0; i < thisYearAlbedoInputFiles.length; i++) {
            final String s = thisYearAlbedoInputFiles[i];
            daysOfYear[i] = Integer.parseInt(s.substring(14, 17));
        }
        return daysOfYear;
    }
}
