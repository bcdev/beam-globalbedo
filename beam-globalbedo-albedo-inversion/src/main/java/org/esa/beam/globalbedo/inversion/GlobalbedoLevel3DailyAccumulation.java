package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import java.awt.*;
import java.io.*;

/**
 * 'Master' operator for the daily accumulation part in Albedo Inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.dailyacc")
public class GlobalbedoLevel3DailyAccumulation extends Operator {

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

    @Parameter(defaultValue = "false", description = "Write also binary accumulator files besides dimap (to be decided)")
    private boolean writeBinaryAccumulators;

    @Override
    public void initialize() throws OperatorException {

//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose  // todo: change back

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

        String dailyAccumulatorDir = bbdrRootDir + File.separator + "AccumulatorFiles"
                + File.separator + year + File.separator + tile;
        if (computeSnow) {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "Snow" + File.separator);
        } else {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
        }

        Product accumulationProduct;
        // todo: make final decision
        if (writeBinaryAccumulators) {
            JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
            String dailyAccumulatorBinaryFilename = "matrices_" + year + doy + ".bin";
            final File dailyAccumulatorBinaryFile = new File(dailyAccumulatorDir + dailyAccumulatorBinaryFilename);
            DailyAccumulation2Op accumulationOp = new DailyAccumulation2Op();
            accumulationOp.setSourceProducts(inputProducts);
            accumulationOp.setParameter("computeSnow", computeSnow);
            accumulationOp.setParameter("dailyAccumulatorBinaryFile", dailyAccumulatorBinaryFile);
            accumulationProduct = accumulationOp.getTargetProduct();
        } else {
            DailyAccumulationOp accumulationOp = new DailyAccumulationOp();
            accumulationOp.setSourceProducts(inputProducts);
            accumulationOp.setParameter("computeSnow", computeSnow);
            accumulationProduct = accumulationOp.getTargetProduct();
        }

        setTargetProduct(accumulationProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3DailyAccumulation.class);
        }
    }
}
