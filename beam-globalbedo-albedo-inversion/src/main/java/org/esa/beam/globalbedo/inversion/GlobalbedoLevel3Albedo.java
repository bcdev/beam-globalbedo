package org.esa.beam.globalbedo.inversion;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;

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

    @Override
    public void initialize() throws OperatorException {
        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

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

        AlbedoInputContainer inputProductContainer = null;
        for (Product priorProduct : priorProducts) {
            int doy = AlbedoInversionUtils.getDoyFromPriorName(priorProduct.getName());
            try {
                inputProductContainer = IOUtils.getAlbedoInputProducts(accumulatorDir, doy, year, tile,
                                                                       wings,
                                                                       computeSnow);
                System.out.println();
            } catch (IOException e) {
                throw new OperatorException("Cannot load input products: " + e.getMessage());
            }
        }

        // STEP 3: we need to reproject the priors for further use...
        Product[] reprojectedPriorProducts = IOUtils.getReprojectedPriorProducts(priorProducts, tile,
                                                                                 inputProductContainer.getInputProducts()[0]);

        // STEP 4: compute pixelwise results (perform inversion) and write output
        // --> InversionOp (pixel operator), implement breadboard method 'Inversion'

        // test:
        setTargetProduct(priorProducts[0]);
        System.out.println("done");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Albedo.class);
        }
    }
}
