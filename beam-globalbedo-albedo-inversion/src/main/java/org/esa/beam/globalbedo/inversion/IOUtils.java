package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class IOUtils {

    public static Product[] getAccumulationInputProducts(String bbdrRootDir, String tile, int year, int doy) throws
                                                                                                             IOException {
        String daystring = AlbedoInversionUtils.getDateFromDoy(year, doy);

        String merisBbdrDir = bbdrRootDir + File.separator + "MERIS" + File.separator + year + File.separator + tile;
        String[] merisBbdrFiles = (new File(merisBbdrDir)).list();
        List<String> merisBbdrFileList = getDailyBBDRFilenames(merisBbdrFiles, daystring);

        String aatsrBbdrDir = bbdrRootDir + File.separator + "AATSR" + File.separator + year + File.separator + tile;
        String[] aatsrBbdrFiles = (new File(aatsrBbdrDir)).list();
        List<String> aatsrBbdrFileList = getDailyBBDRFilenames(aatsrBbdrFiles, daystring);

        String vgtBbdrDir = bbdrRootDir + File.separator + "VGT" + File.separator + year + File.separator + tile;
        String[] vgtBbdrFiles = (new File(vgtBbdrDir)).list();
        List<String> vgtBbdrFileList = getDailyBBDRFilenames(vgtBbdrFiles, daystring);

        final int numberOfInputProducts = merisBbdrFileList.size() + aatsrBbdrFileList.size() + vgtBbdrFileList.size();
        Product[] bbdrProducts = new Product[numberOfInputProducts];

        int productIndex = 0;
        for (Iterator<String> i = merisBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = merisBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }
        for (Iterator<String> i = aatsrBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = aatsrBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }
        for (Iterator<String> i = vgtBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = vgtBbdrDir + File.separator + i.next();
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

    public static Product[] getPriorProducts(String priorDir, String tile, boolean computeSnow) throws IOException {

        String[] priorFiles = (new File(priorDir)).list();
        List<String> snowFilteredPriorList = getPriorProductNames(priorFiles, computeSnow);

        Product[] priorProducts = new Product[snowFilteredPriorList.size()];

        int productIndex = 0;
        for (Iterator<String> i = snowFilteredPriorList.iterator(); i.hasNext();) {
            String sourceProductFileName = priorDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            priorProducts[productIndex] = product;
            productIndex++;
        }

        return priorProducts;
    }

    public static Product[] getReprojectedPriorProducts(Product[] priorProducts, String tile,
                                                        Product geoCodingReferenceProduct) {
        Product[] reprojectedProducts = new Product[priorProducts.length];
        for (int i = 0; i < priorProducts.length; i++) {
            Product priorProduct = priorProducts[i];
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

    public static AlbedoInputContainer getAlbedoInputProducts(String accumulatorRootDir, int doy, int year, String tile,
                                                              int wings,
                                                              boolean computeSnow) throws IOException {

        List<String> albedoInputProductList = getAlbedoInputProductNames(accumulatorRootDir, doy, year, tile, wings,
                                                                         computeSnow);

        Product[] albedoInputProducts = new Product[albedoInputProductList.size()];
        int[] albedoInputProductDoys = new int[albedoInputProductList.size()];
        int[] albedoInputProductYears = new int[albedoInputProductList.size()];

        int productIndex = 0;
        for (Iterator<String> i = albedoInputProductList.iterator(); i.hasNext();) {
            String productYearRootDir;
            final String thisProductName = i.next();
            // e.g. get '2006' from 'matrices_2006_xxx.dim'...
            final String thisProductYear = thisProductName.substring(9, 13);
            final String thisProductDoy = thisProductName.substring(14, 17);
            if (computeSnow) {
                productYearRootDir = accumulatorRootDir.concat(
                        File.separator + thisProductYear + File.separator + tile + File.separator + "Snow");
            } else {
                productYearRootDir = accumulatorRootDir.concat(
                        File.separator + thisProductYear + File.separator + tile + File.separator + "NoSnow");
            }

            String sourceProductFileName = productYearRootDir + File.separator + thisProductName;
            Product product = ProductIO.readProduct(sourceProductFileName);
            albedoInputProducts[productIndex] = product;
            albedoInputProductDoys[productIndex] = Integer.parseInt(thisProductDoy);
            albedoInputProductYears[productIndex] = Integer.parseInt(thisProductYear);
            productIndex++;
        }

        AlbedoInputContainer inputProductContainer = new AlbedoInputContainer();
        inputProductContainer.setInputProducts(albedoInputProducts);
        inputProductContainer.setInputProductDoys(albedoInputProductDoys);
        inputProductContainer.setInputProductYears(albedoInputProductYears);

        return inputProductContainer;
    }

    static List<String> getAlbedoInputProductNames(String accumulatorRootDir, int doy, int year, String tile, int wings,
                                                   boolean computeSnow) {
        List<String> albedoInputProductList = new ArrayList<String>();

        FilenameFilter yearFilter = new FilenameFilter() {
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

        FilenameFilter inputProductNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // accept only filenames like 'matrices_2005_123.dim'...
                return (name.length() == 21 && name.startsWith("matrices") && name.endsWith("dim"));
            }
        };

        String[] allYears = (new File(accumulatorRootDir)).list(yearFilter);

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
            String[] thisYearAlbedoInputFiles = (new File(thisYearsRootDir)).list(inputProductNameFilter);
            int[] daysOfThisYear = getDaysOfYear(thisYearAlbedoInputFiles);

            for (String s : thisYearAlbedoInputFiles) {
                if (s.startsWith("matrices_" + thisYear)) {
                    // check the 'wings' condition...
                    //    # Left wing
                    if (365 + (doy - wings) <= 366) {
                        for (int i = 0; i < daysOfThisYear.length; i++) {
                            if (daysOfThisYear[i] >= 366 + (doy - wings) && Integer.parseInt(thisYear) < year) {
                                albedoInputProductList.add(s);
                            }
                        }
                    }
                    //    # Center
                    for (int i = 0; i < daysOfThisYear.length; i++) {
                        if ((daysOfThisYear[i] < doy + wings) && (daysOfThisYear[i] >= doy - wings) &&
                            (Integer.parseInt(thisYear) == year)) {
                            albedoInputProductList.add(s);
                        }
                    }
                    //    # Right wing
                    if ((doy + wings) - 365 > 0) {
                        for (int i = 0; i < daysOfThisYear.length; i++) {
                            if (daysOfThisYear[i] <= (doy + wings - 365) && Integer.parseInt(thisYear) > year) {
                                albedoInputProductList.add(s);
                            }
                        }
                    }
                }
            }
        }

        Collections.sort(albedoInputProductList);
        return albedoInputProductList;
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
