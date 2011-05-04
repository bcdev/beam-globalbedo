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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Operator for the inversion and albedo retrieval part in Albedo Inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.albedo")
public class GlobalbedoLevel3Albedo extends Operator {

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

    // todo: do we need this configurable?
    private boolean usePrior = true;
    private Logger logger;

    @Override
    public void initialize() throws OperatorException {
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get Prior input files...
        final String priorDir = gaRootDir + File.separator + "Priors" + File.separator + tile + File.separator +
                "background" + File.separator + "processed.p1.0.618034.p2.1.00000_java";

        Product priorProduct;
        try {
            priorProduct = IOUtils.getPriorProduct(priorDir, doy, computeSnow);
        } catch (IOException e) {
            throw new OperatorException("Cannot load prior product: " + e.getMessage());
        }

        // todo: what if priorProduct is null (i.e., priorFile of doy does not exist?)

        // STEP 2: get Daily Accumulator input files...
        final String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles";

        AlbedoInput inputProduct = null;
        final int doy = AlbedoInversionUtils.getDoyFromPriorName(priorProduct.getName(), false);
        try {
            inputProduct = IOUtils.getAlbedoInputProduct(accumulatorDir, doy, year, tile,
                                                         wings,
                                                         computeSnow);
        } catch (IOException e) {
            // todo: just skip product, but add appropriate logging here
            logger = BeamLogManager.getSystemLogger();
            logger.log(Level.ALL, "Could not process DoY " + doy + " - skipping.");
        }

        // STEP 3: we need to reproject the priors for further use...
        Product reprojectedPriorProduct;
        try {
            if (inputProduct != null) {
                reprojectedPriorProduct = IOUtils.getReprojectedPriorProduct(priorProduct, tile,
                        inputProduct.getProductFilenames()[0]);
            } else {
                throw new OperatorException("No accumulator input products available - cannot proceed.");
            }
        } catch (IOException e) {
            throw new OperatorException("Cannot reproject prior products: " + e.getMessage());
        }

        // STEP 4: full accumulation
//        FullAccumulationJAIOp jaiOp = new FullAccumulationJAIOp();
        FullAccumulationJAI2Op jaiOp = new FullAccumulationJAI2Op();
        jaiOp.setParameter("sourceFilenames", inputProduct.getProductFilenames());
        for (int i=0; i<inputProduct.getProductDoys().length; i++) {
            System.out.println("allDoys = " + i + ", " + inputProduct.getProductDoys()[i]);
        }
        jaiOp.setParameter("allDoys", inputProduct.getProductDoys());
        Product fullAccumulationProduct = jaiOp.getTargetProduct();

        // STEP 5: compute pixelwise results (perform inversion) and write output
        // --> InversionOp (pixel operator), implement breadboard method 'Inversion'
        InversionOp inversionOp = new InversionOp();
        inversionOp.setSourceProduct("fullAccumulationProduct", fullAccumulationProduct);
        inversionOp.setSourceProduct("priorProduct", reprojectedPriorProduct);
        inversionOp.setParameter("year", year);
        inversionOp.setParameter("tile", tile);
        inversionOp.setParameter("computeSnow", computeSnow);
        inversionOp.setParameter("usePrior", usePrior);
        inversionOp.setParameter("priorScaleFactor", priorScaleFactor);
        Product inversionProduct = inversionOp.getTargetProduct();

        setTargetProduct(inversionProduct);
//        setTargetProduct(fullAccumulationProduct);
        System.out.println("done");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Albedo.class);
        }
    }
}
