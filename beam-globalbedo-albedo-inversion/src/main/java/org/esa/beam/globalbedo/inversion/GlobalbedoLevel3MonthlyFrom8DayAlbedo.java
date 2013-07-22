package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.JAI;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for getting monthly albedos from 8-day periods
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.albedo.monthly")
public class GlobalbedoLevel3MonthlyFrom8DayAlbedo extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "1", interval = "[1,12]", description = "Month index")
    private int monthIndex;

    @Parameter(defaultValue = "true", description = "If set, 8-day products are already mosaics")
    private boolean isInputMosaic;


    private Logger logger;

    @Override
    public void initialize() throws OperatorException {
        logger = BeamLogManager.getSystemLogger();
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get Albedo 8-day input files...
        Product[] albedo8DayProduct = getEightDayInputProducts();

        if (albedo8DayProduct != null && albedo8DayProduct.length > 0) {

            // STEP 2: get Albedo monthly product...

            MonthlyFrom8DayAlbedoOp monthlyAlbedoOp = new MonthlyFrom8DayAlbedoOp();
            monthlyAlbedoOp.setSourceProducts(albedo8DayProduct);
            monthlyAlbedoOp.setParameter("monthIndex", monthIndex);
            setTargetProduct(monthlyAlbedoOp.getTargetProduct());

            logger.log(Level.ALL, "Finished monthly albedo computation process for tile: " + tile + ", year: " + year +
                    ", month: " + monthIndex);
        } else {
            logger.log(Level.WARNING, "No monthly albedos computated for tile: " + tile + ", year: " + year +
                    ", month: " + monthIndex);
        }
    }

    private Product[] getEightDayInputProducts() {
        Product[] inputProducts;
        String albedoDir;
        if (isInputMosaic) {
            albedoDir = gaRootDir + File.separator + "Mosaic" + File.separator + "albedo" + File.separator;
            return IOUtils.getAlbedo8DayProducts(albedoDir, tile, true, year, monthIndex);
        } else {
            albedoDir = gaRootDir + File.separator + "Albedo" + File.separator + tile + File.separator;
            return IOUtils.getAlbedo8DayProducts(albedoDir, tile, false, year, -1);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3MonthlyFrom8DayAlbedo.class);
        }
    }

}
