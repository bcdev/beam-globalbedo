package org.esa.beam.globalbedo.inversion.singlepixel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.inversion.Accumulator;
import org.esa.beam.globalbedo.inversion.FullAccumulator;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for the whole single pixel BRDF computation (daily acc, full acc, inversion)
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l3.inversion.single",
        description = "'Master' operator for the whole single pixel BRDF computation (daily acc, full acc, inversion)",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class GlobalbedoLevel3InversionSinglePixel extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "BBDR root directory")
    private String bbdrRootDir;

    @Parameter(defaultValue = "", description = "Inversion target directory")
    private String inversionDir;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "180", description = "Wings")  // means 3 months wings on each side of the year
    private int wings;

    @Parameter(defaultValue = "true", description = "Decide whether MODIS priors shall be used in inversion")
    private boolean usePrior = true;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory suffix")
    // e.g., background/processed.p1.0.618034.p2.1.00000
    private String priorRootDirSuffix;

    @Parameter(defaultValue = "kernel", description = "MODIS Prior file name prefix")
    // e.g., filename = kernel.001.006.h18v04.Snow.1km.nc
    private String priorFileNamePrefix;

    //    @Parameter(defaultValue = "MEAN:_BAND_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    // Oct. 2015:
    @Parameter(defaultValue = "Mean_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    //    @Parameter(defaultValue = "SD:_BAND_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    @Parameter(defaultValue = "Cov_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "", description = "Version of BBDR single pixel input (vyyMMdd)")
    private String versionString;

    @Parameter(defaultValue = "", description = "x pixel index in tile (as string with leading zero)")
    private String pixelX;

    @Parameter(defaultValue = "", description = "y pixel index in tile (as string with leading zero)")
    private String pixelY;

    @Parameter(defaultValue = "true", description = "If set, product is written to CSV file following conventions")
    private boolean writeInversionProductToCsv;

    @Parameter(description = "Array of all daily accumulators for the pixel (computed externally) ")
    private Accumulator[] allDailyAccs;

    @SourceProduct(description = "Prior product as single pixel (i.e. CSV)", optional = true)
    private Product priorPixelProduct;

    @Parameter(defaultValue = "5", description = "Prior version (MODIS collection)")  // todo: change default to 6 later
    private int priorVersion;


    private float latitude = Float.NaN;
    private float longitude = Float.NaN;

    @Override
    public void initialize() throws OperatorException {

        Logger logger = BeamLogManager.getSystemLogger();

        if (inversionDir == null) {
            final String snowDir = computeSnow ? "Snow" : "NoSnow";
            inversionDir = gaRootDir + File.separator + "Inversion_single" + File.separator + year +
                    File.separator + tile + File.separator + snowDir;
        }

        // procedure:
        // - input: year, process in memory all doys of this year
        // 1. produce all daily accs in wings period: array dailyAccs[540][81+9+1+1]
        //    (for this, generate test fake data from 2005 to 2007 in folders:
        //    /group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest/BBDR_single/h18v03
        //    /group_workspaces/cems2/qa4ecv/vol1/olafd/GlobAlbedoTest/BBDR_single/h22v07
        // 2. loop 1..46: produce all full accs for doys of year: fullAccs[46][81+9+1+1]]
        // 3. loop 1..46: produce all BRDF products for doys of year and snow/nosnow (write to single-pixel csv files)

        // maybe in separate module (use MergeBrdfOp):
        // 4. loop 1..46: produce all merged BRDF products for doys of year (write to single-pixel csv files)
        // maybe in separate module (use BrdfToAlbedoOp):
        // 5. loop 1..46: produce all Albedo products from merged BRDF products for doys of year (write to single-pixel csv files)

        if (allDailyAccs == null) {
            List<Accumulator> allDailyAccsList = new ArrayList<>();
            for (int iDay = -90; iDay < 455; iDay++) {
                int currentYear = year;
                int currentDay = iDay;          // currentDay is in [0,365] !!
                if (iDay < 0) {
                    currentYear--;
                    currentDay = iDay + 365;
                } else if (iDay > 365) {
                    currentYear++;
                    currentDay = iDay - 365;
                }

                Product[] inputProducts;
                try {
                    // STEP 1: get BBDR input product list...
                    final List<Product> inputProductList =
                            IOUtils.getAccumulationSinglePixelInputProducts(bbdrRootDir, tile, currentYear, currentDay,
                                                                            pixelX, pixelY, versionString);
                    inputProducts = inputProductList.toArray(new Product[inputProductList.size()]);
                } catch (IOException e) {
                    throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
                }

                if (inputProducts.length > 0) {
                    // STEP 2: do daily accumulation

                    if (Float.isNaN(latitude) || Float.isNaN(longitude)) {
                        final GeoPos latLon = AlbedoInversionUtils.getLatLonFromProduct(inputProducts[0]);
                        latitude = latLon.getLat();
                        longitude = latLon.getLon();
                    }

                    logger.log(Level.ALL, "Daily acc 'single': tile: " +
                            tile + ", year: " + currentYear + ", day: " + IOUtils.getDoyString(currentDay));
                    for (Product inputProduct : inputProducts) {
                        logger.log(Level.ALL, "       - ': " + inputProduct.getName());
                    }
                    DailyAccumulationSinglePixel dailyAccumulationSinglePixel =
                            new DailyAccumulationSinglePixel(inputProducts, computeSnow);
                    final Accumulator dailyAcc = dailyAccumulationSinglePixel.accumulate();
                    allDailyAccsList.add(dailyAcc);
                } else {
                    allDailyAccsList.add(Accumulator.createZeroAccumulator());
                }
            }
            allDailyAccs = allDailyAccsList.toArray(new Accumulator[allDailyAccsList.size()]);
        }

        // STEP 2: Full accumulation
        FullAccumulationSinglePixel fullAccumulationSinglePixel = new FullAccumulationSinglePixel(year, doy);
        FullAccumulator fullAcc = fullAccumulationSinglePixel.accumulate(allDailyAccs);

        // STEP 3: Inversion
        Product priorProduct = null;
        if (usePrior && priorPixelProduct == null) {
            // STEP 1: get Prior input file...
            String priorDir = priorRootDir + File.separator + tile;
            if (priorRootDirSuffix != null) {
                priorDir = priorDir.concat(File.separator + priorRootDirSuffix);
            }

            logger.log(Level.INFO, "No single pixel Prior set - searching for prior file in directory: '" +
                    priorDir + "'...");

            try {
                priorProduct = IOUtils.getPriorProduct(priorVersion, priorDir, priorFileNamePrefix, doy, computeSnow);
            } catch (IOException e) {
                throw new OperatorException("No prior file available for DoY " + IOUtils.getDoyString(doy) +
                                                    " - cannot proceed...: " + e.getMessage());
            }
        }

        if (!usePrior || (usePrior && (priorProduct != null || priorPixelProduct != null))) {
            // STEP 3: we need to attach a geocoding to the Prior product...
            Product priorSinglePixelProduct = null;
            if (usePrior) {
                if (priorProduct != null) {
                    try {
                        IOUtils.attachGeoCodingToPriorProduct(priorProduct, tile, null);
                        SubsetOp subsetOp = new SubsetOp();
                        subsetOp.setParameterDefaultValues();
                        subsetOp.setSourceProduct(priorProduct);
                        subsetOp.setRegion(new Rectangle(Integer.parseInt(pixelX), Integer.parseInt(pixelY), 1, 1));
                        priorSinglePixelProduct = subsetOp.getTargetProduct();
                        if (priorSinglePixelProduct.getSceneRasterWidth() != 1 ||
                                priorSinglePixelProduct.getSceneRasterHeight() != 1) {
                            logger.log(Level.WARNING, ("Prior subset does not match 1x1 dimension - will not use this Prior!"));
                            usePrior = false;
                        }
                    } catch (IOException e) {
                        throw new OperatorException("Cannot reproject prior products - cannot proceed: " + e.getMessage());
                    }
                } else if (priorPixelProduct != null) {
                    priorSinglePixelProduct = priorPixelProduct;
                } else {
                    logger.log(Level.WARNING, "No prior file found for tile: " + tile + ", year: " + year + ", DoY: " +
                            IOUtils.getDoyString(doy) + " , Snow = " + computeSnow + " - no inversion performed.");
                }
            }

            // STEP 5: do inversion...

            InversionSinglePixelOp inversionSinglePixelOp = new InversionSinglePixelOp();
            inversionSinglePixelOp.setParameterDefaultValues();
            Product dummySourceProduct;
            if (usePrior && priorSinglePixelProduct != null) {
                inversionSinglePixelOp.setSourceProduct("priorProduct", priorSinglePixelProduct);
            } else {
                dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(1, 1);
                inversionSinglePixelOp.setSourceProduct("priorProduct", dummySourceProduct);
            }

            inversionSinglePixelOp.setParameter("year", year);
            inversionSinglePixelOp.setParameter("tile", tile);
            inversionSinglePixelOp.setParameter("doy", doy);
            inversionSinglePixelOp.setParameter("latitude", latitude);
            inversionSinglePixelOp.setParameter("longitude", longitude);
            inversionSinglePixelOp.setParameter("fullAccumulator", fullAcc);
            inversionSinglePixelOp.setParameter("computeSnow", computeSnow);
            inversionSinglePixelOp.setParameter("usePrior", usePrior);
            inversionSinglePixelOp.setParameter("priorMeanBandNamePrefix", priorMeanBandNamePrefix);
            inversionSinglePixelOp.setParameter("priorSdBandNamePrefix", priorSdBandNamePrefix);
            Product inversionProduct = inversionSinglePixelOp.getTargetProduct();

            if (priorProduct == null) {
                // same in the standard mode without using priors...
                inversionProduct.setGeoCoding(IOUtils.getSinusoidalTileGeocoding(tile));
            }

            if (writeInversionProductToCsv) {
                writeCsvProduct(inversionProduct, getInversionFilename(doy));
            } else {
                setTargetProduct(inversionProduct);
            }

            logger.log(Level.INFO, "Finished inversion process for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);

            if (writeInversionProductToCsv) {
                setTargetProduct(new Product("dummy", "dummy", 1, 1));
            }
        }
    }

    private String getInversionFilename(int doy) {
        // we want e.g.:
        // ../GlobAlbedoTest/Inversion_single/Snow/2005/h18v03/
        //   Qa4ecv.brdf.single.2005121.h18v04.<pixelX>.<pixelY>.Snow.nc
        final String snowString = computeSnow ? "Snow" : "NoSnow";
        final String inversionFileName = "Qa4ecv.brdf.single." + year + "." + IOUtils.getDoyString(doy) + "." + tile + "." +
                pixelX + "." + pixelY + "." + snowString;
        return inversionFileName;
    }


    private void writeCsvProduct(Product product, String productName) {
        File dir = new File(inversionDir);
        dir.mkdirs();
        File file = new File(dir, productName + ".csv");
        WriteOp writeOp = new WriteOp(product, file, "CSV");
        writeOp.writeProduct(ProgressMonitor.NULL);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3InversionSinglePixel.class);
        }
    }
}
