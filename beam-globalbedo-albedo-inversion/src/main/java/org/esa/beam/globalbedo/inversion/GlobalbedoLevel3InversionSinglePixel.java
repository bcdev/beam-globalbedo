package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for the whole single pixel BRDF computation (daily acc, full acc, inversion)
 * todo: adapt implementation!
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.inversion.single")
public class GlobalbedoLevel3InversionSinglePixel extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "BBDR root directory")
    private String bbdrRootDir;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

//    @Parameter(description = "Day of Year", interval = "[1,366]")
//    private int doy;

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

    @Parameter(defaultValue = "MEAN:_BAND_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    @Parameter(defaultValue = "SD:_BAND_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "", description = "Version of BBDR single pixel input (vyyMMdd)")
    private String versionString;

    @Parameter(defaultValue = "", description = "x pixel index in tile (as string with leding zero)")
    private String pixelX;

    @Parameter(defaultValue = "", description = "y pixel index in tile (as string with leding zero)")
    private String pixelY;

    @Override
    public void initialize() throws OperatorException {

        Logger logger = BeamLogManager.getSystemLogger();

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

        List<Accumulator> allDailyAccsList = new ArrayList<>();
        for (int iDay = -90; iDay < 455; iDay++) {
            int currentYear = year;
            if (iDay < 0) {
                currentYear--;
                iDay += 365;
            }
            if (iDay > 365) {
                currentYear++;
                currentYear++;
            }
            Product[] inputProducts;
            try {
                // STEP 1: get BBDR input product list...
                // todo: apply pixelX, pixelY and version filter
                final List<Product> inputProductList =
                        IOUtils.getAccumulationSinglePixelInputProducts(bbdrRootDir, tile, currentYear, iDay);
                inputProducts = inputProductList.toArray(new Product[inputProductList.size()]);
            } catch (IOException e) {
                throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
            }

            if (inputProducts.length > 0) {
                // STEP 2: do daily accumulation
                logger.log(Level.ALL, "Daily acc 'single': tile: " + tile + ", year: " + year + ", day: ");
                for (Product inputProduct : inputProducts) {
                    logger.log(Level.ALL, "       - ': " + inputProduct.getName());
                }
                DailyAccumulationSinglePixel dailyAccumulationSinglePixel =
                        new DailyAccumulationSinglePixel(inputProducts, computeSnow);
                final Accumulator dailyAcc = dailyAccumulationSinglePixel.compute();
                allDailyAccsList.add(dailyAcc);
            } else {
                allDailyAccsList.add(Accumulator.createZeroAccumulator());
            }
        }
        final Accumulator[] allDailyAccs = allDailyAccsList.toArray(new Accumulator[allDailyAccsList.size()]);

        for (int doy = 1; doy < 46; doy += 8) {    // todo: use constants
            // STEP 2: do full accumulation
            //  Start full acc for the 46 doys
            FullAccumulationSinglePixel fullAccumulationSinglePixel = new FullAccumulationSinglePixel(year, doy);
            FullAccumulator fullAcc = fullAccumulationSinglePixel.accumulate(allDailyAccs);  // todo


            // STEP 3: Inversion
            Product priorProduct = null;
            if (usePrior) {
                // STEP 1: get Prior input file...
                String priorDir = priorRootDir + File.separator + tile;
                if (priorRootDirSuffix != null) {
                    priorDir = priorDir.concat(File.separator + priorRootDirSuffix);
                }

                logger.log(Level.ALL, "Searching for prior file in directory: '" + priorDir + "'...");

                try {
                    priorProduct = IOUtils.getPriorProduct(priorDir, priorFileNamePrefix, doy, computeSnow);
                } catch (IOException e) {
                    throw new OperatorException("No prior file available for DoY " + IOUtils.getDoyString(doy) +
                                                        " - cannot proceed...: " + e.getMessage());
                }
            }

            if (!usePrior || (usePrior && priorProduct != null)) {
                // STEP 2: set paths...
                final String bbdrString = "BBDR_single";
                final String bbdrRootDir = gaRootDir + File.separator + bbdrString;
                String fullAccumulatorDir = bbdrRootDir + File.separator + "FullAcc"
                        + File.separator + year + File.separator + tile;

                // STEP 3: we need to attach a geocoding to the Prior product...
                if (usePrior) {
                    try {
                        IOUtils.attachGeoCodingToPriorProduct(priorProduct, tile, null);
                    } catch (IOException e) {
                        throw new OperatorException("Cannot reproject prior products - cannot proceed: " + e.getMessage());
                    }
                }

                // STEP 5: do inversion...

                InversionSinglePixelOp inversionSinglePixelOp = new InversionSinglePixelOp();
                inversionSinglePixelOp.setParameterDefaultValues();
                Product dummySourceProduct;
                // todo: properly extract the Prior info for given single pixel:
                // e.g.:
                // - store pixelX, pixelY from original BBDR single pixel files
                // - then take prior product for given year/doy/tile/Xsnow and extract matrix for that pixel
                if (priorProduct != null) {
                    inversionSinglePixelOp.setSourceProduct("priorProduct", priorProduct);
                } else {
                    dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(AlbedoInversionConstants.MODIS_TILE_WIDTH,
                                                                                       AlbedoInversionConstants.MODIS_TILE_HEIGHT);
                    inversionSinglePixelOp.setSourceProduct("priorProduct", dummySourceProduct);
                }
                inversionSinglePixelOp.setParameter("year", year);
                inversionSinglePixelOp.setParameter("tile", tile);
                inversionSinglePixelOp.setParameter("doy", doy);
                inversionSinglePixelOp.setParameter("pixelX", pixelX);
                inversionSinglePixelOp.setParameter("pixelY", pixelY);
                inversionSinglePixelOp.setParameter("computeSnow", computeSnow);
                inversionSinglePixelOp.setParameter("usePrior", usePrior);
                inversionSinglePixelOp.setParameter("priorMeanBandNamePrefix", priorMeanBandNamePrefix);
                inversionSinglePixelOp.setParameter("priorSdBandNamePrefix", priorSdBandNamePrefix);
                // todo: make sure we get a 1x1 product!
                Product inversionProduct = inversionSinglePixelOp.getTargetProduct();

                if (priorProduct == null) {
                    // same in the standard mode without using priors...
                    inversionProduct.setGeoCoding(IOUtils.getSinusoidalTileGeocoding(tile));
                }

                setTargetProduct(inversionProduct);     // todo: use WriteOp for the 46 BRDF products
            } else {
                logger.log(Level.ALL, "No prior file found for tile: " + tile + ", year: " + year + ", DoY: " +
                        IOUtils.getDoyString(doy) + " , Snow = " + computeSnow + " - no inversion performed.");
            }

            logger.log(Level.ALL, "Finished inversion process for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3InversionSinglePixel.class);
        }
    }
}
