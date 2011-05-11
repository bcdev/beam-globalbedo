package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.inversion.AlbedoInput;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

    public static Product getPriorProduct(String priorDir, int doy, boolean computeSnow) throws IOException {

        final String[] priorFiles = (new File(priorDir)).list();
        final List<String> snowFilteredPriorList = getPriorProductNames(priorFiles, computeSnow);

        String doyString = Integer.toString(doy);
        if (doy < 10) {
            doyString = "00" + doyString;
        } else if (doy < 100) {
            doyString = "0" + doyString;
        }

        for (String priorFileName : snowFilteredPriorList) {
            if (priorFileName.startsWith("Kernels_" + doyString)) {
                String sourceProductFileName = priorDir + File.separator + priorFileName;
                Product product = ProductIO.readProduct(sourceProductFileName);
                return product;
            }
        }

        return null;
    }

    public static Product getReprojectedPriorProduct(Product priorProduct, String tile,
                                                     String sourceProductFileName) throws IOException {
        Product geoCodingReferenceProduct = ProductIO.readProduct(sourceProductFileName);
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

        String doyString = Integer.toString(doy);
        if (doy < 10) {
            doyString = "00" + doyString;
        } else if (doy < 100) {
            doyString = "0" + doyString;
        }

        for (String brdfFileName : brdfFileList) {
            if (brdfFileName.startsWith("GlobAlbedo." + Integer.toString(year) + doyString)) {
                String sourceProductFileName = brdfDir + File.separator + brdfFileName;
                Product product = ProductIO.readProduct(sourceProductFileName);
                return product;
            }
        }

        return null;
    }

    static List<String> getPriorProductNames(String[] priorFiles, boolean computeSnow) {

        List<String> snowFilteredPriorList = new ArrayList<String>();
        for (String s : priorFiles) {
            if ((computeSnow && s.endsWith("_Snow.hdr")) || (!computeSnow && s.endsWith("_NoSnow.hdr"))) {
                    snowFilteredPriorList.add(s);
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
                                                    boolean computeSnow) throws IOException {

        final List<String> albedoInputProductList = getAlbedoInputProductFileNames(accumulatorRootDir, false, doy, year,
                                                                                   tile,
                                                                                   wings,
                                                                                   computeSnow);

        String[] albedoInputProductFilenames = new String[albedoInputProductList.size()];

        int[] albedoInputProductDoys = new int[albedoInputProductList.size()];
        int[] albedoInputProductYears = new int[albedoInputProductList.size()];

        int productIndex = 0;
        for (String albedoInputProductName : albedoInputProductList) {

            String productYearRootDir;
            // e.g. get '2006' from 'matrices_2006_xxx.dim'...
            final String thisProductYear = albedoInputProductName.substring(9, 13);
            final String thisProductDoy = albedoInputProductName.substring(14, 17);
            if (computeSnow) {
                productYearRootDir = accumulatorRootDir.concat(
                        File.separator + thisProductYear + File.separator + tile + File.separator + "Snow");
            } else {
                productYearRootDir = accumulatorRootDir.concat(
                        File.separator + thisProductYear + File.separator + tile + File.separator + "NoSnow");
            }

            String sourceProductFileName = productYearRootDir + File.separator + albedoInputProductName;
            albedoInputProductFilenames[productIndex] = sourceProductFileName;
            // todo: add leap year condition
            albedoInputProductDoys[productIndex] = Integer.parseInt(
                    thisProductDoy) - (doy + 8) - 365 * (year - Integer.parseInt(thisProductYear));
            albedoInputProductYears[productIndex] = Integer.parseInt(thisProductYear);
            productIndex++;
        }

        AlbedoInput inputProduct = new AlbedoInput();
        inputProduct.setProductFilenames(albedoInputProductFilenames);
        inputProduct.setProductDoys(albedoInputProductDoys);
        inputProduct.setProductYears(albedoInputProductYears);

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
                // accept only filenames like 'matrices_2005_123.dim'...
                if (isBinaryFiles) {
                    return (name.length() == 20 && name.startsWith("matrices") && name.endsWith("bin"));
                } else {
                    return (name.length() == 21 && name.startsWith("matrices") && name.endsWith("dim"));
                }
            }
        };

        final String[] allYears = (new File(accumulatorRootDir)).list(yearFilter);

        int filenameOffset = 0;
        if (!isBinaryFiles) {
            filenameOffset++;
        }
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
                                    s.substring(13 + filenameOffset, 16 + filenameOffset));
                            // todo: consider leap year condition (not done in the breadboard!!)
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

    public static double[] readBinaryDoubleArray(File file, int size) {
        FileInputStream f = null;
        try {
            f = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        FileChannel ch = f.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(size);
        double[] darray = new double[size];
        int nRead, nGet;
        int index = 0;
        try {
            while ((nRead = ch.read(bb)) != -1) {
                if (nRead == 0) {
                    continue;
                }
                bb.position(0);
                bb.limit(nRead);
                while (bb.hasRemaining()) {
                    nGet = Math.min(bb.remaining(), size);
                    darray[index] = bb.getDouble();
                    index++;
                }
                bb.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return darray;
    }

    public static void writeBinaryDoubleArray3D(File file, double[][][] values) {
        try {
            // Create an output stream to the file.
            FileOutputStream file_output = new FileOutputStream(file);
            // Wrap the FileOutputStream with a DataOutputStream
            DataOutputStream data_out = new DataOutputStream(file_output);

            // Write the data to the file in an integer/double pair
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[i].length; j++) {
                    for (int k = 0; k < values[i][j].length; k++) {
                        data_out.writeDouble(values[i][j][k]);
                    }
                }
            }

            // Close file when finished with it..
            file_output.close();
        } catch (IOException e) {
            System.out.println("IO exception = " + e);
        }
    }

    private static boolean isLeapYear(int year) {
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


}
