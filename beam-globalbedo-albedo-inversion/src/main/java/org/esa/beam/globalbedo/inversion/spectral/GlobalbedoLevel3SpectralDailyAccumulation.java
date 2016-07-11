package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.DailyAccumulationOp;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for the daily accumulation part in spectral inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.dailyacc.spectral",
        description = "'Master' operator for the daily accumulation part in Albedo Inversion",
        autoWriteDisabled = true,    // NOTE: if autoWriteDisabled = true, computePixel/computeTile will not be entered in operator providing the target product!
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011-2016 by Brockmann Consult")
public class GlobalbedoLevel3SpectralDailyAccumulation extends Operator{

    @Parameter(defaultValue = "", description = "SDR root directory")
    private String sdrRootDir;

    @Parameter(label = "Sensors to ingest in BRDF retrieval", defaultValue = "MERIS,VGT")
    private String[] sensors;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;


    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        // STEP 1: get SDR input product list...
        Product[] inputProducts;
        try {
            inputProducts = IOUtils.getAccumulationInputProducts(sdrRootDir, sensors, tile, year, doy);
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of SDR input products: " + e.getMessage());
        }

        if (inputProducts.length > 0) {
            String dailyAccumulatorDir = sdrRootDir + File.separator + "DailyAcc"
                    + File.separator + year + File.separator + tile;
            if (computeSnow) {
                dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "Snow" + File.separator);
            }  else {
                dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
            }

            // STEP 2: do accumulation, write to binary file
            Product accumulationProduct;
            // make sure that binary output is written sequentially
            JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
            String dailyAccumulatorBinaryFilename = "matrices_" + year + IOUtils.getDoyString(doy) + ".bin";
            final File dailyAccumulatorBinaryFile = new File(dailyAccumulatorDir + dailyAccumulatorBinaryFilename);
            SpectralDailyAccumulationOp accumulationOp = new SpectralDailyAccumulationOp();
            accumulationOp.setParameterDefaultValues();
            accumulationOp.setSourceProducts(inputProducts);
            accumulationOp.setParameter("computeSnow", computeSnow);
            accumulationOp.setParameter("dailyAccumulatorBinaryFile", dailyAccumulatorBinaryFile);
            accumulationProduct = accumulationOp.getTargetProduct();

            setTargetProduct(accumulationProduct);
        } else {
            logger.log(Level.ALL, "No SDR input products found for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
            //  no SDR input - just set a dummy target product
            Product dummyProduct = new Product("dummy", "dummy", 1, 1);
            setTargetProduct(dummyProduct);
        }

        logger.log(Level.ALL, "Finished daily accumulation process for tile: " + tile + ", year: " + year + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3SpectralDailyAccumulation.class);
        }
    }
}
