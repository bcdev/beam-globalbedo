package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for the BRDF retrieval part (full accumulation and inversion)
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.inversion")
public class GlobalbedoLevel3Inversion extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "001", description = "DoY")
    private int doy;

    @Parameter(defaultValue = "540", description = "Wings")   // 540 # One year plus 3 months wings
    private int wings;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "30.0", description = "Prior scale factor")
    private double priorScaleFactor;

    @Parameter(defaultValue = "true", description = "Use binary accumulator files instead of dimap (to be decided)")
    private boolean useBinaryAccumulators;

    @Parameter(defaultValue = "false", description = "Do accumulation only (no inversion)")
    private boolean accumulationOnly;

    // todo: do we need this configurable?
    private boolean usePrior = true;

    private Logger logger;

    @Override
    public void initialize() throws OperatorException {
        logger = BeamLogManager.getSystemLogger();
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get Prior input file...
        final String priorDir = gaRootDir + File.separator + "Priors" + File.separator + tile + File.separator +
                "background" + File.separator + "processed.p1.0.618034.p2.1.00000";

        Product priorProduct;
        try {
            priorProduct = IOUtils.getPriorProduct(priorDir, doy, computeSnow);
        } catch (IOException e) {
            throw new OperatorException("Cannot load prior product: " + e.getMessage());
        }

        if (priorProduct == null) {
            logger.log(Level.ALL, "No prior file available for DoY " + doy + " - do inversion without prior...");
            usePrior = false;
        }

        // STEP 2: get Daily Accumulator input files...
        final String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles";

        AlbedoInput inputProduct = null;
        try {
            inputProduct = IOUtils.getAlbedoInputProduct(accumulatorDir, useBinaryAccumulators, doy, year, tile,
                    wings,
                    computeSnow);
        } catch (IOException e) {
            // todo: just skip product, but add appropriate logging here
            logger.log(Level.ALL, "Could not process DoY " + doy + " - skipping.");
        }

        // STEP 3: get BBDR product list...
        Product[] bbdrProducts;
        final String bbdrRootDir = gaRootDir + File.separator + "BBDR";
        try {
            bbdrProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, tile, year, doy);
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        // STEP 4: we need to reproject the priors for further use...
        Product reprojectedPriorProduct = null;
        if (usePrior) {
            try {
                if (inputProduct != null) {
                    reprojectedPriorProduct = IOUtils.getReprojectedPriorProduct(priorProduct, tile,
                            bbdrProducts[0]);
                } else {
                    throw new OperatorException("No accumulator input products available - cannot proceed.");
                }
            } catch (IOException e) {
                throw new OperatorException("Cannot reproject prior products: " + e.getMessage());
            }
        }

        String dailyAccumulatorDir = bbdrRootDir + File.separator + "AccumulatorFiles"
                + File.separator + year + File.separator + tile;
        String fullAccumulatorBinaryFilename = "matrices_full_" + year + doy + ".bin";
        if (computeSnow) {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "Snow" + File.separator);
        } else {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
        }

        String fullAccumulatorFilePath = dailyAccumulatorDir + fullAccumulatorBinaryFilename;

        InversionOp inversionOp = new InversionOp();
        inversionOp.setSourceProduct("priorProduct", reprojectedPriorProduct);  // may be null
        inversionOp.setParameter("fullAccumulatorFilePath", fullAccumulatorFilePath);
        inversionOp.setParameter("year", year);
        inversionOp.setParameter("tile", tile);
        inversionOp.setParameter("doy", doy);
        inversionOp.setParameter("computeSnow", computeSnow);
        inversionOp.setParameter("usePrior", usePrior);
        inversionOp.setParameter("priorScaleFactor", priorScaleFactor);
        Product inversionProduct = inversionOp.getTargetProduct();
        setTargetProduct(inversionProduct);

        System.out.println("done");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Inversion.class);
        }
    }
}
