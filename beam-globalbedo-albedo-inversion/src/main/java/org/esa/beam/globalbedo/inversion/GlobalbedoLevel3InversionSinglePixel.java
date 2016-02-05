package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.JAI;
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

        Accumulator[] allDailyAccs = new Accumulator[540];
        // todo: loop over all 540 days...
        Product[] inputProducts;
        try {
            // STEP 1: get BBDR input product list...
            final List<Product> inputProductList = new ArrayList<>();
            // todo: this is wrong!! we need an inputProductList per single day of the 540!!
            inputProductList.addAll(IOUtils.getAccumulationSinglePixelInputProducts(bbdrRootDir, tile, year));
            inputProductList.addAll(IOUtils.getAccumulationSinglePixelInputProducts(bbdrRootDir, tile, year-1));
            inputProductList.addAll(IOUtils.getAccumulationSinglePixelInputProducts(bbdrRootDir, tile, year+1));
            inputProducts = inputProductList.toArray(new Product[inputProductList.size()]);
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        if (inputProducts != null && inputProducts.length > 0) {
            // STEP 2: do daily accumulation
            DailyAccumulationSinglePixel dailyAccumulationSinglePixel =
                    new DailyAccumulationSinglePixel(inputProducts, computeSnow);
            final Accumulator dailyAcc = dailyAccumulationSinglePixel.compute();

            // STEP 2: do full accumulation

        }
        // todo: end loop over all days. Start full acc for the 46 doys

//        getTargetProduct().setName("SUCCESS_dailyacc_" + year + "_" + doy);
//
//        logger.log(Level.ALL, "Finished daily accumulation process for tile: " + tile + ", year: " + year + ", DoY: " +
//                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3InversionSinglePixel.class);
        }
    }
}
