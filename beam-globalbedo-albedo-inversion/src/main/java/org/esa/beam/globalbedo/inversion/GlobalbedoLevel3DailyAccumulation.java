package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.WriteOp;

import javax.media.jai.JAI;
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

        String dailyAccumulatorDir = bbdrRootDir + File.separator + "AccumulatorFiles"
                + File.separator + year + File.separator + tile;
        if (computeSnow) {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "Snow" + File.separator);
        } else {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
        }

        // STEP 2: do accumulation, write to binary file
        Product accumulationProduct;
        // make sure that binary output is written sequentially
        JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
        String dailyAccumulatorBinaryFilename = "matrices_" + year + IOUtils.getDoyString(doy) + ".bin";
        final File dailyAccumulatorBinaryFile = new File(dailyAccumulatorDir + dailyAccumulatorBinaryFilename);
        DailyAccumulationOp accumulationOp = new DailyAccumulationOp();
        accumulationOp.setSourceProducts(inputProducts);
        accumulationOp.setParameter("computeSnow", computeSnow);
        accumulationOp.setParameter("dailyAccumulatorBinaryFile", dailyAccumulatorBinaryFile);
        accumulationProduct = accumulationOp.getTargetProduct();

        setTargetProduct(accumulationProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3DailyAccumulation.class);
        }
    }
}
