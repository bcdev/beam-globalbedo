package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.dataio.ProductIO;
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

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "tileInfo_0.dim", description = "Name of tile info filename providing the geocoding")
    private String tileInfoFilename;

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

    private boolean usePrior = true;

    private Logger logger;

    @Override
    public void initialize() throws OperatorException {
        logger = BeamLogManager.getSystemLogger();
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get Prior input file...
        String priorSnowSeparationDirName;
        if (computeSnow) {
            priorSnowSeparationDirName = "PriorStage2Snow";
        } else {
            priorSnowSeparationDirName = "PriorStage2";
        }
        final String priorDir = priorRootDir + File.separator + priorSnowSeparationDirName + File.separator + tile +
                File.separator + "background" + File.separator + "processed.p1.0.618034.p2.1.00000";

        Product priorProduct = null;
        try {
            priorProduct = IOUtils.getPriorProduct(priorDir, doy, computeSnow);
        } catch (IOException e) {
            throw new OperatorException("No prior file available for DoY " + IOUtils.getDoyString(doy) +
                    " - cannot proceed...: " + e.getMessage());
        }

        if (priorProduct != null) {
            // STEP 2: set paths...
            final String bbdrRootDir = gaRootDir + File.separator + "BBDR";
            String fullAccumulatorDir = bbdrRootDir + File.separator + "AccumulatorFiles"
                    + File.separator + year + File.separator + tile;

            // STEP 3: we need to reproject the priors for further use...
            Product reprojectedPriorProduct = null;
            try {
                String tileInfoFilePath = IOUtils.getTileInfoFilePath(fullAccumulatorDir, tileInfoFilename);
                Product tileInfoProduct = ProductIO.readProduct(tileInfoFilePath);
                if (priorProduct != null) {
                    reprojectedPriorProduct = IOUtils.getReprojectedPriorProduct(priorProduct, tile,
                            tileInfoProduct);
                } else {
                    usePrior = false;
                    reprojectedPriorProduct = tileInfoProduct;
                }
            } catch (IOException e) {
                throw new OperatorException("Cannot reproject prior products - cannot proceed: " + e.getMessage());
            }

            // STEP 5: do inversion...
            String fullAccumulatorBinaryFilename = "matrices_full_" + year + IOUtils.getDoyString(doy) + ".bin";
            if (computeSnow) {
                fullAccumulatorDir = fullAccumulatorDir.concat(File.separator + "Snow" + File.separator);
            } else {
                fullAccumulatorDir = fullAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
            }

            String fullAccumulatorFilePath = fullAccumulatorDir + fullAccumulatorBinaryFilename;

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
        } else {
            logger.log(Level.ALL, "No prior file found for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " , Snow = " + computeSnow + " - no inversion performed.");
        }

        logger.log(Level.ALL, "Finished inversion process for tile: " + tile + ", year: " + year + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Inversion.class);
        }
    }
}
