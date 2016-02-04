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

    @Parameter(description = "Day of Year", interval = "[1,366]")
    private int doy;

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

        // STEP 1: get BBDR input product list...
        Product[] inputProducts;
        try {
            inputProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, tile, year, doy);
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        if (inputProducts != null && inputProducts.length > 0) {
//            String dailyAccumulatorDir = bbdrRootDir + File.separator + "DailyAcc"
//                    + File.separator + year + File.separator + tile;
//            if (computeSnow) {
//                dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "Snow" + File.separator);
//            }  else {
//                dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
//            }

            // STEP 2: do daily accumulation
            Product dailyAccumulationProduct;
            DailyAccumulationOp dailyAccumulationOp = new DailyAccumulationOp();
            dailyAccumulationOp.setParameterDefaultValues();
            dailyAccumulationOp.setSourceProducts(inputProducts);
            dailyAccumulationOp.setParameter("computeSnow", computeSnow);
            dailyAccumulationProduct = dailyAccumulationOp.getTargetProduct();

            // STEP 2: do full accumulation

            setTargetProduct(dailyAccumulationProduct);
        } else {
            logger.log(Level.ALL, "No input products found for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
            //  no BBDR input - just set a dummy target product
            Product dummyProduct = new Product("dummy", "dummy", 1, 1);
            setTargetProduct(dummyProduct);
        }

        getTargetProduct().setName("SUCCESS_dailyacc_" + year + "_" + doy);

        logger.log(Level.ALL, "Finished daily accumulation process for tile: " + tile + ", year: " + year + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3InversionSinglePixel.class);
        }
    }
}
