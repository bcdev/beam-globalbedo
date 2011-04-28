package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Operator for the creation of full accumulators in Albedo Inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.fullaccumulation")
public class GlobalbedoLevel3FullAccumulation extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

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

        Product[] priorProducts;
        try {
            priorProducts = IOUtils.getPriorProducts(priorDir, computeSnow);
        } catch (IOException e) {
            throw new OperatorException("Cannot load prior products: " + e.getMessage());
        }

        // STEP 2: get Daily Accumulator input files...
        final String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles";

        AlbedoInput inputProduct = null;
        Vector<int[]> allDoysVector = new Vector<int[]>();
        int priorIndex = 0;
        for (Product priorProduct : priorProducts) {
            if (priorIndex == 0) {   // test!!
                final int doy = AlbedoInversionUtils.getDoyFromPriorName(priorProduct.getName(), false);
                allDoysVector.clear();
                try {
                    inputProduct = IOUtils.getAlbedoInputProducts(accumulatorDir, doy, year, tile,
                                                                  wings,
                                                                  computeSnow);
                    final int[] allDoys = inputProduct.getProductDoys();
                    allDoysVector.add(allDoys);
                    priorIndex++;
                } catch (IOException e) {
                    // todo: just skip product, but add appropriate logging here
                    logger = BeamLogManager.getSystemLogger();
                    logger.log(Level.ALL, "Could not process DoY " + doy + " - skipping.");
//                    System.out.println("Could not process DoY " + doy + " - skipping.");
                }
            }
        }

        // STEP 3: we need to reproject the priors for further use...
        Product[] reprojectedPriorProducts;
        try {
            if (inputProduct != null) {
                reprojectedPriorProducts = IOUtils.getReprojectedPriorProducts(priorProducts, tile,
                                                                               inputProduct.getProductFilenames()[0]);
            } else {
                throw new OperatorException("No accumulator input products available - cannot proceed.");
            }
        } catch (IOException e) {
            throw new OperatorException("Cannot reproject prior products: " + e.getMessage());
        }

        TestFullAccumulationJAIAllDoysOp jaiOp = new TestFullAccumulationJAIAllDoysOp();
        jaiOp.setParameter("sourceFilenames", inputProduct.getProductFilenames());
        jaiOp.setParameter("gaRootDir", gaRootDir);
        jaiOp.setParameter("year", year);
        jaiOp.setParameter("tile", tile);
        jaiOp.setParameter("allDoys", allDoysVector);
        Product fullAccumulationProduct = jaiOp.getTargetProduct();

        // do the next steps per prior product:
//        priorIndex = 0;
//        for (Product priorProduct : reprojectedPriorProducts) {
//            final int doy = AlbedoInversionUtils.getDoyFromPriorName(priorProduct.getName(), true);
//            final String targetFileName = "fullacc_" + year + "_" + doy; // todo tile, snow
//
//            final String fullaccTargetDir = accumulatorDir + File.separator + "full";
//            final File targetFile = new File(fullaccTargetDir, targetFileName);
//            final WriteOp writeOp = new WriteOp(fullAccumulationProduct, targetFile, ProductIO.DEFAULT_FORMAT_NAME);
//            writeOp.writeProduct(ProgressMonitor.NULL);
//            priorIndex++;
//        }

        // test:
//        setTargetProduct(priorProducts[0]);
        System.out.println("done");
        // todo: how to avoid that either no target product is set, or 'result.dim' is written which is not needed ?
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3FullAccumulation.class);
        }
    }
}
