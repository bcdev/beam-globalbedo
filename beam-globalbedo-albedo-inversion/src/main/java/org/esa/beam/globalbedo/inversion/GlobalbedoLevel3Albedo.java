package org.esa.beam.globalbedo.inversion;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */

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

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

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

    @Parameter(defaultValue = "540", description = "Wings")   // 540 # One year plus 3 months wings
    private int wings;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    // todo: do we need this configurable?
    private boolean usePrior = true;

    private static final double HALFLIFE = 11.54;

    @Override
    public void initialize() throws OperatorException {
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get Prior input files...
        String priorDir = gaRootDir + File.separator + "Priors" + File.separator + tile + File.separator +
                "background" + File.separator + "processed.p1.0.618034.p2.1.00000_java";

        Product[] priorProducts;
        try {
            priorProducts = IOUtils.getPriorProducts(priorDir, tile, computeSnow);
        } catch (IOException e) {
            throw new OperatorException("Cannot load prior products: " + e.getMessage());
        }

        // STEP 2: get Daily Accumulator input files...
//        String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles" +
//                                File.separator + year + File.separator + tile;
        String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles";

        AlbedoInput inputProduct = null;
        double[] allWeights = new double[priorProducts.length];
        Vector allDoysVector = new Vector();
        int priorIndex = 0;
        for (Product priorProduct : priorProducts) {
            if (priorIndex == 0) {   // test!!
                int doy = AlbedoInversionUtils.getDoyFromPriorName(priorProduct.getName(), false);
                allDoysVector.clear();
                try {
                    inputProduct = IOUtils.getAlbedoInputProducts(accumulatorDir, doy, year, tile,
                            wings,
                            computeSnow);
                    int[] allDoys = inputProduct.getProductDoys();
                    allDoysVector.add(allDoys);
                    allWeights[priorIndex] = Math.exp(-1.0 * allDoys[priorIndex] / HALFLIFE);
                    priorIndex++;
                } catch (IOException e) {
                    // todo: just skip product, but add appropriate logging here
                    System.out.println("Could not process DoY " + doy + " - skipping.");
//                throw new OperatorException("Cannot load input products: " + e.getMessage());
                }
            }
        }

        // STEP 3: we need to reproject the priors for further use...
        Product[] reprojectedPriorProducts = new Product[0];
        try {
            reprojectedPriorProducts = IOUtils.getReprojectedPriorProducts(priorProducts, tile,
                                                                           inputProduct.getProductFilenames()[0]);
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }

        // do the next steps per prior product:
        priorIndex = 0;
        for (Product priorProduct : reprojectedPriorProducts) {

            // STEP 4: do the full accumulation (pixelwise matrix addition, so we need another pixel operator...
            // --> FullAccumulationOp (pixel operator), implement breadboard method 'Accumulator'
            if (priorIndex == 0) {   // test!!
//                FullAccumulationOp fullAccumulationOp = new FullAccumulationOp();
//                // the following means that the source products corresponding to the LAST prior are used
//                // this is ok since the input products are the same for all priors (checked with GL, 20110407)
//                fullAccumulationOp.setSourceProducts(inputProduct.getProducts());
//                fullAccumulationOp.setParameter("allDoys", allDoysVector.get(priorIndex));
//                fullAccumulationOp.setParameter("weight", allWeights[priorIndex]);
//                Product fullAccumulationProduct = fullAccumulationOp.getTargetProduct();

                // test: JAI accumulator
                FullAccumulationJAIOp jaiOp = new FullAccumulationJAIOp();
                jaiOp.setParameter("sourceFilenames", inputProduct.getProductFilenames());
                jaiOp.setParameter("weight", allWeights[priorIndex]);
                Product fullAccumulationProduct = jaiOp.getTargetProduct();
                // end test

                // STEP 5: compute pixelwise results (perform inversion) and write output
                // --> InversionOp (pixel operator), implement breadboard method 'Inversion'
                InversionOp inversionOp = new InversionOp();
                inversionOp.setSourceProduct("fullAccumulationProduct", fullAccumulationProduct);
                inversionOp.setSourceProduct("priorProduct", priorProduct);
                inversionOp.setParameter("year", year);
                inversionOp.setParameter("tile", tile);
                inversionOp.setParameter("computeSnow", computeSnow);
                inversionOp.setParameter("usePrior", usePrior);
                Product inversionProduct = inversionOp.getTargetProduct();

//                setTargetProduct(fullAccumulationProduct);
//                setTargetProduct(inversionProduct);

                int doy = AlbedoInversionUtils.getDoyFromPriorName(priorProduct.getName(), true);
                String targetFileName = IOUtils.getInversionTargetFileName(year, doy, tile, computeSnow, usePrior);

                final String inversionTargetDir = gaRootDir + File.separator + "inversion" + File.separator + tile;
                File targetFile = new File(inversionTargetDir, targetFileName);
                final WriteOp writeOp = new WriteOp(fullAccumulationProduct, targetFile, ProductIO.DEFAULT_FORMAT_NAME);
//                final WriteOp writeOp = new WriteOp(inversionProduct, targetFile, ProductIO.DEFAULT_FORMAT_NAME);
                writeOp.writeProduct(ProgressMonitor.NULL);
                priorIndex++;
            }
        }


        // test:
//        setTargetProduct(priorProducts[0]);
        System.out.println("done");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Albedo.class);
        }
    }
}
