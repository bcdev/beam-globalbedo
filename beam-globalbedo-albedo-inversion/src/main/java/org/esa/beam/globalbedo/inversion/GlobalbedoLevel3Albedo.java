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
    private static final double HALFLIFE = 11.54;

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
        double[] allWeights = new double[priorProducts.length];
        Vector allDoysVector = new Vector();
        int productIndex = 0;
        for (Product priorProduct : priorProducts) {
            int doy = AlbedoInversionUtils.getDoyFromPriorName(priorProduct.getName());
            allDoysVector.clear();
            try {
                // todo: how to proceed if input file(s) cannot be read?
                inputProductContainer = IOUtils.getAlbedoInputProducts(accumulatorDir, doy, year, tile,
                                                                       wings,
                                                                       computeSnow);
                allWeights[productIndex] = Math.exp(-1.0 * inputProductContainer.getInputProductDoys().length / HALFLIFE);
                int[] allDoys = inputProductContainer.getInputProductDoys();
                allDoysVector.add(allDoys);
                productIndex++;
            } catch (IOException e) {
                throw new OperatorException("Cannot load input products: " + e.getMessage());
            }
        }

        // STEP 3: do the full accumulation (pixelwise matrix addition, so we need another pixel operator...
        // --> FullAccumulationOp (pixel operator), implement breadboard method 'Accumulator'
        FullAccumulationOp fullAccumulationOp = new FullAccumulationOp();
        // todo: the following means that the source products corresponding to the LAST prior are used
        // this seems to be as in breadboard, but make sure that this is correct
        // todo: make one product PER PRIOR, move in loop below in step 5
        fullAccumulationOp.setSourceProducts(inputProductContainer.getInputProducts());
        fullAccumulationOp.setParameter("allDoys", allDoysVector);
        fullAccumulationOp.setParameter("allWeights", allWeights);
        Product fullAccumulationProduct = fullAccumulationOp.getTargetProduct();

        // STEP 4: we need to reproject the priors for further use...
        Product[] reprojectedPriorProducts = IOUtils.getReprojectedPriorProducts(priorProducts, tile,
                                                                                 inputProductContainer.getInputProducts()[0]);

        // STEP 5: compute pixelwise results (perform inversion) and write output
        // --> InversionOp (pixel operator), implement breadboard method 'Inversion'
        for (Product priorProduct : reprojectedPriorProducts) {
            InversionOp inversionOp = new InversionOp();
            inversionOp.setSourceProduct("fullAccumulationProduct", fullAccumulationProduct);
            inversionOp.setSourceProduct("priorProduct", priorProduct);
            inversionOp.setParameter("year", year);
            inversionOp.setParameter("tile", tile);
            inversionOp.setParameter("computeSnow", computeSnow);
//            target=ProcessInversion, args=[ data.MData[i], data.VData[i], data.EData[i], data.Mask[i], Lambda,
//                    PriorFileToProcess[0], columns, rows, UsePrior, data.DoYClosestSample[i], tile, year, Snow])

            setTargetProduct(fullAccumulationOp.getTargetProduct());
            File targetFile = null; // todo define
            final WriteOp writeOp = new WriteOp(getTargetProduct(), targetFile, ProductIO.DEFAULT_FORMAT_NAME);
            writeOp.writeProduct(ProgressMonitor.NULL);
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
