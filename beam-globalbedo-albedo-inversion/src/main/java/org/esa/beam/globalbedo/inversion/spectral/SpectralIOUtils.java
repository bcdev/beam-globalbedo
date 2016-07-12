package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.globalbedo.inversion.AccumulatorHolder;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;

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
                bandNames[index] = "mean_lambda" + (i + 1) + "_f" + j;
                index++;
            }
        }
        return bandNames;
    }

    public static String[][] getSpectralInversionUncertaintyBandNames(int numSdrBands,
                                                                      Map<Integer, String> spectralWaveBandsMap) {
        String bandNames[][] = new String[3 * numSdrBands][3 * numSdrBands];

        for (int i = 0; i < 3 * numSdrBands; i++) {
            // only UR triangle matrix
            for (int j = i; j < 3 * numSdrBands; j++) {
                bandNames[i][j] = "VAR_" + spectralWaveBandsMap.get(i / 3) + "_f" + (i % 3) + "_" +
                        spectralWaveBandsMap.get(j / 3) + "_f" + (j % 3);
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
                                                                                                 computeSnow,
                                                                                                 computeSeaice);
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

                String sourceProductBinaryFileName = productYearRootDir + File.separator + albedoInputProductBinaryName;
                albedoInputProductBinaryFilenames[binaryProductIndex] = sourceProductBinaryFileName;
                binaryProductIndex++;
            }
            accumulatorHolder.setProductBinaryFilenames(albedoInputProductBinaryFilenames);
        }

        return accumulatorHolder;
    }

    static List<String> getDailyAccumulatorBinaryFileNames(String accumulatorRootDir, final int doyIn,
                                                           final int year, String tile,
                                                           final int subStartX, final int subStartY,
                                                           int wings,
                                                           boolean computeSnow, boolean computeSeaice) {
        List<String> accumulatorNameList = new ArrayList<>();

        final FilenameFilter yearFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // accept only years between 1995 and 2010 (GA period), but allow 2011 wings!
                // we assume that wings is max. 540, so they may lap just into previous or next year
                int startYear = 1995;
                for (int i = 0; i <= 16; i++) {
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
                return name.startsWith("matrices_" + Integer.toString(year) + Integer.toString(doyIn)) &&
                        name.endsWith(".bin");
            }
        };

        final String[] accumulatorYears = (new File(accumulatorRootDir)).list(yearFilter);

        int doy = doyIn + 8; // 'MODIS day'
        doy = Math.min(doy + 8, 365); // at least we need to set this limit

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

    public static String[] getDailyAccumulatorFilenameList(AccumulatorHolder accumulatorHolder) {
        List<String> filenameList = new ArrayList<>();
        if (accumulatorHolder != null) {
            final String[] thisInputFilenames = accumulatorHolder.getProductBinaryFilenames();
            if (accumulatorHolder.getProductBinaryFilenames().length == 0) {
                BeamLogManager.getSystemLogger().log(Level.ALL, "No daily accumulators found for DoY " +
                        IOUtils.getDoyString(accumulatorHolder.getReferenceDoy()) + " ...");
            }
            int index = 0;
            while (index < thisInputFilenames.length) {
                if (!filenameList.contains(thisInputFilenames[index])) {
                    filenameList.add(thisInputFilenames[index]);
                }
                index++;
            }
        }

        return filenameList.toArray(new String[filenameList.size()]);
    }

}
