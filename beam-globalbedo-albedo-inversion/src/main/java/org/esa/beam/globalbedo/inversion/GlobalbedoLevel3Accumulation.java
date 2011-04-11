package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

import javax.media.jai.JAI;
import java.io.IOException;

/**
 * Operator for the daily accumulation part in Albedo Inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.accumulation")
public class GlobalbedoLevel3Accumulation extends Operator {

    @Parameter(defaultValue = "", description = "BBDR root directory")
    private String bbdrRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "001", description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Override
    public void initialize() throws OperatorException {

        //        JAI.getDefaultInstance().getTileScheduler().setParallelism(4);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get BBDR input product list...
        Product[] inputProducts;
        try {
            inputProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, tile, year, doy);
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        // STEP 2: accumulate all optimal estimations M, V, E as obtained from single observation files:
        // M_tot(DoY) = sum(M(obs[i]))
        // V_tot(DoY) = sum(V(obs[i]))
        // E_tot(DoY) = sum(E(obs[i]))
        DailyAccumulationOp accumulationOp = new DailyAccumulationOp();
        accumulationOp.setSourceProducts(inputProducts);
//        Product[] testProducts = new Product[]{inputProducts[0]};
//        accumulationOp.setSourceProducts(testProducts);
        accumulationOp.setParameter("computeSnow", computeSnow);
        Product accumulationProduct = accumulationOp.getTargetProduct();

        setTargetProduct(accumulationProduct);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Accumulation.class);
        }
    }
}
